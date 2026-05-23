package com.photosoap.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.photosoap.data.local.db.entity.UserStatsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserStatsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stats: UserStatsEntity)

    @Query("SELECT * FROM user_stats LIMIT 1")
    suspend fun get(): UserStatsEntity?

    @Query("SELECT * FROM user_stats LIMIT 1")
    fun observe(): Flow<UserStatsEntity?>

    @Query("UPDATE user_stats SET totalReviewed = :count WHERE id = 1")
    suspend fun updateTotalReviewed(count: Int)

    @Query("UPDATE user_stats SET totalDeleted = :count WHERE id = 1")
    suspend fun updateTotalDeleted(count: Int)

    @Query("UPDATE user_stats SET totalKept = :count WHERE id = 1")
    suspend fun updateTotalKept(count: Int)

    @Query("UPDATE user_stats SET storageFreed = :bytes WHERE id = 1")
    suspend fun updateStorageFreed(bytes: Long)

    @Query("UPDATE user_stats SET sessionReviewCount = :count WHERE id = 1")
    suspend fun updateSessionReviewCount(count: Int)

    @Query("UPDATE user_stats SET currentStreak = :streak WHERE id = 1")
    suspend fun updateCurrentStreak(streak: Int)

    @Query("UPDATE user_stats SET bestStreak = :streak WHERE id = 1 AND bestStreak < :streak")
    suspend fun updateBestStreak(streak: Int)

    @Query("UPDATE user_stats SET dayStreak = :streak WHERE id = 1")
    suspend fun updateDayStreak(streak: Int)

    @Query("UPDATE user_stats SET lastReviewDate = :date WHERE id = 1")
    suspend fun updateLastReviewDate(date: Long)

    @Query("UPDATE user_stats SET todayReviewCount = :count, todayDate = :date WHERE id = 1")
    suspend fun updateTodayReview(count: Int, date: Long)

    @Query("UPDATE user_stats SET bestDayReviewCount = :count WHERE id = 1 AND bestDayReviewCount < :count")
    suspend fun updateBestDayReviewCount(count: Int)

    @Query("UPDATE user_stats SET dailyChallengeProgress = :progress WHERE id = 1")
    suspend fun updateDailyChallengeProgress(progress: Int)

    @Query("UPDATE user_stats SET dailyChallengeTarget = :target, dailyChallengeType = :type, dailyChallengeDate = :date WHERE id = 1")
    suspend fun updateDailyChallenge(target: Int, type: String, date: Long)

    @Query("DELETE FROM user_stats")
    suspend fun deleteAll()
}
