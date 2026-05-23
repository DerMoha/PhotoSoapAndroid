package com.photosoap.android.di

import com.photosoap.android.data.remote.SupabaseApi
import com.photosoap.android.data.remote.SupabaseApiImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideSupabaseApi(): SupabaseApi = SupabaseApiImpl()
}
