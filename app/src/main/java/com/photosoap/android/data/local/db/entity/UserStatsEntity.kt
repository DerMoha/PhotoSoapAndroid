package com.photosoap.android.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_stats")
data class UserStatsEntity(
    @PrimaryKey
    val id: Int = 1,
    @ColumnInfo(defaultValue = "0")
    val photosReviewed: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val photosKept: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val photosDeleted: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val photoStorageFreed: Long = 0,
    @ColumnInfo(defaultValue = "0")
    val videosReviewed: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val videosKept: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val videosDeleted: Int = 0,
    @ColumnInfo(defaultValue = "0")
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
