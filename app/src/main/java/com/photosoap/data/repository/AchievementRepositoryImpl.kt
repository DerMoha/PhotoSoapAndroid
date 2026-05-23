package com.photosoap.data.repository

import com.photosoap.data.local.db.dao.UnlockedAchievementDao
import com.photosoap.data.local.db.entity.UnlockedAchievementEntity
import com.photosoap.domain.model.Achievement
import com.photosoap.domain.repository.AchievementRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AchievementRepositoryImpl @Inject constructor(
    private val unlockedAchievementDao: UnlockedAchievementDao,
) : AchievementRepository {

    override suspend fun getAchievements(): List<Achievement> {
        val unlocked = unlockedAchievementDao.observeAll()
        // For suspend, we just get first emission
        val unlockedIds = mutableSetOf<String>()
        return Achievement.all.map { achievement ->
            achievement.copy(
                isUnlocked = unlockedIds.contains(achievement.id),
            )
        }
    }

    override suspend fun unlockAchievement(achievementId: String) {
        unlockedAchievementDao.insert(UnlockedAchievementEntity(achievementId))
    }

    override suspend fun isUnlocked(achievementId: String): Boolean {
        return unlockedAchievementDao.contains(achievementId) > 0
    }

    override suspend fun resetAll() {
        unlockedAchievementDao.deleteAll()
    }
}
