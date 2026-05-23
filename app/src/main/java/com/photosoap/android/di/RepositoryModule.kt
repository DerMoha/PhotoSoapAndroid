package com.photosoap.android.di

import com.photosoap.android.data.local.datastore.SettingsDataStore
import com.photosoap.android.data.local.db.dao.ReviewedPhotoDao
import com.photosoap.android.data.local.db.dao.UnlockedAchievementDao
import com.photosoap.android.data.local.db.dao.UserStatsDao
import com.photosoap.android.data.remote.SupabaseApi
import com.photosoap.android.data.repository.AchievementRepositoryImpl
import com.photosoap.android.data.repository.MetricsRepositoryImpl
import com.photosoap.android.data.repository.PhotoRepositoryImpl
import com.photosoap.android.data.repository.SettingsRepositoryImpl
import com.photosoap.android.data.repository.StatsRepositoryImpl
import com.photosoap.android.domain.repository.AchievementRepository
import com.photosoap.android.domain.repository.MetricsRepository
import com.photosoap.android.domain.repository.PhotoRepository
import com.photosoap.android.domain.repository.SettingsRepository
import com.photosoap.android.domain.repository.StatsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideStatsRepository(
        userStatsDao: UserStatsDao,
    ): StatsRepository = StatsRepositoryImpl(userStatsDao)

    @Provides
    @Singleton
    fun provideAchievementRepository(
        unlockedAchievementDao: UnlockedAchievementDao,
    ): AchievementRepository = AchievementRepositoryImpl(unlockedAchievementDao)

    @Provides
    @Singleton
    fun providePhotoRepository(
        reviewedPhotoDao: ReviewedPhotoDao,
    ): PhotoRepository = PhotoRepositoryImpl(reviewedPhotoDao)

    @Provides
    @Singleton
    fun provideSettingsRepository(
        settingsDataStore: SettingsDataStore,
    ): SettingsRepository = SettingsRepositoryImpl(settingsDataStore)

    @Provides
    @Singleton
    fun provideMetricsRepository(
        settingsDataStore: SettingsDataStore,
        supabaseApi: SupabaseApi,
    ): MetricsRepository = MetricsRepositoryImpl(settingsDataStore, supabaseApi)
}
