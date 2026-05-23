package com.photosoap.android.di

import android.content.Context
import androidx.room.Room
import com.photosoap.android.data.local.db.PhotoSoapDatabase
import com.photosoap.android.data.local.db.dao.ReviewedPhotoDao
import com.photosoap.android.data.local.db.dao.UnlockedAchievementDao
import com.photosoap.android.data.local.db.dao.UserStatsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PhotoSoapDatabase {
        return Room.databaseBuilder(
            context,
            PhotoSoapDatabase::class.java,
            "photosoap.db",
        ).fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideUserStatsDao(db: PhotoSoapDatabase): UserStatsDao = db.userStatsDao()

    @Provides
    fun provideReviewedPhotoDao(db: PhotoSoapDatabase): ReviewedPhotoDao = db.reviewedPhotoDao()

    @Provides
    fun provideUnlockedAchievementDao(db: PhotoSoapDatabase): UnlockedAchievementDao =
        db.unlockedAchievementDao()
}
