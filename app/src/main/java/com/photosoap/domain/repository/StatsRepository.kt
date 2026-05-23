package com.photosoap.domain.repository

import com.photosoap.data.local.db.entity.UserStatsEntity

interface StatsRepository {
    suspend fun getStats(): UserStatsEntity
    suspend fun updateStats(stats: UserStatsEntity)
    suspend fun incrementReviewed()
    suspend fun incrementDeleted(fileSize: Long)
    suspend fun incrementKept()
    suspend fun resetStreak()
    suspend fun updateSessionReviewCount(count: Int)
    suspend fun updateBestStreak(streak: Int)
    suspend fun updateDayStreak(streak: Int)
    suspend fun updateTodayReviewCount(count: Int)
    suspend fun updateBestDayReviewCount(count: Int)
    suspend fun updateDailyChallengeProgress(progress: Int)
    suspend fun updateDailyChallenge(target: Int, type: String, date: Long)
}
