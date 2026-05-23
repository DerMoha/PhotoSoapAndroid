package com.photosoap.android.data.repository

import com.photosoap.android.data.local.datastore.SettingsDataStore
import com.photosoap.android.data.remote.SupabaseApi
import com.photosoap.android.data.remote.PermanentMetricsException
import com.photosoap.android.data.remote.dto.DailyBucket
import com.photosoap.android.data.remote.dto.MetricsPayload
import com.photosoap.android.domain.repository.MetricsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.JsonObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

class MetricsRepositoryImpl @Inject constructor(
    private val dataStore: SettingsDataStore,
    private val api: SupabaseApi,
) : MetricsRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override val installId: Flow<String> = dataStore.installId
    override val isEnabled: Flow<Boolean> = dataStore.analyticsEnabled

    private val pendingBuckets = mutableListOf<DailyBucket>()
    private var retryCount = 0
    private var disabledForSession = false

    override suspend fun trackReview() {
        if (!isEnabled.first() || disabledForSession) return
        ensureInstallId()
        addToDailyBucket(reviewDelta = 1)
        checkFlushThreshold()
    }

    override suspend fun trackDeletion(fileSize: Long) {
        if (!isEnabled.first() || disabledForSession) return
        ensureInstallId()
        addToDailyBucket(deleteDelta = 1, bytesDelta = fileSize)
        checkFlushThreshold()
    }

    override suspend fun trackBatchDeletion(count: Int, totalFileSize: Long) {
        if (!isEnabled.first() || disabledForSession) return
        ensureInstallId()
        addToDailyBucket(deleteDelta = count, bytesDelta = totalFileSize)
        checkFlushThreshold()
    }

    override suspend fun flush() {
        if (!isEnabled.first() || disabledForSession) return
        if (dataStore.metricsDisabledPermanently.first()) return

        val id = installId.first()
        if (id.isBlank()) return

        val buckets = loadPendingBuckets()
        if (buckets.isEmpty()) return

        val payload = MetricsPayload(
            installId = id,
            submittedAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
            dailyBuckets = buckets,
        )

        val result = api.ingestMetrics(payload)
        result.fold(
            onSuccess = {
                dataStore.setPendingMetrics("[]")
                pendingBuckets.clear()
                retryCount = 0
                dataStore.setLastMetricsFlush(System.currentTimeMillis())
            },
            onFailure = { error ->
                if (error is PermanentMetricsException) {
                    dataStore.setMetricsDisabledPermanently(true)
                    dataStore.setPendingMetrics("[]")
                    pendingBuckets.clear()
                } else {
                    retryCount++
                    savePendingBuckets(buckets)
                    scheduleRetry()
                }
            },
        )
    }

    private suspend fun ensureInstallId() {
        val id = installId.first()
        if (id.isBlank()) {
            dataStore.setInstallId(UUID.randomUUID().toString())
        }
    }

    private fun addToDailyBucket(reviewDelta: Int = 0, deleteDelta: Int = 0, bytesDelta: Long = 0) {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val existing = pendingBuckets.find { it.metricDate == today }

        if (existing != null) {
            val index = pendingBuckets.indexOf(existing)
            pendingBuckets[index] = existing.copy(
                reviewedPhotos = existing.reviewedPhotos + reviewDelta,
                deletedPhotos = existing.deletedPhotos + deleteDelta,
                keptPhotos = existing.keptPhotos + maxOf(0, reviewDelta - deleteDelta),
                bytesFreed = existing.bytesFreed + bytesDelta,
            )
        } else {
            pendingBuckets.add(
                DailyBucket(
                    metricDate = today,
                    reviewedPhotos = reviewDelta,
                    deletedPhotos = deleteDelta,
                    keptPhotos = maxOf(0, reviewDelta - deleteDelta),
                    bytesFreed = bytesDelta,
                )
            )
        }
    }

    private suspend fun checkFlushThreshold() {
        val now = System.currentTimeMillis()
        val lastFlush = dataStore.lastMetricsFlush.first()
        val hoursSinceLastFlush = (now - lastFlush) / (1000 * 60 * 60)

        val shouldFlush = hoursSinceLastFlush >= 24 ||
                pendingBuckets.size >= 3 ||
                pendingBuckets.sumOf { it.reviewedPhotos } >= 200 ||
                pendingBuckets.sumOf { it.bytesFreed } >= 500_000_000L

        if (shouldFlush) {
            savePendingBuckets(pendingBuckets)
            flush()
        }
    }

    private suspend fun scheduleRetry() {
        val delayMs = when (retryCount) {
            1 -> 15 * 60 * 1000L
            2 -> 60 * 60 * 1000L
            3 -> 6 * 60 * 60 * 1000L
            else -> return
        }
        delay(delayMs)
        flush()
    }

    private suspend fun loadPendingBuckets(): List<DailyBucket> {
        if (pendingBuckets.isNotEmpty()) return pendingBuckets
        val jsonStr = dataStore.pendingMetrics.first()
        if (jsonStr.isBlank() || jsonStr == "[]") return emptyList()
        return try {
            json.decodeFromString<List<DailyBucket>>(jsonStr)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun savePendingBuckets(buckets: List<DailyBucket>) {
        val jsonStr = json.encodeToString(buckets)
        dataStore.setPendingMetrics(jsonStr)
    }
}
