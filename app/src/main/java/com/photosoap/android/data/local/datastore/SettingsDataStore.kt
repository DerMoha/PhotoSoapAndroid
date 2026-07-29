package com.photosoap.android.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "photosoap_settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        private val KEY_HAS_SEEN_ONBOARDING = booleanPreferencesKey("has_seen_onboarding")
        private val KEY_HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        private val KEY_USE_DELETE_QUEUE = booleanPreferencesKey("use_delete_queue")
        private val KEY_ANALYTICS_ENABLED = booleanPreferencesKey("analytics_enabled")
        private val KEY_MEDIA_KIND = stringPreferencesKey("media_kind")
        private val KEY_SORT_ORDER = stringPreferencesKey("sort_order")
        private val KEY_INSTALL_ID = stringPreferencesKey("install_id")
        private val KEY_LAST_METRICS_FLUSH = longPreferencesKey("last_metrics_flush")
        private val KEY_PENDING_METRICS = stringPreferencesKey("pending_metrics")
        private val KEY_PENDING_DELETIONS = stringPreferencesKey("pending_deletions")
        private val KEY_METRICS_DISABLED_PERMANENTLY = booleanPreferencesKey("metrics_disabled_permanently")
    }

    val hasSeenOnboarding: Flow<Boolean> = context.dataStore.data.map { it[KEY_HAS_SEEN_ONBOARDING] ?: false }
    val hapticsEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_HAPTICS_ENABLED] ?: true }
    val useDeleteQueue: Flow<Boolean> = context.dataStore.data.map { it[KEY_USE_DELETE_QUEUE] ?: true }
    val analyticsEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_ANALYTICS_ENABLED] ?: false }
    val mediaKind: Flow<String> = context.dataStore.data.map { it[KEY_MEDIA_KIND] ?: "ALL" }
    val sortOrder: Flow<String> = context.dataStore.data.map { it[KEY_SORT_ORDER] ?: "NEWEST_FIRST" }
    val installId: Flow<String> = context.dataStore.data.map { it[KEY_INSTALL_ID] ?: "" }
    val lastMetricsFlush: Flow<Long> = context.dataStore.data.map { it[KEY_LAST_METRICS_FLUSH] ?: 0L }
    val pendingMetrics: Flow<String> = context.dataStore.data.map { it[KEY_PENDING_METRICS] ?: "" }
    val pendingDeletions: Flow<String> = context.dataStore.data.map { it[KEY_PENDING_DELETIONS] ?: "[]" }
    val metricsDisabledPermanently: Flow<Boolean> = context.dataStore.data.map { it[KEY_METRICS_DISABLED_PERMANENTLY] ?: false }

    suspend fun setOnboardingSeen() {
        context.dataStore.edit { it[KEY_HAS_SEEN_ONBOARDING] = true }
    }

    suspend fun resetOnboarding() {
        context.dataStore.edit { it[KEY_HAS_SEEN_ONBOARDING] = false }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_HAPTICS_ENABLED] = enabled }
    }

    suspend fun setUseDeleteQueue(enabled: Boolean) {
        context.dataStore.edit { it[KEY_USE_DELETE_QUEUE] = enabled }
    }

    suspend fun setAnalyticsEnabled(enabled: Boolean) {
        context.dataStore.edit {
            it[KEY_ANALYTICS_ENABLED] = enabled
            if (!enabled) {
                it.remove(KEY_INSTALL_ID)
                it.remove(KEY_PENDING_METRICS)
                it.remove(KEY_LAST_METRICS_FLUSH)
                it.remove(KEY_METRICS_DISABLED_PERMANENTLY)
            }
        }
    }

    suspend fun setMediaKind(kind: String) {
        context.dataStore.edit { it[KEY_MEDIA_KIND] = kind }
    }

    suspend fun setSortOrder(order: String) {
        context.dataStore.edit { it[KEY_SORT_ORDER] = order }
    }

    suspend fun setInstallId(id: String) {
        context.dataStore.edit { it[KEY_INSTALL_ID] = id }
    }

    suspend fun setLastMetricsFlush(time: Long) {
        context.dataStore.edit { it[KEY_LAST_METRICS_FLUSH] = time }
    }

    suspend fun setPendingMetrics(json: String) {
        context.dataStore.edit { it[KEY_PENDING_METRICS] = json }
    }

    suspend fun setPendingDeletions(json: String) {
        context.dataStore.edit { it[KEY_PENDING_DELETIONS] = json }
    }

    suspend fun setMetricsDisabledPermanently(disabled: Boolean) {
        context.dataStore.edit { it[KEY_METRICS_DISABLED_PERMANENTLY] = disabled }
    }
}
