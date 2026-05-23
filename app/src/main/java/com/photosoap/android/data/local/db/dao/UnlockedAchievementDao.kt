package com.photosoap.android.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.photosoap.android.data.local.db.entity.UnlockedAchievementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UnlockedAchievementDao {
    @Query("SELECT * FROM unlocked_achievements ORDER BY unlockDate DESC")
    fun observeAll(): Flow<List<UnlockedAchievementEntity>>

    @Query("SELECT achievement_id FROM unlocked_achievements")
    suspend fun getAllIds(): List<String>

    @Query("SELECT COUNT(*) FROM unlocked_achievements WHERE achievement_id = :id")
    suspend fun countById(id: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(achievement: UnlockedAchievementEntity)

    @Query("DELETE FROM unlocked_achievements")
    suspend fun deleteAll()
}
