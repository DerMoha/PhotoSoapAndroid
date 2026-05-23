package com.photosoap.domain.repository

interface SettingsRepository {
    suspend fun isHapticsEnabled(): Boolean
    suspend fun setHapticsEnabled(enabled: Boolean)
    suspend fun isUseDeleteList(): Boolean
    suspend fun setUseDeleteList(enabled: Boolean)
    suspend fun isAnalyticsEnabled(): Boolean
    suspend fun setAnalyticsEnabled(enabled: Boolean)
    suspend fun hasSeenOnboarding(): Boolean
    suspend fun setHasSeenOnboarding(seen: Boolean)
    suspend fun getInstallId(): String
    suspend fun resetOnboarding()
}
