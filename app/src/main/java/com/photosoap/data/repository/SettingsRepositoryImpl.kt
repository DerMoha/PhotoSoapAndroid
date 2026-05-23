package com.photosoap.data.repository

import com.photosoap.data.local.datastore.SettingsDataStore
import com.photosoap.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: SettingsDataStore,
) : SettingsRepository {

    override suspend fun isHapticsEnabled(): Boolean =
        dataStore.hapticsEnabled.first()

    override suspend fun setHapticsEnabled(enabled: Boolean) =
        dataStore.setHapticsEnabled(enabled)

    override suspend fun isUseDeleteList(): Boolean =
        dataStore.useDeleteList.first()

    override suspend fun setUseDeleteList(enabled: Boolean) =
        dataStore.setUseDeleteList(enabled)

    override suspend fun isAnalyticsEnabled(): Boolean =
        dataStore.analyticsEnabled.first()

    override suspend fun setAnalyticsEnabled(enabled: Boolean) =
        dataStore.setAnalyticsEnabled(enabled)

    override suspend fun hasSeenOnboarding(): Boolean =
        dataStore.hasSeenOnboarding.first()

    override suspend fun setHasSeenOnboarding(seen: Boolean) =
        dataStore.setHasSeenOnboarding(seen)

    override suspend fun getInstallId(): String =
        dataStore.installId.first()

    override suspend fun resetOnboarding() {
        dataStore.setHasSeenOnboarding(false)
    }
}
