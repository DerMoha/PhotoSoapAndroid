package com.photosoap.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.photosoap.data.local.db.entity.ReviewedPhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewedPhotoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: ReviewedPhotoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(photos: List<ReviewedPhotoEntity>)

    @Query("SELECT COUNT(*) FROM reviewed_photos")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM reviewed_photos WHERE photoId = :photoId")
    suspend fun contains(photoId: Long): Int

    @Query("SELECT * FROM reviewed_photos ORDER BY reviewDate DESC")
    fun observeAll(): Flow<List<ReviewedPhotoEntity>>

    @Query("SELECT * FROM reviewed_photos WHERE photoId = :photoId LIMIT 1")
    suspend fun getByPhotoId(photoId: Long): ReviewedPhotoEntity?

    @Query("DELETE FROM reviewed_photos WHERE photoId = :photoId")
    suspend fun delete(photoId: Long)

    @Query("DELETE FROM reviewed_photos")
    suspend fun deleteAll()
}
