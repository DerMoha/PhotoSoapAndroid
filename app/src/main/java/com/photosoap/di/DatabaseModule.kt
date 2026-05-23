package com.photosoap.di

import android.content.Context
import androidx.room.Room
import com.photosoap.data.local.db.PhotoSoapDatabase
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
        ).build()
    }
}
