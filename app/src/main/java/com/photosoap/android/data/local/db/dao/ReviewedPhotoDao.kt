package com.photosoap.android.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.photosoap.android.data.local.db.entity.ReviewedPhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewedPhotoDao {
    @Query("SELECT * FROM reviewed_photos ORDER BY reviewDate DESC")
    fun observeAll(): Flow<List<ReviewedPhotoEntity>>

    @Query("SELECT COUNT(*) FROM reviewed_photos WHERE photo_uri = :uri")
    suspend fun countByUri(uri: String): Int

    @Query("SELECT * FROM reviewed_photos WHERE photo_uri = :uri LIMIT 1")
    suspend fun getByUri(uri: String): ReviewedPhotoEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(photo: ReviewedPhotoEntity)

    @Query("DELETE FROM reviewed_photos WHERE photo_uri = :uri")
    suspend fun deleteByUri(uri: String)

    @Query("DELETE FROM reviewed_photos")
    suspend fun deleteAll()
}
