package com.shuvo.SmartDrive.ui.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.firebase.auth.FirebaseUser
import com.shuvo.SmartDrive.DriveApplication
import com.shuvo.SmartDrive.data.auth.MultiDriveOAuthManager
import com.shuvo.SmartDrive.data.model.FileCategory
import com.shuvo.SmartDrive.data.model.LinkedDriveAccount
import com.shuvo.SmartDrive.data.model.UnifiedCloudFile
import com.shuvo.SmartDrive.data.model.UnifiedStoragePoolState
import com.shuvo.SmartDrive.data.model.UserProfile
import com.shuvo.SmartDrive.data.repository.UnifiedStorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Main ViewModel coordinating the 75 GB Unified Cloud Storage experience:
 * - Authentication & User Profile
 * - 5-Slot Google Account Linker & Parallel Storage Pool Analyzer
 * - Smart Non-Splitting Bin-Packing Batch Upload
 * - Real-Time Category Breakdown & Unified Explorer
 * - In-App Zero-Disk Cloud Streaming Viewers
 * - Permanent File Hard Deletion & On-Demand Device Downloads
 */
class MainStorageViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DriveApplication
    private val authRepo = app.authRepository
    private val storageRepo = app.unifiedStorageRepository
    private val oAuthManager = app.oAuthManager

    // --- Authentication & Profile States ---
    private val _currentUser = MutableStateFlow<FirebaseUser?>(authRepo.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _authLoading = MutableStateFlow(false)
    val authLoading: StateFlow<Boolean> = _authLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    // --- Day/Night Mode & View Preference (iOS Style) ---
    private val uiPrefs = application.getSharedPreferences("smartdrive_ui_prefs", android.content.Context.MODE_PRIVATE)
    private val _isDarkMode = MutableStateFlow(uiPrefs.getBoolean("is_dark_mode", false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleTheme() {
        val next = !_isDarkMode.value
        _isDarkMode.value = next
        uiPrefs.edit().putBoolean("is_dark_mode", next).apply()
    }

    private val _isGridView = MutableStateFlow(uiPrefs.getBoolean("is_grid_view", true))
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    fun setGridView(isGrid: Boolean) {
        _isGridView.value = isGrid
        uiPrefs.edit().putBoolean("is_grid_view", isGrid).apply()
    }

    // --- 5-Slot Linked Accounts & Storage Pool ---
    private val _linkedAccounts = MutableStateFlow<List<LinkedDriveAccount>>(emptyList())
    val linkedAccounts: StateFlow<List<LinkedDriveAccount>> = _linkedAccounts.asStateFlow()

    private val _isMergingPool = MutableStateFlow(false)
    val isMergingPool: StateFlow<Boolean> = _isMergingPool.asStateFlow()

    private val _mergeStatusText = MutableStateFlow("Analyzing connected accounts...")
    val mergeStatusText: StateFlow<String> = _mergeStatusText.asStateFlow()

    private val _storagePoolState = MutableStateFlow(UnifiedStoragePoolState())
    val storagePoolState: StateFlow<UnifiedStoragePoolState> = _storagePoolState.asStateFlow()

    // --- Unified File List & Search/Filter ---
    private val _allFiles = MutableStateFlow<List<UnifiedCloudFile>>(emptyList())
    val allFiles: StateFlow<List<UnifiedCloudFile>> = _allFiles.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<FileCategory?>(null)
    val selectedCategory: StateFlow<FileCategory?> = _selectedCategory.asStateFlow()

    val filteredFiles: StateFlow<List<UnifiedCloudFile>> = combine(
        _allFiles,
        _searchQuery,
        _selectedCategory
    ) { files, query, category ->
        files.filter { file ->
            val matchesCategory = category == null || file.category == category
            val matchesSearch = query.isBlank() ||
                    file.fileName.contains(query, ignoreCase = true) ||
                    file.mimeType.contains(query, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Batch Upload Progress State ---
    data class BatchUploadUiState(
        val isUploading: Boolean = false,
        val currentFileName: String = "",
        val currentFileIndex: Int = 0,
        val totalFiles: Int = 0,
        val currentFileProgressPercent: Float = 0f,
        val overallProgressPercent: Float = 0f,
        val errorMessage: String? = null,
        val isSuccess: Boolean = false
    )

    private val _uploadState = MutableStateFlow(BatchUploadUiState())
    val uploadState: StateFlow<BatchUploadUiState> = _uploadState.asStateFlow()

    // --- Permanent Delete Confirmation State ---
    private val _filePendingDeletion = MutableStateFlow<UnifiedCloudFile?>(null)
    val filePendingDeletion: StateFlow<UnifiedCloudFile?> = _filePendingDeletion.asStateFlow()

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    // --- Feedback Notifications (Toasts / Snackbars) ---
    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    // --- Google Drive OAuth Consent Request Event ---
    private val _recoverableAuthIntent = MutableSharedFlow<Intent>(extraBufferCapacity = 1)
    val recoverableAuthIntent: SharedFlow<Intent> = _recoverableAuthIntent.asSharedFlow()

    init {
        viewModelScope.launch {
            authRepo.authStateFlow.collect { user ->
                _currentUser.value = user
                if (user != null) {
                    loadUserData(user.uid)
                } else {
                    _userProfile.value = null
                    _linkedAccounts.value = emptyList()
                    _allFiles.value = emptyList()
                    _storagePoolState.value = UnifiedStoragePoolState()
                }
            }
        }
    }

    private fun loadUserData(uid: String) {
        viewModelScope.launch {
            val localStored = oAuthManager.getStoredLinkedAccounts()
            if (localStored.isNotEmpty()) {
                _linkedAccounts.value = localStored
            }

            authRepo.userProfileFlow(uid).collect { profile ->
                _userProfile.value = profile
                if (profile != null && profile.linkedDriveAccounts.isNotEmpty()) {
                    _linkedAccounts.value = profile.linkedDriveAccounts
                    oAuthManager.saveLinkedAccounts(profile.linkedDriveAccounts)
                    recalculatePool(profile.linkedDriveAccounts, _allFiles.value)
                }
            }
        }

        viewModelScope.launch {
            storageRepo.observeUnifiedFiles(uid).collect { files ->
                _allFiles.value = files
                recalculatePool(_linkedAccounts.value, files)
            }
        }
    }

    // --- Auth Actions ---

    fun signInWithEmail(email: String, pass: String, onSuccess: () -> Unit) {
        if (email.isBlank() || pass.isBlank()) {
            _authError.value = "Email and password cannot be empty"
            return
        }
        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = null
            val result = authRepo.signInWithEmail(email.trim(), pass)
            _authLoading.value = false
            result.fold(
                onSuccess = { onSuccess() },
                onFailure = { _authError.value = it.localizedMessage ?: "Login failed" }
            )
        }
    }

    fun signUpWithEmail(name: String, email: String, pass: String, onSuccess: () -> Unit) {
        if (email.isBlank() || pass.isBlank()) {
            _authError.value = "Email and password cannot be empty"
            return
        }
        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = null
            val result = authRepo.signUpWithEmail(name.trim(), email.trim(), pass)
            _authLoading.value = false
            result.fold(
                onSuccess = { onSuccess() },
                onFailure = { _authError.value = it.localizedMessage ?: "Registration failed" }
            )
        }
    }

    fun signInWithGoogle(idToken: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = null
            val result = authRepo.signInWithGoogleCredential(idToken)
            _authLoading.value = false
            result.fold(
                onSuccess = { onSuccess() },
                onFailure = { _authError.value = it.localizedMessage ?: "Google sign-in failed" }
            )
        }
    }

    fun getActiveUserId(): String? = _currentUser.value?.uid

    fun signOut() {
        authRepo.signOut()
        _userProfile.value = null
        _linkedAccounts.value = emptyList()
        _allFiles.value = emptyList()
        _storagePoolState.value = UnifiedStoragePoolState()
    }

    fun clearAuthError() {
        _authError.value = null
    }

    // --- 5-Slot Account Linker ---

    fun onGoogleAccountLinked(account: LinkedDriveAccount) {
        val current = _linkedAccounts.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.accountEmail.equals(account.accountEmail, ignoreCase = true) }
        if (existingIndex >= 0) {
            current[existingIndex] = account
        } else {
            if (current.size >= MultiDriveOAuthManager.MAX_SLOTS) {
                _uiMessage.value = "Maximum 5 Google Accounts reached."
                return
            }
            current.add(account)
        }
        _linkedAccounts.value = current
        oAuthManager.saveLinkedAccounts(current)

        val uid = getActiveUserId()
        if (uid != null) {
            viewModelScope.launch {
                authRepo.updateLinkedAccounts(uid, current)
            }
        }
        recalculatePool(current, _allFiles.value)

        // Immediately fetch live Drive storage quota for this account
        viewModelScope.launch(Dispatchers.IO) {
            val tokenResult = oAuthManager.getAccessToken(account.accountEmail)
            tokenResult.fold(
                onSuccess = {
                    val updated = storageRepo.refreshSingleAccountQuota(account)
                    val updatedList = _linkedAccounts.value.map {
                        if (it.accountEmail.equals(updated.accountEmail, ignoreCase = true)) updated else it
                    }
                    _linkedAccounts.value = updatedList
                    oAuthManager.saveLinkedAccounts(updatedList)
                    recalculatePool(updatedList, _allFiles.value)
                    if (uid != null) {
                        authRepo.updateLinkedAccounts(uid, updatedList)
                        storageRepo.syncAllDrivesToFirestore(uid, updatedList)
                    }
                    val freeStr = updated.formattedFreeStorage
                    _uiMessage.value = "Added ${updated.accountEmail} • $freeStr free left"
                },
                onFailure = { error ->
                    if (error is UserRecoverableAuthException) {
                        error.intent?.let { intent ->
                            _recoverableAuthIntent.tryEmit(intent)
                        }
                    }
                }
            )
        }
    }

    fun checkOrRequestDriveAuth(accountEmail: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = oAuthManager.getAccessToken(accountEmail)
            result.fold(
                onSuccess = {
                    refreshStoragePool()
                },
                onFailure = { error ->
                    if (error is UserRecoverableAuthException) {
                        error.intent?.let { intent ->
                            _recoverableAuthIntent.tryEmit(intent)
                        }
                    }
                }
            )
        }
    }

    fun refreshStoragePool() {
        viewModelScope.launch(Dispatchers.IO) {
            val current = _linkedAccounts.value
            if (current.isEmpty()) return@launch
            val refreshed = storageRepo.refreshLiveQuotas(current)
            _linkedAccounts.value = refreshed
            oAuthManager.saveLinkedAccounts(refreshed)
            recalculatePool(refreshed, _allFiles.value)
            val uid = getActiveUserId()
            if (uid != null) {
                authRepo.updateLinkedAccounts(uid, refreshed)
                storageRepo.syncAllDrivesToFirestore(uid, refreshed)
            }
            val totalFreeBytes = refreshed.sumOf { it.freeQuotaBytes }
            val formattedFree = LinkedDriveAccount.formatBytes(totalFreeBytes)
            _uiMessage.value = "Storage updated: $formattedFree available across ${refreshed.size} drives"
        }
    }

    fun removeLinkedAccount(accountEmail: String) {
        val current = _linkedAccounts.value.filterNot { it.accountEmail.equals(accountEmail, ignoreCase = true) }
        _linkedAccounts.value = current
        oAuthManager.removeLinkedAccount(accountEmail)

        val uid = getActiveUserId()
        if (uid != null) {
            viewModelScope.launch {
                authRepo.updateLinkedAccounts(uid, current)
            }
        }
        recalculatePool(current, _allFiles.value)
    }

    fun mergeAndAnalyzeStoragePool(onComplete: () -> Unit) {
        val uid = getActiveUserId() ?: return
        val current = _linkedAccounts.value
        if (current.isEmpty()) {
            _uiMessage.value = "Please link at least one Google Account first."
            return
        }

        viewModelScope.launch {
            _isMergingPool.value = true
            _mergeStatusText.value = "Verifying Drive permissions across accounts..."

            // Prompt consent for any account needing user authorization
            for (acc in current) {
                val tokenResult = oAuthManager.getAccessToken(acc.accountEmail)
                val ex = tokenResult.exceptionOrNull()
                if (ex is UserRecoverableAuthException) {
                    ex.intent?.let { intent ->
                        _recoverableAuthIntent.tryEmit(intent)
                    }
                }
            }

            _mergeStatusText.value = "Fetching live quotas from ${current.size} Google Drives..."

            val refreshedAccounts = storageRepo.refreshLiveQuotas(current)
            _linkedAccounts.value = refreshedAccounts
            oAuthManager.saveLinkedAccounts(refreshedAccounts)
            authRepo.updateLinkedAccounts(uid, refreshedAccounts)

            _mergeStatusText.value = "Discovering and synchronizing files..."
            storageRepo.syncAllDrivesToFirestore(uid, refreshedAccounts)

            _mergeStatusText.value = "Calculating unified 75 GB virtual pool..."
            recalculatePool(refreshedAccounts, _allFiles.value)

            _isMergingPool.value = false
            onComplete()
        }
    }

    private fun recalculatePool(accounts: List<LinkedDriveAccount>, files: List<UnifiedCloudFile>) {
        _storagePoolState.value = storageRepo.computeStoragePoolState(accounts, files)
    }

    // --- Search & Filter Controls ---

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCategorySelected(category: FileCategory?) {
        _selectedCategory.value = if (_selectedCategory.value == category) null else category
    }

    // --- Smart Non-Splitting Batch Upload ---

    fun handleSelectedUrisForUpload(uris: List<Uri>, contentResolver: ContentResolver) {
        if (uris.isEmpty()) return
        val uid = getActiveUserId() ?: return

        viewModelScope.launch {
            val candidateFiles = uris.mapNotNull { uri ->
                var name = "unknown_${System.currentTimeMillis()}"
                var size = 0L
                val mime = contentResolver.getType(uri) ?: "application/octet-stream"

                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex != -1) name = cursor.getString(nameIndex) ?: name
                        if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
                    }
                }
                UnifiedStorageRepository.CandidateFile(uri, name, size, mime)
            }

            if (candidateFiles.isEmpty()) return@launch

            val liveAccounts = storageRepo.refreshLiveQuotas(_linkedAccounts.value)
            _linkedAccounts.value = liveAccounts

            val plan = storageRepo.planBatchUpload(candidateFiles, liveAccounts)

            when (plan) {
                is UnifiedStorageRepository.AllocationResult.Failure -> {
                    _uploadState.value = BatchUploadUiState(
                        isUploading = false,
                        errorMessage = plan.reason
                    )
                }
                is UnifiedStorageRepository.AllocationResult.Success -> {
                    executeBatchUpload(uid, plan.allocations, contentResolver)
                }
            }
        }
    }

    private suspend fun executeBatchUpload(
        userId: String,
        allocations: List<UnifiedStorageRepository.UploadAllocation>,
        contentResolver: ContentResolver
    ) {
        val totalFiles = allocations.size
        _uploadState.value = BatchUploadUiState(
            isUploading = true,
            totalFiles = totalFiles,
            currentFileIndex = 0,
            overallProgressPercent = 0f
        )

        for ((index, alloc) in allocations.withIndex()) {
            val fileName = alloc.file.fileName
            _uploadState.value = _uploadState.value.copy(
                currentFileName = fileName,
                currentFileIndex = index + 1,
                currentFileProgressPercent = 0f
            )

            val result = storageRepo.uploadFileStreaming(
                userId = userId,
                allocation = alloc,
                contentResolver = contentResolver,
                onProgress = { filePercent, _, _ ->
                    val overall = ((index.toFloat() * 100f) + filePercent) / totalFiles.toFloat()
                    _uploadState.value = _uploadState.value.copy(
                        currentFileProgressPercent = filePercent,
                        overallProgressPercent = overall
                    )
                }
            )

            if (result.isFailure) {
                _uploadState.value = _uploadState.value.copy(
                    isUploading = false,
                    errorMessage = "Upload failed for '$fileName': ${result.exceptionOrNull()?.localizedMessage}"
                )
                return
            }
        }

        _uploadState.value = BatchUploadUiState(
            isUploading = false,
            isSuccess = true,
            overallProgressPercent = 100f
        )

        val refreshed = storageRepo.refreshLiveQuotas(_linkedAccounts.value)
        _linkedAccounts.value = refreshed
        recalculatePool(refreshed, _allFiles.value)
    }

    fun dismissUploadProgressDialog() {
        _uploadState.value = BatchUploadUiState()
    }

    // --- On-Demand File Download ---

    fun downloadFile(file: UnifiedCloudFile) {
        viewModelScope.launch {
            val result = storageRepo.downloadFileOnDemand(file)
            result.fold(
                onSuccess = {
                    _uiMessage.value = "Downloading '${file.fileName}' to Downloads folder..."
                },
                onFailure = {
                    _uiMessage.value = "Download failed: ${it.localizedMessage}"
                }
            )
        }
    }

    // --- Permanent Hard Delete (Bypass Trash) ---

    fun promptPermanentDelete(file: UnifiedCloudFile) {
        _filePendingDeletion.value = file
    }

    fun dismissPermanentDeleteDialog() {
        _filePendingDeletion.value = null
    }

    fun executePermanentDelete() {
        val file = _filePendingDeletion.value ?: return
        val uid = getActiveUserId() ?: return

        viewModelScope.launch {
            _isDeleting.value = true
            val result = storageRepo.permanentlyDeleteFile(uid, file)
            _isDeleting.value = false
            _filePendingDeletion.value = null

            result.fold(
                onSuccess = {
                    _uiMessage.value = "'${file.fileName}' permanently deleted. Quota reclaimed."
                    val refreshed = storageRepo.refreshLiveQuotas(_linkedAccounts.value)
                    _linkedAccounts.value = refreshed
                    recalculatePool(refreshed, _allFiles.value)
                },
                onFailure = {
                    _uiMessage.value = "Delete failed: ${it.localizedMessage}"
                }
            )
        }
    }

    // --- In-App Streaming Token Helpers ---

    suspend fun getAccessTokenForAccount(accountEmail: String): String? {
        return oAuthManager.getAccessToken(accountEmail).getOrNull()
    }

    fun clearUiMessage() {
        _uiMessage.value = null
    }
}

