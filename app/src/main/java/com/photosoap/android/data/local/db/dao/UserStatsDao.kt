package com.photosoap.android.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.photosoap.android.data.local.db.entity.ProcessedDeletionEntity
import com.photosoap.android.data.local.db.entity.UserStatsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserStatsDao {
    @Query("SELECT * FROM user_stats WHERE id = 1")
    fun observe(): Flow<UserStatsEntity?>

    @Query("SELECT * FROM user_stats WHERE id = 1")
    suspend fun get(): UserStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stats: UserStatsEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProcessedDeletion(deletion: ProcessedDeletionEntity): Long

    @Query(
        "DELETE FROM processed_deletions WHERE requestId NOT IN " +
            "(SELECT requestId FROM processed_deletions ORDER BY processedAt DESC LIMIT 256)",
    )
    suspend fun pruneProcessedDeletions()

    @Transaction
    suspend fun applyDeletionOnce(requestId: String, stats: UserStatsEntity): Boolean {
        if (insertProcessedDeletion(ProcessedDeletionEntity(requestId)) == -1L) return false
        upsert(stats)
        pruneProcessedDeletions()
        return true
    }

    @Query("DELETE FROM user_stats")
    suspend fun deleteAll()
}
