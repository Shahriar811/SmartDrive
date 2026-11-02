package com.shuvo.SmartDrive.data.model

import java.util.Locale

/**
 * Summary breakdown of storage consumption for a specific file category
 * across all connected Google Drive accounts.
 */
data class StorageCategorySummary(
    val category: FileCategory,
    val totalBytes: Long,
    val fileCount: Int,
    val percentageOfTotal: Float // 0f to 100f
) {
    val displayName: String
        get() = category.displayName

    val colorHex: Long
        get() = category.colorHex

    val formattedTotalSize: String
        get() {
            if (totalBytes <= 0) return "0.0 GB"
            val gb = totalBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
            return if (gb >= 0.01) {
                String.format(Locale.US, "%.2f GB", gb)
            } else {
                val mb = totalBytes.toDouble() / (1024.0 * 1024.0)
                String.format(Locale.US, "%.1f MB", mb)
            }
        }
}

/**
 * Master Storage Pool Snapshot:
 * Aggregates up to 5 accounts (total 75 GB = 80,530,636,800 bytes).
 */
data class UnifiedStoragePoolState(
    val totalQuotaBytes: Long = 75L * 1024L * 1024L * 1024L, // 75 GB
    val usedQuotaBytes: Long = 0L,
    val maxSingleFileCapacityBytes: Long = 15L * 1024L * 1024L * 1024L,
    val connectedAccountCount: Int = 0,
    val categories: List<StorageCategorySummary> = emptyList(),
    val isAnalyzing: Boolean = false,
    val errorMessage: String? = null
) {
    val freeQuotaBytes: Long
        get() = (totalQuotaBytes - usedQuotaBytes).coerceAtLeast(0L)

    val usedPercentage: Float
        get() = if (totalQuotaBytes > 0L) {
            (usedQuotaBytes.toFloat() / totalQuotaBytes.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    val formattedUsed: String
        get() {
            val gb = usedQuotaBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
            return String.format(Locale.US, "%.1f GB", gb)
        }

    val formattedFree: String
        get() {
            val gb = freeQuotaBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
            return String.format(Locale.US, "%.1f GB", gb)
        }

    val formattedTotal: String
        get() {
            val gb = totalQuotaBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
            return String.format(Locale.US, "%.1f GB", gb)
        }

    val formattedMaxSingleFile: String
        get() {
            val gb = maxSingleFileCapacityBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
            return if (gb >= 0.1) {
                String.format(Locale.US, "%.1f GB", gb)
            } else {
                val mb = maxSingleFileCapacityBytes.toDouble() / (1024.0 * 1024.0)
                String.format(Locale.US, "%.1f MB", mb)
            }
        }
}

