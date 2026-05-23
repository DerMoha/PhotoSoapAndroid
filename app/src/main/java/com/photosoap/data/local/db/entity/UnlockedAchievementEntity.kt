package com.photosoap.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "unlocked_achievements",
    primaryKeys = ["achievementId"],
    indices = [Index(value = ["achievementId"], unique = true)],
)
data class UnlockedAchievementEntity(
    val achievementId: String,
    val unlockDate: Long = System.currentTimeMillis(),
)
