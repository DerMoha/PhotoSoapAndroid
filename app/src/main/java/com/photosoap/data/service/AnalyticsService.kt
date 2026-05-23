package com.photosoap.data.service

import com.photosoap.data.local.datastore.SettingsDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class AppEvent(
    val name: String,
    val properties: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis(),
)

@Singleton
class AnalyticsService @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
) {
    private val events = mutableListOf<AppEvent>()

    private suspend fun isEnabled(): Boolean = settingsDataStore.analyticsEnabled.first()

    suspend fun track(event: AppEvent) {
        if (!isEnabled()) return
        events.add(event)
        // In production, flush events to analytics backend
        if (events.size > 20) flush()
    }

    suspend fun trackEvent(name: String, properties: Map<String, String> = emptyMap()) {
        track(AppEvent(name = name, properties = properties))
    }

    fun getRecentEvents(count: Int = 10): List<AppEvent> {
        return events.takeLast(count)
    }

    private fun flush() {
        // Send events to analytics endpoint
        events.clear()
    }

    fun clear() {
        events.clear()
    }
}
