package com.photosoap.android.domain.repository

import com.photosoap.android.domain.model.AccentColor
import com.photosoap.android.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val accentColor: Flow<AccentColor>
    suspend fun setAccentColor(accent: AccentColor)
    val themeMode: Flow<ThemeMode>
    val dynamicColor: Flow<Boolean>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setDynamicColor(enabled: Boolean)
    val hasSeenOnboarding: Flow<Boolean>
    val hapticsEnabled: Flow<Boolean>
    val useDeleteQueue: Flow<Boolean>
    val analyticsEnabled: Flow<Boolean>
    val previewHintSeen: Flow<Boolean>
    suspend fun setPreviewHintSeen()
    val hideFavorites: Flow<Boolean>
    suspend fun setHideFavorites(enabled: Boolean)
    val mediaKind: Flow<String>
    val sortOrder: Flow<String>
    val pendingDeletions: Flow<String>
    val pendingDeletionRequest: Flow<String>

    suspend fun setOnboardingSeen()
    suspend fun resetOnboarding()
    suspend fun setHapticsEnabled(enabled: Boolean)
    suspend fun setUseDeleteQueue(enabled: Boolean)
    suspend fun setAnalyticsEnabled(enabled: Boolean)
    suspend fun setMediaKind(kind: String)
    suspend fun setSortOrder(order: String)
    suspend fun setPendingDeletions(json: String)
    suspend fun setPendingDeletionRequest(json: String)
}
