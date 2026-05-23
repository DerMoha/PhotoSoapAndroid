package com.photosoap.di

import com.photosoap.data.repository.AchievementRepositoryImpl
import com.photosoap.data.repository.MetricsRepositoryImpl
import com.photosoap.data.repository.PhotoRepositoryImpl
import com.photosoap.data.repository.SettingsRepositoryImpl
import com.photosoap.data.repository.StatsRepositoryImpl
import com.photosoap.domain.repository.AchievementRepository
import com.photosoap.domain.repository.MetricsRepository
import com.photosoap.domain.repository.PhotoRepository
import com.photosoap.domain.repository.SettingsRepository
import com.photosoap.domain.repository.StatsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPhotoRepository(impl: PhotoRepositoryImpl): PhotoRepository

    @Binds
    @Singleton
    abstract fun bindStatsRepository(impl: StatsRepositoryImpl): StatsRepository

    @Binds
    @Singleton
    abstract fun bindAchievementRepository(impl: AchievementRepositoryImpl): AchievementRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindMetricsRepository(impl: MetricsRepositoryImpl): MetricsRepository
}
