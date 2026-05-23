package com.photosoap.android.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "unlocked_achievements",
    indices = [Index(value = ["achievement_id"], unique = true)],
)
data class UnlockedAchievementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "achievement_id")
    val achievementId: String,
    val unlockDate: Long,
)
