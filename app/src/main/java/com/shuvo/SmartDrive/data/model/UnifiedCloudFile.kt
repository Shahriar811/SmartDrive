package com.shuvo.SmartDrive.data.model

import com.google.firebase.firestore.PropertyName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Unified virtual file entry across the 5 Google Drive accounts.
 * Indexed in Firestore at `users/{userId}/files/{fileId}`.
 */
data class UnifiedCloudFile(
    @get:PropertyName("id")
    @set:PropertyName("id")
    var id: String = "",

    @get:PropertyName("driveFileId")
    @set:PropertyName("driveFileId")
    var driveFileId: String = "",

    @get:PropertyName("fileName")
    @set:PropertyName("fileName")
    var fileName: String = "",

    @get:PropertyName("mimeType")
    @set:PropertyName("mimeType")
    var mimeType: String = "",

    @get:PropertyName("size")
    @set:PropertyName("size")
    var size: Long = 0L,

    @get:PropertyName("ownerAccountEmail")
    @set:PropertyName("ownerAccountEmail")
    var ownerAccountEmail: String = "",

    @get:PropertyName("createdAt")
    @set:PropertyName("createdAt")
    var createdAt: Long = System.currentTimeMillis(),

    @get:PropertyName("thumbnailUrl")
    @set:PropertyName("thumbnailUrl")
    var thumbnailUrl: String? = null,

    @get:PropertyName("webContentLink")
    @set:PropertyName("webContentLink")
    var webContentLink: String? = null
) {
    val category: FileCategory
        get() = FileCategory.fromMimeType(mimeType, fileName)

    val formattedSize: String
        get() = formatBytes(size)

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            return sdf.format(Date(createdAt))
        }

    val fileExtension: String
        get() = fileName.substringAfterLast('.', "").uppercase(Locale.getDefault())

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "driveFileId" to driveFileId,
            "fileName" to fileName,
            "mimeType" to mimeType,
            "size" to size,
            "ownerAccountEmail" to ownerAccountEmail,
            "createdAt" to createdAt,
            "thumbnailUrl" to thumbnailUrl,
            "webContentLink" to webContentLink
        )
    }

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
                mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
                kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
                else -> "$bytes B"
            }
        }
    }
}

enum class FileCategory(val displayName: String, val colorHex: Long) {
    VIDEOS("Videos", 0xFFEB459E),          // Discord Magenta/Pink
    PHOTOS("Photos", 0xFF57F287),          // Discord Green
    DOCUMENTS("Documents", 0xFFFEE75C),    // Discord Yellow
    TEXT_AND_CODE("Text & Code", 0xFF00A8FC), // Discord Cyan
    AUDIO_AND_OTHERS("Audio & Others", 0xFFED4245); // Discord Red

    companion object {
        fun fromMimeType(mimeType: String, fileName: String = ""): FileCategory {
            val lowerMime = mimeType.lowercase(Locale.ROOT)
            val lowerName = fileName.lowercase(Locale.ROOT)

            return when {
                lowerMime.startsWith("video/") ||
                        lowerName.endsWith(".mp4") || lowerName.endsWith(".mkv") ||
                        lowerName.endsWith(".mov") || lowerName.endsWith(".webm") ||
                        lowerName.endsWith(".avi") -> VIDEOS

                lowerMime.startsWith("image/") ||
                        lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") ||
                        lowerName.endsWith(".png") || lowerName.endsWith(".webp") ||
                        lowerName.endsWith(".gif") || lowerName.endsWith(".bmp") -> PHOTOS

                lowerMime == "application/pdf" ||
                        lowerMime.contains("officedocument") ||
                        lowerMime.contains("msword") ||
                        lowerMime.contains("powerpoint") ||
                        lowerMime.contains("presentation") ||
                        lowerMime.contains("spreadsheet") ||
                        lowerMime.contains("google-apps.document") ||
                        lowerMime.contains("google-apps.spreadsheet") ||
                        lowerMime.contains("google-apps.presentation") ||
                        lowerName.endsWith(".pdf") || lowerName.endsWith(".doc") ||
                        lowerName.endsWith(".docx") || lowerName.endsWith(".xls") ||
                        lowerName.endsWith(".xlsx") || lowerName.endsWith(".ppt") ||
                        lowerName.endsWith(".pptx") -> DOCUMENTS

                lowerMime.startsWith("text/") ||
                        lowerMime == "application/json" ||
                        lowerMime == "application/xml" ||
                        lowerMime == "application/javascript" ||
                        lowerMime == "application/x-yaml" ||
                        lowerName.endsWith(".txt") || lowerName.endsWith(".md") ||
                        lowerName.endsWith(".json") || lowerName.endsWith(".kt") ||
                        lowerName.endsWith(".java") || lowerName.endsWith(".py") ||
                        lowerName.endsWith(".js") || lowerName.endsWith(".ts") ||
                        lowerName.endsWith(".xml") || lowerName.endsWith(".html") ||
                        lowerName.endsWith(".css") || lowerName.endsWith(".csv") ||
                        lowerName.endsWith(".yaml") || lowerName.endsWith(".yml") -> TEXT_AND_CODE

                else -> AUDIO_AND_OTHERS
            }
        }
    }
}

