package com.photosoap.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_stats")
data class UserStatsEntity(
    @PrimaryKey val id: Int = 1,
    val totalReviewed: Int = 0,
    val totalDeleted: Int = 0,
    val totalKept: Int = 0,
    val storageFreed: Long = 0L,
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
    val dailyChallengeType: String = "",
    val dailyChallengeDate: Long? = null,
)
