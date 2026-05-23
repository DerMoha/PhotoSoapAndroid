package com.photosoap.android.domain.model

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val iconName: String,
    val isUnlocked: (UserStats) -> Boolean,
) {
    companion object {
        val ALL = listOf(
            Achievement(
                id = "first_steps",
                title = "First Steps",
                description = "Review 50 photos",
                iconName = "👣",
            ) { it.totalReviewed >= 50 },
            Achievement(
                id = "spring_cleaning",
                title = "Spring Cleaning",
                description = "Delete 200 photos",
                iconName = "🧹",
            ) { it.totalDeleted >= 200 },
            Achievement(
                id = "memory_keeper",
                title = "Memory Keeper",
                description = "Keep 500 photos",
                iconName = "💾",
            ) { it.totalKept >= 500 },
            Achievement(
                id = "streak_master",
                title = "Streak Master",
                description = "Build a streak of 100",
                iconName = "🔥",
            ) { it.bestStreak >= 100 },
            Achievement(
                id = "daily_devotee",
                title = "Daily Devotee",
                description = "Review photos 14 days in a row",
                iconName = "📅",
            ) { it.dayStreak >= 14 },
            Achievement(
                id = "storage_saver",
                title = "Storage Saver",
                description = "Free 5 GB of storage",
                iconName = "💿",
            ) { it.storageFreed >= 5_000_000_000L },
            Achievement(
                id = "century_club",
                title = "Century Club",
                description = "Review 500 photos in one session",
                iconName = "💯",
            ) { it.sessionReviewCount >= 500 },
            Achievement(
                id = "photo_pro",
                title = "Photo Pro",
                description = "Review 5,000 photos total",
                iconName = "📸",
            ) { it.totalReviewed >= 5000 },
            Achievement(
                id = "decisive",
                title = "Decisive",
                description = "Build a streak of 200 consecutive reviews",
                iconName = "⚡",
            ) { it.bestStreak >= 200 },
            Achievement(
                id = "cleanup_champion",
                title = "Cleanup Champion",
                description = "Delete 2,000 photos",
                iconName = "🏆",
            ) { it.totalDeleted >= 2000 },
        )
    }
}

data class UserStats(
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
