package com.shuvo.SmartDrive.data.model

import com.google.firebase.firestore.PropertyName
import java.util.Locale

/**
 * Represents one of up to 5 linked Google Drive accounts.
 * Standard free tier per account is 15 GB (16,106,127,360 bytes).
 */
data class LinkedDriveAccount(
    @get:PropertyName("accountEmail")
    @set:PropertyName("accountEmail")
    var accountEmail: String = "",

    @get:PropertyName("displayName")
    @set:PropertyName("displayName")
    var displayName: String = "",

    @get:PropertyName("avatarUrl")
    @set:PropertyName("avatarUrl")
    var avatarUrl: String = "",

    @get:PropertyName("totalQuotaBytes")
    @set:PropertyName("totalQuotaBytes")
    var totalQuotaBytes: Long = 15L * 1024L * 1024L * 1024L, // 15 GB

    @get:PropertyName("usedQuotaBytes")
    @set:PropertyName("usedQuotaBytes")
    var usedQuotaBytes: Long = 0L,

    @get:PropertyName("lastSyncTimestamp")
    @set:PropertyName("lastSyncTimestamp")
    var lastSyncTimestamp: Long = System.currentTimeMillis()
) {
    val freeQuotaBytes: Long
        get() = (totalQuotaBytes - usedQuotaBytes).coerceAtLeast(0L)

    val usageFraction: Float
        get() = if (totalQuotaBytes > 0L) {
            (usedQuotaBytes.toFloat() / totalQuotaBytes.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    val formattedUsedStorage: String
        get() = formatBytes(usedQuotaBytes)

    val formattedTotalStorage: String
        get() = formatBytes(totalQuotaBytes)

    val formattedFreeStorage: String
        get() = formatBytes(freeQuotaBytes)

    fun toMap(): Map<String, Any> {
        return mapOf(
            "accountEmail" to accountEmail,
            "displayName" to displayName,
            "avatarUrl" to avatarUrl,
            "totalQuotaBytes" to totalQuotaBytes,
            "usedQuotaBytes" to usedQuotaBytes,
            "lastSyncTimestamp" to lastSyncTimestamp
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): LinkedDriveAccount {
            return LinkedDriveAccount(
                accountEmail = map["accountEmail"] as? String ?: "",
                displayName = map["displayName"] as? String ?: "",
                avatarUrl = map["avatarUrl"] as? String ?: "",
                totalQuotaBytes = (map["totalQuotaBytes"] as? Number)?.toLong() ?: (15L * 1024L * 1024L * 1024L),
                usedQuotaBytes = (map["usedQuotaBytes"] as? Number)?.toLong() ?: 0L,
                lastSyncTimestamp = (map["lastSyncTimestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }

        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0.0 GB"
            val gigabytes = bytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
            return if (gigabytes >= 0.1) {
                String.format(Locale.US, "%.1f GB", gigabytes)
            } else {
                val megabytes = bytes.toDouble() / (1024.0 * 1024.0)
                String.format(Locale.US, "%.1f MB", megabytes)
            }
        }
    }
}

