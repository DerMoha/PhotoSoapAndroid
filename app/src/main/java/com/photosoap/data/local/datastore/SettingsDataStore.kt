package com.photosoap.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "photosoap_settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val USE_DELETE_LIST = booleanPreferencesKey("use_delete_list")
        val ANALYTICS_ENABLED = booleanPreferencesKey("analytics_enabled")
        val HAS_SEEN_ONBOARDING = booleanPreferencesKey("has_seen_onboarding")
        val INSTALL_ID = stringPreferencesKey("install_id")
        val LAST_METRICS_FLUSH_TIME = stringPreferencesKey("last_metrics_flush_time")
    }

    val hapticsEnabled: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.HAPTICS_ENABLED] ?: true
    }

    val useDeleteList: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.USE_DELETE_LIST] ?: true
    }

    val analyticsEnabled: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.ANALYTICS_ENABLED] ?: false
    }

    val hasSeenOnboarding: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.HAS_SEEN_ONBOARDING] ?: false
    }

    val installId: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.INSTALL_ID] ?: java.util.UUID.randomUUID().toString()
    }

    val lastMetricsFlushTime: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.LAST_METRICS_FLUSH_TIME] ?: ""
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.HAPTICS_ENABLED] = enabled
        }
    }

    suspend fun setUseDeleteList(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.USE_DELETE_LIST] = enabled
        }
    }

    suspend fun setAnalyticsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.ANALYTICS_ENABLED] = enabled
        }
    }

    suspend fun setHasSeenOnboarding(seen: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.HAS_SEEN_ONBOARDING] = seen
        }
    }

    suspend fun setInstallId(id: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.INSTALL_ID] = id
        }
    }

    suspend fun setLastMetricsFlushTime(time: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.LAST_METRICS_FLUSH_TIME] = time
        }
    }
}
