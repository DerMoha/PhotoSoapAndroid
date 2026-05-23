package com.photosoap.domain.model

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val iconName: String,
    val isUnlocked: Boolean = false,
    val progress: Float = 0f,
    val unlockDate: Long? = null,
) {
    companion object {
        val all = listOf(
            Achievement(
                id = "first_steps",
                title = "First Steps",
                description = "Review 50 photos",
                iconName = "👣",
            ),
            Achievement(
                id = "spring_cleaning",
                title = "Spring Cleaning",
                description = "Delete 200 photos",
                iconName = "🧹",
            ),
            Achievement(
                id = "memory_keeper",
                title = "Memory Keeper",
                description = "Keep 500 photos",
                iconName = "💾",
            ),
            Achievement(
                id = "streak_master",
                title = "Streak Master",
                description = "Reach a best streak of 100",
                iconName = "🔥",
            ),
            Achievement(
                id = "daily_devotee",
                title = "Daily Devotee",
                description = "Review photos 14 days in a row",
                iconName = "📅",
            ),
            Achievement(
                id = "storage_saver",
                title = "Storage Saver",
                description = "Free 5 GB of storage",
                iconName = "💿",
            ),
            Achievement(
                id = "century_club",
                title = "Century Club",
                description = "Review 500 photos in one session",
                iconName = "💯",
            ),
            Achievement(
                id = "photo_pro",
                title = "Photo Pro",
                description = "Review 5,000 photos total",
                iconName = "📸",
            ),
            Achievement(
                id = "decisive",
                title = "Decisive",
                description = "Reach 200 consecutive actions",
                iconName = "⚡",
            ),
            Achievement(
                id = "cleanup_champion",
                title = "Cleanup Champion",
                description = "Delete 2,000 photos",
                iconName = "🏆",
            ),
        )
    }
}
