package com.photosoap.android.domain.repository

import com.photosoap.android.domain.model.UserStats
import kotlinx.coroutines.flow.Flow

interface StatsRepository {
    fun observeStats(): Flow<UserStats?>
    suspend fun getStats(): UserStats?
    suspend fun updateStats(stats: UserStats)
    suspend fun createIfNeeded(): UserStats
}
