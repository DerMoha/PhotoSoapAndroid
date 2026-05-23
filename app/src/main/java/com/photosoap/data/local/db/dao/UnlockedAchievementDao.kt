package com.photosoap.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.photosoap.data.local.db.entity.UnlockedAchievementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UnlockedAchievementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(achievement: UnlockedAchievementEntity)

    @Query("SELECT * FROM unlocked_achievements ORDER BY unlockDate DESC")
    fun observeAll(): Flow<List<UnlockedAchievementEntity>>

    @Query("SELECT COUNT(*) FROM unlocked_achievements WHERE achievementId = :id")
    suspend fun contains(id: String): Int

    @Query("DELETE FROM unlocked_achievements")
    suspend fun deleteAll()
}
