package com.photosoap.android.data.repository

import com.photosoap.android.data.local.db.dao.UserStatsDao
import com.photosoap.android.data.local.db.entity.UserStatsEntity
import com.photosoap.android.domain.model.UserStats
import com.photosoap.android.domain.repository.StatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class StatsRepositoryImpl @Inject constructor(
    private val userStatsDao: UserStatsDao,
) : StatsRepository {

    override fun observeStats(): Flow<UserStats?> {
        return userStatsDao.observe().map { it?.toDomain() }
    }

    override suspend fun getStats(): UserStats? {
        return userStatsDao.get()?.toDomain()
    }

    override suspend fun updateStats(stats: UserStats) {
        userStatsDao.upsert(stats.toEntity())
    }

    override suspend fun createIfNeeded(): UserStats {
        val existing = userStatsDao.get()
        if (existing != null) return existing.toDomain()
        val newStats = UserStatsEntity()
        userStatsDao.upsert(newStats)
        return newStats.toDomain()
    }
}

fun UserStatsEntity.toDomain(): UserStats = UserStats(
    totalReviewed = totalReviewed,
    totalDeleted = totalDeleted,
    totalKept = totalKept,
    storageFreed = storageFreed,
    sessionReviewCount = sessionReviewCount,
    currentStreak = currentStreak,
    bestStreak = bestStreak,
    dayStreak = dayStreak,
    lastReviewDate = lastReviewDate,
    todayReviewCount = todayReviewCount,
    todayDate = todayDate,
    bestDayReviewCount = bestDayReviewCount,
    dailyChallengeProgress = dailyChallengeProgress,
    dailyChallengeTarget = dailyChallengeTarget,
    dailyChallengeType = dailyChallengeType,
    dailyChallengeDate = dailyChallengeDate,
)

fun UserStats.toEntity(): UserStatsEntity = UserStatsEntity(
    totalReviewed = totalReviewed,
    totalDeleted = totalDeleted,
    totalKept = totalKept,
    storageFreed = storageFreed,
    sessionReviewCount = sessionReviewCount,
    currentStreak = currentStreak,
    bestStreak = bestStreak,
    dayStreak = dayStreak,
    lastReviewDate = lastReviewDate,
    todayReviewCount = todayReviewCount,
    todayDate = todayDate,
    bestDayReviewCount = bestDayReviewCount,
    dailyChallengeProgress = dailyChallengeProgress,
    dailyChallengeTarget = dailyChallengeTarget,
    dailyChallengeType = dailyChallengeType,
    dailyChallengeDate = dailyChallengeDate,
)
