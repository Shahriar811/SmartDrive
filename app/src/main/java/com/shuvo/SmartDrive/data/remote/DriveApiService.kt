package com.shuvo.SmartDrive.data.remote

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

/**
 * Retrofit REST API interface for Google Drive API v3.
 * Supports direct streaming, byte-range seeks, resumable uploads, and hard deletion.
 */
interface DriveApiService {

    /**
     * Fetches live storage quota and user info for the authenticated account.
     */
    @GET("drive/v3/about")
    suspend fun getAbout(
        @Header("Authorization") authHeader: String,
        @Query("fields") fields: String = "storageQuota,user"
    ): Response<DriveAboutResponse>

    /**
     * Queries files from Google Drive (filtering out trashed files).
     */
    @GET("drive/v3/files")
    suspend fun listFiles(
        @Header("Authorization") authHeader: String,
        @Query("q") query: String = "trashed = false and 'me' in owners",
        @Query("fields") fields: String = "files(id, name, mimeType, size, createdTime, thumbnailLink, webContentLink)",
        @Query("pageSize") pageSize: Int = 1000
    ): Response<DriveFileListResponse>

    /**
     * Initiates a resumable upload session.
     * The response returns the session URL in the HTTP 'Location' header.
     */
    @POST("upload/drive/v3/files?uploadType=resumable")
    suspend fun initiateResumableUpload(
        @Header("Authorization") authHeader: String,
        @Header("X-Upload-Content-Type") mimeType: String,
        @Header("X-Upload-Content-Length") contentLength: Long,
        @Body metadata: DriveFileMetadata
    ): Response<Unit>

    /**
     * Uploads the streamed file content to the resumable upload session URL.
     */
    @PUT
    suspend fun uploadToSessionUrl(
        @Url sessionUrl: String,
        @Header("Content-Range") contentRange: String,
        @Body fileStream: RequestBody
    ): Response<DriveFileItem>

    /**
     * Permanently deletes a file from Google Drive, completely bypassing the trash.
     * Quota is instantly freed.
     */
    @DELETE("drive/v3/files/{fileId}")
    suspend fun hardDeleteFile(
        @Header("Authorization") authHeader: String,
        @Path("fileId") fileId: String
    ): Response<Unit>

    /**
     * Streams file content directly without saving to disk.
     * Supports HTTP Range header for video seeking and partial streaming.
     */
    @Streaming
    @GET("drive/v3/files/{fileId}?alt=media")
    suspend fun streamFile(
        @Header("Authorization") authHeader: String,
        @Path("fileId") fileId: String,
        @Header("Range") rangeHeader: String? = null
    ): Response<ResponseBody>

    companion object {
        private const val BASE_URL = "https://www.googleapis.com/"

        fun create(): DriveApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.HEADERS
            }

            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(DriveApiService::class.java)
        }
    }
}

// Drive API Response Models

data class DriveAboutResponse(
    @SerializedName("storageQuota") val storageQuota: DriveStorageQuota?,
    @SerializedName("user") val user: DriveUser?
)

data class DriveStorageQuota(
    @SerializedName("limit") val limit: String?,
    @SerializedName("usage") val usage: String?,
    @SerializedName("usageInDrive") val usageInDrive: String?,
    @SerializedName("usageInDriveTrash") val usageInDriveTrash: String?
) {
    val limitBytes: Long
        get() = limit?.toLongOrNull() ?: (15L * 1024L * 1024L * 1024L)

    val usageBytes: Long
        get() = usage?.toLongOrNull() ?: 0L

    val freeBytes: Long
        get() = (limitBytes - usageBytes).coerceAtLeast(0L)
}

data class DriveUser(
    @SerializedName("displayName") val displayName: String?,
    @SerializedName("emailAddress") val emailAddress: String?,
    @SerializedName("photoLink") val photoLink: String?
)

data class DriveFileListResponse(
    @SerializedName("files") val files: List<DriveFileItem>?
)

data class DriveFileItem(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("mimeType") val mimeType: String,
    @SerializedName("size") val size: String?,
    @SerializedName("createdTime") val createdTime: String?,
    @SerializedName("thumbnailLink") val thumbnailLink: String?,
    @SerializedName("webContentLink") val webContentLink: String?
) {
    val sizeBytes: Long
        get() = size?.toLongOrNull() ?: 0L
}

data class DriveFileMetadata(
    @SerializedName("name") val name: String,
    @SerializedName("mimeType") val mimeType: String
)

