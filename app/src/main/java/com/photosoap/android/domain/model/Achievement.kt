package com.photosoap.android.domain.model

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val iconName: String,
    val isUnlocked: (UserStats) -> Boolean,
    val progress: (UserStats) -> Float = { if (isUnlocked(it)) 1f else 0f },
) {
    companion object {
        val ALL = listOf(
            Achievement(
                id = "first_steps",
                title = "First Steps",
                description = "Review 50 photos",
                iconName = "👣",
                isUnlocked = { it.totalReviewed >= 50 },
            ).withProgress { it.totalReviewed.toFloat() / 50f },
            Achievement(
                id = "spring_cleaning",
                title = "Spring Cleaning",
                description = "Delete 200 photos",
                iconName = "🧹",
                isUnlocked = { it.totalDeleted >= 200 },
            ).withProgress { it.totalDeleted.toFloat() / 200f },
            Achievement(
                id = "memory_keeper",
                title = "Memory Keeper",
                description = "Keep 500 photos",
                iconName = "💾",
                isUnlocked = { it.totalKept >= 500 },
            ).withProgress { it.totalKept.toFloat() / 500f },
            Achievement(
                id = "streak_master",
                title = "Streak Master",
                description = "Build a streak of 100",
                iconName = "🔥",
                isUnlocked = { it.bestStreak >= 100 },
            ).withProgress { it.bestStreak.toFloat() / 100f },
            Achievement(
                id = "daily_devotee",
                title = "Daily Devotee",
                description = "Review photos 14 days in a row",
                iconName = "📅",
                isUnlocked = { it.dayStreak >= 14 },
            ).withProgress { it.dayStreak.toFloat() / 14f },
            Achievement(
                id = "storage_saver",
                title = "Storage Saver",
                description = "Free 5 GB of storage",
                iconName = "💿",
                isUnlocked = { it.storageFreed >= 5_000_000_000L },
            ).withProgress { it.storageFreed.toFloat() / 5_000_000_000f },
            Achievement(
                id = "century_club",
                title = "Century Club",
                description = "Review 500 photos in one session",
                iconName = "💯",
                isUnlocked = { it.sessionReviewCount >= 500 },
            ).withProgress { it.sessionReviewCount.toFloat() / 500f },
            Achievement(
                id = "photo_pro",
                title = "Photo Pro",
                description = "Review 5,000 photos total",
                iconName = "📸",
                isUnlocked = { it.totalReviewed >= 5000 },
            ).withProgress { it.totalReviewed.toFloat() / 5000f },
            Achievement(
                id = "decisive",
                title = "Decisive",
                description = "Build a streak of 200 consecutive reviews",
                iconName = "⚡",
                isUnlocked = { it.bestStreak >= 200 },
            ).withProgress { it.bestStreak.toFloat() / 200f },
            Achievement(
                id = "cleanup_champion",
                title = "Cleanup Champion",
                description = "Delete 2,000 photos",
                iconName = "🏆",
                isUnlocked = { it.totalDeleted >= 2000 },
            ).withProgress { it.totalDeleted.toFloat() / 2000f },
        )
    }
}

private fun Achievement.withProgress(p: (UserStats) -> Float): Achievement {
    return copy(progress = p)
}

data class UserStats(
    val photosReviewed: Int = 0,
    val photosKept: Int = 0,
    val photosDeleted: Int = 0,
    val photoStorageFreed: Long = 0,
    val videosReviewed: Int = 0,
    val videosKept: Int = 0,
    val videosDeleted: Int = 0,
    val videoStorageFreed: Long = 0,
    val totalReviewed: Int = 0,
    val totalDeleted: Int = 0,
    val totalKept: Int = 0,
    val storageFreed: Long = 0,
    val sessionReviewCount: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val dayStreak: Int = 0,
    val lastReviewDate: Long? = null,
    val todayReviewCount: Int = 0,
    val todayDate: Long? = null,
    val bestDayReviewCount: Int = 0,
    val dailyChallengeProgress: Int = 0,
    val dailyChallengeTarget: Int = 0,
    val dailyChallengeType: String = "review",
    val dailyChallengeDate: Long? = null,
)
