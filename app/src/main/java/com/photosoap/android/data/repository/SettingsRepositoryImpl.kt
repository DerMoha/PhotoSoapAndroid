package com.photosoap.android.data.repository

import com.photosoap.android.data.local.datastore.SettingsDataStore
import com.photosoap.android.domain.repository.SettingsRepository
import com.photosoap.android.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: SettingsDataStore,
) : SettingsRepository {

    override val themeMode = dataStore.themeMode
    override val dynamicColor = dataStore.dynamicColor
    override suspend fun setThemeMode(mode: ThemeMode) = dataStore.setThemeMode(mode)
    override suspend fun setDynamicColor(enabled: Boolean) = dataStore.setDynamicColor(enabled)
    override val hasSeenOnboarding: Flow<Boolean> = dataStore.hasSeenOnboarding
    override val hapticsEnabled: Flow<Boolean> = dataStore.hapticsEnabled
    override val useDeleteQueue: Flow<Boolean> = dataStore.useDeleteQueue
    override val analyticsEnabled: Flow<Boolean> = dataStore.analyticsEnabled
    override val mediaKind: Flow<String> = dataStore.mediaKind
    override val sortOrder: Flow<String> = dataStore.sortOrder
    override val pendingDeletions: Flow<String> = dataStore.pendingDeletions
    override val pendingDeletionRequest: Flow<String> = dataStore.pendingDeletionRequest

    override suspend fun setOnboardingSeen() = dataStore.setOnboardingSeen()
    override suspend fun resetOnboarding() = dataStore.resetOnboarding()
    override suspend fun setHapticsEnabled(enabled: Boolean) = dataStore.setHapticsEnabled(enabled)
    override suspend fun setUseDeleteQueue(enabled: Boolean) = dataStore.setUseDeleteQueue(enabled)
    override suspend fun setAnalyticsEnabled(enabled: Boolean) = dataStore.setAnalyticsEnabled(enabled)
    override suspend fun setMediaKind(kind: String) = dataStore.setMediaKind(kind)
    override suspend fun setSortOrder(order: String) = dataStore.setSortOrder(order)
    override suspend fun setPendingDeletions(json: String) = dataStore.setPendingDeletions(json)
    override suspend fun setPendingDeletionRequest(json: String) = dataStore.setPendingDeletionRequest(json)
}
