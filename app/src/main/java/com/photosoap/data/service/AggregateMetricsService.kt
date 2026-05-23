package com.photosoap.data.service

import com.photosoap.data.local.datastore.SettingsDataStore
import com.photosoap.data.remote.dto.DailyMetricsBucket
import com.photosoap.data.remote.dto.MetricsPayload
import com.photosoap.domain.repository.MetricsRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

data class PersistedDailyMetricsBucket(
    val date: String,
    val reviewedPhotos: Int = 0,
    val deletedPhotos: Int = 0,
    val keptPhotos: Int = 0,
    val bytesFreed: Long = 0L,
)

@Singleton
class AggregateMetricsService @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val metricsRepository: MetricsRepository,
    private val json: Json,
) {
    private var pendingBuckets = mutableListOf<PersistedDailyMetricsBucket>()

    suspend fun recordReview() {
        if (!isAnalyticsEnabled()) return
        val today = getTodayDateString()
        val bucket = pendingBuckets.find { it.date == today }
        if (bucket != null) {
            val idx = pendingBuckets.indexOf(bucket)
            pendingBuckets[idx] = bucket.copy(reviewedPhotos = bucket.reviewedPhotos + 1)
        } else {
            pendingBuckets.add(PersistedDailyMetricsBucket(date = today, reviewedPhotos = 1))
        }
        pendingBuckets = pendingBuckets.sanitize()
        maybeFlush()
    }

    suspend fun recordDeletion(fileSize: Long) {
        if (!isAnalyticsEnabled()) return
        val today = getTodayDateString()
        val bucket = pendingBuckets.find { it.date == today }
        if (bucket != null) {
            val idx = pendingBuckets.indexOf(bucket)
            pendingBuckets[idx] = bucket.copy(
                deletedPhotos = bucket.deletedPhotos + 1,
                bytesFreed = bucket.bytesFreed + fileSize,
            )
        } else {
            pendingBuckets.add(
                PersistedDailyMetricsBucket(
                    date = today,
                    deletedPhotos = 1,
                    bytesFreed = fileSize,
                )
            )
        }
        pendingBuckets = pendingBuckets.sanitize()
        maybeFlush()
    }

    suspend fun recordKeep() {
        if (!isAnalyticsEnabled()) return
        val today = getTodayDateString()
        val bucket = pendingBuckets.find { it.date == today }
        if (bucket != null) {
            val idx = pendingBuckets.indexOf(bucket)
            pendingBuckets[idx] = bucket.copy(keptPhotos = bucket.keptPhotos + 1)
        } else {
            pendingBuckets.add(PersistedDailyMetricsBucket(date = today, keptPhotos = 1))
        }
        pendingBuckets = pendingBuckets.sanitize()
        maybeFlush()
    }

    suspend fun recordBatchDeletion(count: Int, totalFileSize: Long) {
        if (!isAnalyticsEnabled()) return
        val today = getTodayDateString()
        val bucket = pendingBuckets.find { it.date == today }
        if (bucket != null) {
            val idx = pendingBuckets.indexOf(bucket)
            pendingBuckets[idx] = bucket.copy(
                deletedPhotos = bucket.deletedPhotos + count,
                bytesFreed = bucket.bytesFreed + totalFileSize,
            )
        } else {
            pendingBuckets.add(
                PersistedDailyMetricsBucket(
                    date = today,
                    deletedPhotos = count,
                    bytesFreed = totalFileSize,
                )
            )
        }
        pendingBuckets = pendingBuckets.sanitize()
        maybeFlush()
    }

    suspend fun flushNow(): Boolean {
        if (pendingBuckets.isEmpty()) return true
        return try {
            val installId = settingsDataStore.installId.first()
            val payload = MetricsPayload(
                installId = installId,
                appVersion = "1.0.0",
                buildNumber = "1",
                submittedAt = java.time.Instant.now().toString(),
                registerInstall = true,
                dailyBuckets = pendingBuckets.map { it.toDto() },
            )
            val success = metricsRepository.submitMetrics(payload)
            if (success) {
                pendingBuckets.clear()
                settingsDataStore.setLastMetricsFlushTime(System.currentTimeMillis().toString())
            }
            success
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun maybeFlush() {
        if (pendingBuckets.size >= 3) {
            flushNow()
        }
    }

    private suspend fun isAnalyticsEnabled(): Boolean =
        settingsDataStore.analyticsEnabled.first()

    private fun getTodayDateString(): String {
        val now = java.time.LocalDate.now()
        return now.toString()
    }

    private fun PersistedDailyMetricsBucket.toDto() = DailyMetricsBucket(
        date = date,
        reviewedPhotos = reviewedPhotos,
        deletedPhotos = deletedPhotos,
        keptPhotos = keptPhotos,
        bytesFreed = bytesFreed,
    )

    private fun MutableList<PersistedDailyMetricsBucket>.sanitize(): MutableList<PersistedDailyMetricsBucket> {
        // Ensure reviewed >= deleted + kept for each bucket
        return this.map { bucket ->
            val minReviewed = bucket.deletedPhotos + bucket.keptPhotos
            if (bucket.reviewedPhotos < minReviewed) {
                bucket.copy(reviewedPhotos = minReviewed)
            } else bucket
        }.toMutableList()
    }
}
