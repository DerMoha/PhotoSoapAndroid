package com.photosoap.android.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.photosoap.android.data.local.db.dao.ReviewedPhotoDao
import com.photosoap.android.data.local.db.dao.UnlockedAchievementDao
import com.photosoap.android.data.local.db.dao.UserStatsDao
import com.photosoap.android.data.local.db.entity.ReviewedPhotoEntity
import com.photosoap.android.data.local.db.entity.UnlockedAchievementEntity
import com.photosoap.android.data.local.db.entity.UserStatsEntity

@Database(
    entities = [
        UserStatsEntity::class,
        ReviewedPhotoEntity::class,
        UnlockedAchievementEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class PhotoSoapDatabase : RoomDatabase() {
    abstract fun userStatsDao(): UserStatsDao
    abstract fun reviewedPhotoDao(): ReviewedPhotoDao
    abstract fun unlockedAchievementDao(): UnlockedAchievementDao
}
