package com.photosoap.android.domain.repository

import kotlinx.coroutines.flow.Flow

interface AchievementRepository {
    fun observeUnlockDates(): Flow<Map<String, Long>> = kotlinx.coroutines.flow.flowOf(emptyMap())
    fun observeUnlockedIds(): Flow<List<String>>
    suspend fun getUnlockedIds(): List<String>
    suspend fun unlock(id: String)
    suspend fun isUnlocked(id: String): Boolean
}
