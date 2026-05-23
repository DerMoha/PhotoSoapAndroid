package com.photosoap.data.repository

import com.photosoap.data.local.db.dao.UserStatsDao
import com.photosoap.data.local.db.entity.UserStatsEntity
import com.photosoap.domain.repository.StatsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatsRepositoryImpl @Inject constructor(
    private val userStatsDao: UserStatsDao,
) : StatsRepository {

    override suspend fun getStats(): UserStatsEntity {
        return userStatsDao.get() ?: UserStatsEntity().also { userStatsDao.upsert(it) }
    }

    override suspend fun updateStats(stats: UserStatsEntity) {
        userStatsDao.upsert(stats)
    }

    override suspend fun incrementReviewed() {
        val stats = getStats()
        userStatsDao.upsert(
            stats.copy(
                totalReviewed = stats.totalReviewed + 1,
                sessionReviewCount = stats.sessionReviewCount + 1,
            )
        )
    }

    override suspend fun incrementDeleted(fileSize: Long) {
        val stats = getStats()
        userStatsDao.upsert(
            stats.copy(
                totalDeleted = stats.totalDeleted + 1,
                storageFreed = stats.storageFreed + fileSize,
            )
        )
    }

    override suspend fun incrementKept() {
        val stats = getStats()
        userStatsDao.upsert(
            stats.copy(totalKept = stats.totalKept + 1)
        )
    }

    override suspend fun resetStreak() {
        val stats = getStats()
        userStatsDao.upsert(
            stats.copy(
                currentStreak = 0,
                sessionReviewCount = stats.sessionReviewCount + 1, // keep session hint
            )
        )
    }

    override suspend fun updateSessionReviewCount(count: Int) {
        userStatsDao.updateSessionReviewCount(count)
    }

    override suspend fun updateBestStreak(streak: Int) {
        userStatsDao.updateBestStreak(streak)
    }

    override suspend fun updateDayStreak(streak: Int) {
        userStatsDao.updateDayStreak(streak)
    }

    override suspend fun updateTodayReviewCount(count: Int) {
        userStatsDao.updateTodayReview(count, System.currentTimeMillis())
    }

    override suspend fun updateBestDayReviewCount(count: Int) {
        userStatsDao.updateBestDayReviewCount(count)
    }

    override suspend fun updateDailyChallengeProgress(progress: Int) {
        userStatsDao.updateDailyChallengeProgress(progress)
    }

    override suspend fun updateDailyChallenge(target: Int, type: String, date: Long) {
        userStatsDao.updateDailyChallenge(target, type, date)
    }
}
