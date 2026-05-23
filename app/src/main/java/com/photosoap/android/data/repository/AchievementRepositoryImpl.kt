package com.photosoap.android.data.repository

import com.photosoap.android.data.local.db.dao.UnlockedAchievementDao
import com.photosoap.android.data.local.db.entity.UnlockedAchievementEntity
import com.photosoap.android.domain.repository.AchievementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AchievementRepositoryImpl @Inject constructor(
    private val unlockedAchievementDao: UnlockedAchievementDao,
) : AchievementRepository {

    override fun observeUnlockedIds(): Flow<List<String>> {
        return unlockedAchievementDao.observeAll().map { entities ->
            entities.map { it.achievementId }
        }
    }

    override suspend fun getUnlockedIds(): List<String> {
        return unlockedAchievementDao.getAllIds()
    }

    override suspend fun unlock(id: String) {
        if (unlockedAchievementDao.countById(id) == 0) {
            unlockedAchievementDao.insert(
                UnlockedAchievementEntity(
                    achievementId = id,
                    unlockDate = System.currentTimeMillis(),
                )
            )
        }
    }

    override suspend fun isUnlocked(id: String): Boolean {
        return unlockedAchievementDao.countById(id) > 0
    }
}
