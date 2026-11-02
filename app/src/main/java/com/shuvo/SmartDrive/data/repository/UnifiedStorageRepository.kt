package com.shuvo.SmartDrive.data.repository

import android.app.DownloadManager
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.shuvo.SmartDrive.data.auth.FirebaseAuthRepository
import com.shuvo.SmartDrive.data.auth.MultiDriveOAuthManager
import com.shuvo.SmartDrive.data.model.FileCategory
import com.shuvo.SmartDrive.data.model.LinkedDriveAccount
import com.shuvo.SmartDrive.data.model.StorageCategorySummary
import com.shuvo.SmartDrive.data.model.UnifiedCloudFile
import com.shuvo.SmartDrive.data.model.UnifiedStoragePoolState
import com.shuvo.SmartDrive.data.remote.DriveApiService
import com.shuvo.SmartDrive.data.remote.DriveFileMetadata
import com.shuvo.SmartDrive.data.remote.StreamingRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody

/**
 * Manages the unified virtual storage pool across up to 5 Google Drive accounts.
 *
 * Implements:
 * 1. Parallel quota queries and file syncing across all accounts.
 * 2. Smart Non-Splitting Bin-Packing batch upload allocation.
 * 3. Zero-disk streaming upload and streaming download.
 * 4. Permanent hard-delete (bypassing Google Drive trash).
 */
class UnifiedStorageRepository(
    private val context: Context,
    private val driveApi: DriveApiService,
    private val oAuthManager: MultiDriveOAuthManager,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Parallel query of live quotas across all linked accounts.
     * Updates account records with accurate used and total bytes.
     */
    suspend fun refreshLiveQuotas(
        accounts: List<LinkedDriveAccount>
    ): List<LinkedDriveAccount> = withContext(Dispatchers.IO) {
        coroutineScope {
            val deferreds = accounts.map { account ->
                async {
                    val tokenResult = oAuthManager.getAccessToken(account.accountEmail)
                    if (tokenResult.isFailure) {
                        return@async account
                    }
                    val token = tokenResult.getOrNull().orEmpty()
                    try {
                        val response = driveApi.getAbout(authHeader = "Bearer $token")
                        if (response.isSuccessful && response.body() != null) {
                            val quota = response.body()!!.storageQuota
                            val user = response.body()!!.user
                            account.copy(
                                displayName = user?.displayName ?: account.displayName,
                                avatarUrl = user?.photoLink ?: account.avatarUrl,
                                totalQuotaBytes = quota?.limitBytes ?: account.totalQuotaBytes,
                                usedQuotaBytes = quota?.usageBytes ?: account.usedQuotaBytes,
                                lastSyncTimestamp = System.currentTimeMillis()
                            )
                        } else {
                            if (response.code() == 401) {
                                oAuthManager.invalidateToken(account.accountEmail)
                            }
                            account
                        }
                    } catch (e: Exception) {
                        account
                    }
                }
            }
            deferreds.awaitAll()
        }
    }

    /**
     * Computes the aggregated 75 GB Storage Pool State and category summaries.
     */
    fun computeStoragePoolState(
        accounts: List<LinkedDriveAccount>,
        files: List<UnifiedCloudFile>
    ): UnifiedStoragePoolState {
        if (accounts.isEmpty()) {
            return UnifiedStoragePoolState(connectedAccountCount = 0)
        }

        val totalQuota = accounts.sumOf { it.totalQuotaBytes }
        val totalUsedFromAccounts = accounts.sumOf { it.usedQuotaBytes }
        val maxSingle = accounts.maxOfOrNull { it.freeQuotaBytes } ?: 0L

        // Group files by category
        val categoryFilesMap = files.groupBy { it.category }

        val categorySummaries = FileCategory.entries.map { cat ->
            val catFiles = categoryFilesMap[cat] ?: emptyList()
            val totalCatBytes = catFiles.sumOf { it.size }
            val percentage = if (totalQuota > 0L) {
                (totalCatBytes.toFloat() / totalQuota.toFloat()) * 100f
            } else 0f

            StorageCategorySummary(
                category = cat,
                totalBytes = totalCatBytes,
                fileCount = catFiles.size,
                percentageOfTotal = percentage
            )
        }

        return UnifiedStoragePoolState(
            totalQuotaBytes = totalQuota,
            usedQuotaBytes = totalUsedFromAccounts,
            maxSingleFileCapacityBytes = maxSingle,
            connectedAccountCount = accounts.size,
            categories = categorySummaries,
            isAnalyzing = false,
            errorMessage = null
        )
    }

    /**
     * Real-time Flow of all unified files for a given master user ID.
     */
    fun observeUnifiedFiles(userId: String): Flow<List<UnifiedCloudFile>> = callbackFlow {
        val query = firestore.collection(FirebaseAuthRepository.USERS_COLLECTION)
            .document(userId)
            .collection(FirebaseAuthRepository.FILES_COLLECTION)
            .orderBy("createdAt", Query.Direction.DESCENDING)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { it.toObject(UnifiedCloudFile::class.java) }
                trySend(list)
            } else {
                trySend(emptyList())
            }
        }
        awaitClose { listener.remove() }
    }

    /**
     * Synchronizes files across all linked accounts by querying Google Drive API
     * and merging records into Firestore index.
     */
    suspend fun syncAllDrivesToFirestore(
        userId: String,
        accounts: List<LinkedDriveAccount>
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var syncedCount = 0
            coroutineScope {
                val deferredList = accounts.map { account ->
                    async {
                        val tokenResult = oAuthManager.getAccessToken(account.accountEmail)
                        if (tokenResult.isFailure) return@async emptyList<UnifiedCloudFile>()
                        val token = tokenResult.getOrNull().orEmpty()

                        try {
                            val response = driveApi.listFiles(authHeader = "Bearer $token")
                            if (response.isSuccessful && response.body()?.files != null) {
                                response.body()!!.files!!.map { driveFile ->
                                    UnifiedCloudFile(
                                        id = driveFile.id,
                                        driveFileId = driveFile.id,
                                        fileName = driveFile.name,
                                        mimeType = driveFile.mimeType,
                                        size = driveFile.sizeBytes,
                                        ownerAccountEmail = account.accountEmail,
                                        createdAt = System.currentTimeMillis(),
                                        thumbnailUrl = driveFile.thumbnailLink,
                                        webContentLink = driveFile.webContentLink
                                    )
                                }
                            } else {
                                emptyList()
                            }
                        } catch (e: Exception) {
                            emptyList()
                        }
                    }
                }

                val allDiscoveredFiles = deferredList.awaitAll().flatten()

                // Batch upsert to Firestore
                val batch = firestore.batch()
                val filesCollection = firestore.collection(FirebaseAuthRepository.USERS_COLLECTION)
                    .document(userId)
                    .collection(FirebaseAuthRepository.FILES_COLLECTION)

                for (file in allDiscoveredFiles) {
                    val docRef = filesCollection.document(file.id)
                    batch.set(docRef, file)
                    syncedCount++
                }
                if (syncedCount > 0) {
                    batch.commit().await()
                }
            }
            Result.success(syncedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =========================================================================
    // SMART NON-SPLITTING BATCH UPLOAD ALLOCATION (BIN-PACKING)
    // =========================================================================

    /**
     * Represents a file chosen for upload from the Android system file picker.
     */
    data class CandidateFile(
        val uri: Uri,
        val fileName: String,
        val sizeBytes: Long,
        val mimeType: String
    )

    /**
     * Planned allocation mapping each candidate file to a single target Drive account.
     */
    data class UploadAllocation(
        val file: CandidateFile,
        val targetAccountEmail: String
    )

    sealed class AllocationResult {
        data class Success(val allocations: List<UploadAllocation>) : AllocationResult()
        data class Failure(val reason: String) : AllocationResult()
    }

    /**
     * Executes the Bin-Packing Allocation Algorithm:
     * - Rule 1: A single file must NEVER be split or chunked across two accounts.
     * - Rule 2: Single file cannot exceed 15 GB (16,106,127,360 bytes).
     * - Rule 3: Contiguous space check against each account's virtual free quota.
     *
     * If Account 1 has 0.5 GB free and Account 2 has 15 GB free, a 2 GB file
     * will bypass Account 1 and allocate entirely to Account 2.
     */
    fun planBatchUpload(
        candidateFiles: List<CandidateFile>,
        accounts: List<LinkedDriveAccount>
    ): AllocationResult {
        if (accounts.isEmpty()) {
            return AllocationResult.Failure("No Google Drive accounts linked. Link at least 1 account.")
        }

        val maxAllowedSingleFile = 15L * 1024L * 1024L * 1024L // 15 GB

        // Step 1: Validate individual file limits
        for (file in candidateFiles) {
            if (file.sizeBytes > maxAllowedSingleFile) {
                return AllocationResult.Failure(
                    "File '${file.fileName}' exceeds 15 GB. Single file cannot exceed 15 GB."
                )
            }
        }

        // Virtual available space tracker per account email
        val virtualAvailableMap = accounts.associate { it.accountEmail to it.freeQuotaBytes }.toMutableMap()
        val allocations = mutableListOf<UploadAllocation>()

        // Sort files descending by size for optimal First-Fit-Decreasing bin packing
        val sortedFiles = candidateFiles.sortedByDescending { it.sizeBytes }

        for (file in sortedFiles) {
            // Find target account with enough contiguous free space
            val eligibleEntry = virtualAvailableMap.entries
                .filter { it.value >= file.sizeBytes }
                .minByOrNull { it.value } // Best fit minimizes wasted fragmentation

            if (eligibleEntry == null) {
                return AllocationResult.Failure(
                    "Insufficient contiguous space for '${file.fileName}' (${UnifiedCloudFile.formatBytes(file.sizeBytes)}). " +
                            "No single linked drive has enough free storage."
                )
            }

            val targetEmail = eligibleEntry.key
            allocations.add(UploadAllocation(file, targetEmail))

            // Deduct allocated size from virtual free space
            val remaining = virtualAvailableMap[targetEmail] ?: 0L
            virtualAvailableMap[targetEmail] = remaining - file.sizeBytes
        }

        return AllocationResult.Success(allocations)
    }

    /**
     * Executes streaming upload directly from ContentResolver InputStream to Drive API.
     * Zero local disk buffering is used.
     */
    suspend fun uploadFileStreaming(
        userId: String,
        allocation: UploadAllocation,
        contentResolver: ContentResolver,
        onProgress: (percent: Float, bytesUploaded: Long, totalBytes: Long) -> Unit
    ): Result<UnifiedCloudFile> = withContext(Dispatchers.IO) {
        val targetEmail = allocation.targetAccountEmail
        val file = allocation.file

        try {
            val tokenResult = oAuthManager.getAccessToken(targetEmail)
            if (tokenResult.isFailure) {
                return@withContext Result.failure(
                    tokenResult.exceptionOrNull() ?: Exception("Failed to get auth token for $targetEmail")
                )
            }
            val token = tokenResult.getOrNull().orEmpty()
            val authHeader = "Bearer $token"

            // 1. Initiate Resumable Upload Session
            val initResponse = driveApi.initiateResumableUpload(
                authHeader = authHeader,
                mimeType = file.mimeType,
                contentLength = file.sizeBytes,
                metadata = DriveFileMetadata(name = file.fileName, mimeType = file.mimeType)
            )

            if (!initResponse.isSuccessful) {
                return@withContext Result.failure(
                    Exception("Failed to initiate upload session: ${initResponse.code()} ${initResponse.message()}")
                )
            }

            val sessionUrl = initResponse.headers()["Location"]
                ?: return@withContext Result.failure(Exception("Missing resumable upload session Location header"))

            // 2. Stream directly from ContentResolver into the session URL
            val requestBody = StreamingRequestBody(
                contentResolver = contentResolver,
                uri = file.uri,
                mimeType = file.mimeType,
                totalLength = file.sizeBytes,
                onProgress = { written, total ->
                    val fraction = if (total > 0L) written.toFloat() / total.toFloat() else 0f
                    onProgress(fraction * 100f, written, total)
                }
            )

            val contentRange = if (file.sizeBytes > 0) {
                "bytes 0-${file.sizeBytes - 1}/${file.sizeBytes}"
            } else {
                "bytes */0"
            }

            val uploadResponse = driveApi.uploadToSessionUrl(
                sessionUrl = sessionUrl,
                contentRange = contentRange,
                fileStream = requestBody
            )

            if (!uploadResponse.isSuccessful || uploadResponse.body() == null) {
                return@withContext Result.failure(
                    Exception("Streaming upload failed: ${uploadResponse.code()} ${uploadResponse.message()}")
                )
            }

            val uploadedDriveItem = uploadResponse.body()!!

            // 3. Create Unified Virtual File Index entry in Firestore
            val cloudFile = UnifiedCloudFile(
                id = uploadedDriveItem.id,
                driveFileId = uploadedDriveItem.id,
                fileName = file.fileName,
                mimeType = file.mimeType,
                size = file.sizeBytes,
                ownerAccountEmail = targetEmail,
                createdAt = System.currentTimeMillis(),
                thumbnailUrl = uploadedDriveItem.thumbnailLink,
                webContentLink = uploadedDriveItem.webContentLink
            )

            firestore.collection(FirebaseAuthRepository.USERS_COLLECTION)
                .document(userId)
                .collection(FirebaseAuthRepository.FILES_COLLECTION)
                .document(cloudFile.id)
                .set(cloudFile)
                .await()

            Result.success(cloudFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =========================================================================
    // PERMANENT HARD DELETE (BYPASS GOOGLE DRIVE TRASH)
    // =========================================================================

    /**
     * Executes a permanent hard delete directly via Drive REST API DELETE endpoint.
     * Bypasses the Trash completely and immediately reclaims storage quota.
     * Also removes the index entry from Firestore.
     */
    suspend fun permanentlyDeleteFile(
        userId: String,
        file: UnifiedCloudFile
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val tokenResult = oAuthManager.getAccessToken(file.ownerAccountEmail)
            if (tokenResult.isFailure) {
                return@withContext Result.failure(
                    tokenResult.exceptionOrNull() ?: Exception("Auth error for ${file.ownerAccountEmail}")
                )
            }
            val token = tokenResult.getOrNull().orEmpty()

            // 1. Hard delete on Google Drive
            val response = driveApi.hardDeleteFile(
                authHeader = "Bearer $token",
                fileId = file.driveFileId
            )

            // 204 No Content indicates success; 404 indicates file was already deleted
            if (!response.isSuccessful && response.code() != 404) {
                return@withContext Result.failure(
                    Exception("Failed to delete file from Drive: ${response.code()} ${response.message()}")
                )
            }

            // 2. Remove document from Firestore
            firestore.collection(FirebaseAuthRepository.USERS_COLLECTION)
                .document(userId)
                .collection(FirebaseAuthRepository.FILES_COLLECTION)
                .document(file.id)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =========================================================================
    // ON-DEMAND DOWNLOAD & STREAMING RETRIEVAL
    // =========================================================================

    /**
     * Fetches the direct streaming ResponseBody with bearer authentication.
     * Zero-disk footprint: callers read this in-memory.
     */
    suspend fun getFileStream(
        file: UnifiedCloudFile,
        rangeHeader: String? = null
    ): Result<ResponseBody> = withContext(Dispatchers.IO) {
        try {
            val tokenResult = oAuthManager.getAccessToken(file.ownerAccountEmail)
            if (tokenResult.isFailure) {
                return@withContext Result.failure(
                    tokenResult.exceptionOrNull() ?: Exception("Auth error for ${file.ownerAccountEmail}")
                )
            }
            val token = tokenResult.getOrNull().orEmpty()

            val response = driveApi.streamFile(
                authHeader = "Bearer $token",
                fileId = file.driveFileId,
                rangeHeader = rangeHeader
            )

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Stream failed: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Downloads file on-demand directly into public Downloads directory using DownloadManager.
     */
    suspend fun downloadFileOnDemand(file: UnifiedCloudFile): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val tokenResult = oAuthManager.getAccessToken(file.ownerAccountEmail)
            if (tokenResult.isFailure) {
                return@withContext Result.failure(
                    tokenResult.exceptionOrNull() ?: Exception("Auth error")
                )
            }
            val token = tokenResult.getOrNull().orEmpty()

            val downloadUri = Uri.parse("https://www.googleapis.com/drive/v3/files/${file.driveFileId}?alt=media")
            val request = DownloadManager.Request(downloadUri).apply {
                addRequestHeader("Authorization", "Bearer $token")
                setTitle(file.fileName)
                setDescription("Downloading ${file.fileName} from Unified Drive")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, file.fileName)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = manager.enqueue(request)
            Result.success(downloadId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

