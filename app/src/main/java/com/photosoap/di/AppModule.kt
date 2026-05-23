package com.photosoap.di

import com.photosoap.data.local.db.PhotoSoapDatabase
import com.photosoap.data.local.db.dao.ReviewedPhotoDao
import com.photosoap.data.local.db.dao.UnlockedAchievementDao
import com.photosoap.data.local.db.dao.UserStatsDao
import com.photosoap.data.local.datastore.SettingsDataStore
import com.photosoap.data.remote.api.MetricsApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton
import java.util.concurrent.TimeUnit

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://your-supabase-project.functions.supabase.co/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideMetricsApi(retrofit: Retrofit): MetricsApi =
        retrofit.create(MetricsApi::class.java)

    @Provides
    @Singleton
    fun provideUserStatsDao(db: PhotoSoapDatabase): UserStatsDao = db.userStatsDao()

    @Provides
    @Singleton
    fun provideReviewedPhotoDao(db: PhotoSoapDatabase): ReviewedPhotoDao = db.reviewedPhotoDao()

    @Provides
    @Singleton
    fun provideUnlockedAchievementDao(db: PhotoSoapDatabase): UnlockedAchievementDao =
        db.unlockedAchievementDao()
}
