package com.photosoap.android.data.local.datastore

import com.photosoap.android.domain.model.AccentColor
import com.photosoap.android.domain.model.ThemeMode
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
        private val KEY_ACCENT_COLOR = stringPreferencesKey("accent_color")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        private val KEY_HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        private val KEY_USE_DELETE_QUEUE = booleanPreferencesKey("use_delete_queue")
        private val KEY_ANALYTICS_ENABLED = booleanPreferencesKey("analytics_enabled")
        private val KEY_MEDIA_KIND = stringPreferencesKey("media_kind")
        private val KEY_SORT_ORDER = stringPreferencesKey("sort_order")
        private val KEY_INSTALL_ID = stringPreferencesKey("install_id")
        private val KEY_LAST_METRICS_FLUSH = longPreferencesKey("last_metrics_flush")
        private val KEY_PENDING_METRICS = stringPreferencesKey("pending_metrics")
        private val KEY_PENDING_DELETIONS = stringPreferencesKey("pending_deletions")
        private val KEY_PENDING_DELETION_REQUEST = stringPreferencesKey("pending_deletion_request")
        private val KEY_PROCESSED_METRIC_DELETIONS = stringPreferencesKey("processed_metric_deletions")
        private val KEY_METRICS_DISABLED_PERMANENTLY = booleanPreferencesKey("metrics_disabled_permanently")
    }

    val hasSeenOnboarding: Flow<Boolean> = context.dataStore.data.map { it[KEY_HAS_SEEN_ONBOARDING] ?: false }
    val accentColor: Flow<AccentColor> = context.dataStore.data.map { preferences ->
        AccentColor.entries.firstOrNull { it.name == preferences[KEY_ACCENT_COLOR] } ?: AccentColor.TEAL
    }
    suspend fun setAccentColor(accent: AccentColor) {
        context.dataStore.edit {
            it[KEY_ACCENT_COLOR] = accent.name
            it[KEY_DYNAMIC_COLOR] = false
        }
    }
    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        ThemeMode.entries.firstOrNull { it.name == preferences[KEY_THEME_MODE] } ?: ThemeMode.SYSTEM
    }
    val dynamicColor: Flow<Boolean> = context.dataStore.data.map { it[KEY_DYNAMIC_COLOR] ?: true }
    suspend fun setThemeMode(mode: ThemeMode) { context.dataStore.edit { it[KEY_THEME_MODE] = mode.name } }
    suspend fun setDynamicColor(enabled: Boolean) { context.dataStore.edit { it[KEY_DYNAMIC_COLOR] = enabled } }

    val hapticsEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_HAPTICS_ENABLED] ?: true }
    val useDeleteQueue: Flow<Boolean> = context.dataStore.data.map { it[KEY_USE_DELETE_QUEUE] ?: true }
    val analyticsEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_ANALYTICS_ENABLED] ?: false }
    val mediaKind: Flow<String> = context.dataStore.data.map { it[KEY_MEDIA_KIND] ?: "ALL" }
    val sortOrder: Flow<String> = context.dataStore.data.map { it[KEY_SORT_ORDER] ?: "NEWEST_FIRST" }
    val installId: Flow<String> = context.dataStore.data.map { it[KEY_INSTALL_ID] ?: "" }
    val lastMetricsFlush: Flow<Long> = context.dataStore.data.map { it[KEY_LAST_METRICS_FLUSH] ?: 0L }
    val pendingMetrics: Flow<String> = context.dataStore.data.map { it[KEY_PENDING_METRICS] ?: "" }
    val pendingDeletions: Flow<String> = context.dataStore.data.map { it[KEY_PENDING_DELETIONS] ?: "[]" }
    val pendingDeletionRequest: Flow<String> = context.dataStore.data.map { it[KEY_PENDING_DELETION_REQUEST] ?: "" }
    val processedMetricDeletions: Flow<String> = context.dataStore.data.map { it[KEY_PROCESSED_METRIC_DELETIONS] ?: "[]" }
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
                it.remove(KEY_PROCESSED_METRIC_DELETIONS)
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

    suspend fun setPendingDeletionRequest(json: String) {
        context.dataStore.edit {
            if (json.isBlank()) it.remove(KEY_PENDING_DELETION_REQUEST)
            else it[KEY_PENDING_DELETION_REQUEST] = json
        }
    }

    suspend fun setPendingMetricsAndProcessedDeletions(metrics: String, requestIds: String) {
        context.dataStore.edit {
            it[KEY_PENDING_METRICS] = metrics
            it[KEY_PROCESSED_METRIC_DELETIONS] = requestIds
        }
    }

    suspend fun setMetricsDisabledPermanently(disabled: Boolean) {
        context.dataStore.edit { it[KEY_METRICS_DISABLED_PERMANENTLY] = disabled }
    }
}
