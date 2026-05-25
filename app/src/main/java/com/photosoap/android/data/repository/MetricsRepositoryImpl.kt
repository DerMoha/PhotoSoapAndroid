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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

class MetricsRepositoryImpl @Inject constructor(
    private val dataStore: SettingsDataStore,
    private val api: SupabaseApi,
) : MetricsRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()

    override val installId: Flow<String> = dataStore.installId
    override val isEnabled: Flow<Boolean> = dataStore.analyticsEnabled

    private val pendingBuckets = mutableListOf<DailyBucket>()
    private var retryCount = 0

    private var cachedInstallId: String? = null
    private var cachedIsEnabled: Boolean? = null

    override suspend fun trackReview() {
        if (!isCachedEnabled()) return
        val shouldFlush = mutex.withLock {
            ensureInstallIdLocked()
            addToDailyBucket(reviewDelta = 1)
            checkFlushThreshold()
        }
        if (shouldFlush) flush()
    }

    override suspend fun trackDeletion(fileSize: Long) {
        if (!isCachedEnabled()) return
        val shouldFlush = mutex.withLock {
            ensureInstallIdLocked()
            addToDailyBucket(deleteDelta = 1, bytesDelta = fileSize)
            checkFlushThreshold()
        }
        if (shouldFlush) flush()
    }

    override suspend fun trackBatchDeletion(count: Int, totalFileSize: Long) {
        if (!isCachedEnabled()) return
        val shouldFlush = mutex.withLock {
            ensureInstallIdLocked()
            addToDailyBucket(deleteDelta = count, bytesDelta = totalFileSize)
            checkFlushThreshold()
        }
        if (shouldFlush) flush()
    }

    override suspend fun flush() = mutex.withLock {
        if (!isCachedEnabled()) return@withLock
        if (dataStore.metricsDisabledPermanently.first()) return@withLock

        val id = cachedInstallId ?: return@withLock
        if (id.isBlank()) return@withLock

        if (pendingBuckets.isEmpty()) return@withLock

        val payload = MetricsPayload(
            installId = id,
            submittedAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
            dailyBuckets = pendingBuckets.toList(),
        )

        val result = api.ingestMetrics(payload)
        result.fold(
            onSuccess = {
                pendingBuckets.clear()
                dataStore.setPendingMetrics("[]")
                retryCount = 0
                dataStore.setLastMetricsFlush(System.currentTimeMillis())
            },
            onFailure = { error ->
                if (error is PermanentMetricsException) {
                    pendingBuckets.clear()
                    dataStore.setPendingMetrics("[]")
                    dataStore.setMetricsDisabledPermanently(true)
                    retryCount = 0
                } else if (retryCount < 3) {
                    retryCount++
                    savePendingBuckets()
                    scheduleRetry()
                } else {
                    savePendingBuckets()
                    retryCount = 0
                }
            },
        )
    }

    private suspend fun isCachedEnabled(): Boolean {
        val cached = cachedIsEnabled
        if (cached != null) return cached
        cachedIsEnabled = isEnabled.first()
        return cachedIsEnabled ?: false
    }

    private suspend fun ensureInstallIdLocked() {
        val id = cachedInstallId ?: run {
            val fresh = installId.first()
            cachedInstallId = fresh
            fresh
        }
        if (id.isBlank()) {
            val newId = UUID.randomUUID().toString()
            dataStore.setInstallId(newId)
            cachedInstallId = newId
        }
    }

    private fun addToDailyBucket(reviewDelta: Int = 0, deleteDelta: Int = 0, bytesDelta: Long = 0) {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val existing = pendingBuckets.find { it.metricDate == today }

        if (existing != null) {
            val index = pendingBuckets.indexOf(existing)
            val newKept = existing.keptPhotos + reviewDelta - deleteDelta
            pendingBuckets[index] = existing.copy(
                reviewedPhotos = existing.reviewedPhotos + reviewDelta,
                deletedPhotos = existing.deletedPhotos + deleteDelta,
                keptPhotos = maxOf(0, newKept),
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

    private suspend fun checkFlushThreshold(): Boolean {
        val now = System.currentTimeMillis()
        val lastFlush = dataStore.lastMetricsFlush.first()
        val hoursSinceLastFlush = if (lastFlush == 0L) 0 else (now - lastFlush) / (1000 * 60 * 60)

        val shouldFlush = hoursSinceLastFlush >= 24 ||
                pendingBuckets.size >= 3 ||
                pendingBuckets.sumOf { it.reviewedPhotos } >= 200 ||
                pendingBuckets.sumOf { it.bytesFreed } >= 500_000_000L

        if (shouldFlush) savePendingBuckets()
        return shouldFlush
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

    private suspend fun savePendingBuckets() {
        val jsonStr = json.encodeToString(pendingBuckets.toList())
        dataStore.setPendingMetrics(jsonStr)
    }
}
