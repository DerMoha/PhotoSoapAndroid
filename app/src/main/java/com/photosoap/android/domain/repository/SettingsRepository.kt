package com.photosoap.android.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val hasSeenOnboarding: Flow<Boolean>
    val hapticsEnabled: Flow<Boolean>
    val useDeleteQueue: Flow<Boolean>
    val analyticsEnabled: Flow<Boolean>
    val mediaKind: Flow<String>
    val sortOrder: Flow<String>

    suspend fun setOnboardingSeen()
    suspend fun setHapticsEnabled(enabled: Boolean)
    suspend fun setUseDeleteQueue(enabled: Boolean)
    suspend fun setAnalyticsEnabled(enabled: Boolean)
    suspend fun setMediaKind(kind: String)
    suspend fun setSortOrder(order: String)
}
