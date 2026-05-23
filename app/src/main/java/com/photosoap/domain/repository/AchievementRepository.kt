package com.photosoap.domain.repository

import com.photosoap.domain.model.Achievement

interface AchievementRepository {
    suspend fun getAchievements(): List<Achievement>
    suspend fun unlockAchievement(achievementId: String)
    suspend fun isUnlocked(achievementId: String): Boolean
    suspend fun resetAll()
}
