package com.photosoap.domain.model

data class StatsDisplay(
    val totalReviewed: Int = 0,
    val totalDeleted: Int = 0,
    val totalKept: Int = 0,
    val storageFreed: Long = 0L,
    val sessionReviewCount: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val dayStreak: Int = 0,
    val todayReviewCount: Int = 0,
    val bestDayReviewCount: Int = 0,
) {
    val keepDeleteRatio: Float get() {
        val total = totalKept + totalDeleted
        if (total == 0) return 0.5f
        return totalKept.toFloat() / total
    }

    val storageFreedFormatted: String get() {
        val bytes = storageFreed
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
            bytes < 1024L * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
            else -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
        }
    }
}
