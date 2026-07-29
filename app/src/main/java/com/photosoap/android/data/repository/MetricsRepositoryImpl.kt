package com.photosoap.android.data.repository

import com.photosoap.android.data.local.datastore.SettingsDataStore
import com.photosoap.android.data.remote.SupabaseApi
import com.photosoap.android.data.remote.PermanentMetricsException
import com.photosoap.android.data.remote.dto.DailyBucket
import com.photosoap.android.data.remote.dto.MetricsPayload
import com.photosoap.android.domain.repository.MetricsRepository
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
    private var hasLoadedPendingBuckets = false

    override suspend fun trackKept(count: Int) {
        if (count <= 0 || !prepareForCollection()) return
        val shouldFlush = mutex.withLock {
            loadPendingBucketsLocked()
            ensureInstallIdLocked()
            addToDailyBucket(reviewDelta = count)
            savePendingBuckets()
            checkFlushThreshold()
        }
        if (shouldFlush) flush()
    }

    override suspend fun trackDeletion(fileSize: Long) {
        if (!prepareForCollection()) return
        val shouldFlush = mutex.withLock {
            loadPendingBucketsLocked()
            ensureInstallIdLocked()
            addToDailyBucket(reviewDelta = 1, deleteDelta = 1, bytesDelta = fileSize)
            savePendingBuckets()
            checkFlushThreshold()
        }
        if (shouldFlush) flush()
    }

    override suspend fun trackBatchDeletion(count: Int, totalFileSize: Long) {
        if (count <= 0 || !prepareForCollection()) return
        val shouldFlush = mutex.withLock {
            loadPendingBucketsLocked()
            ensureInstallIdLocked()
            addToDailyBucket(
                reviewDelta = count,
                deleteDelta = count,
                bytesDelta = totalFileSize,
            )
            savePendingBuckets()
            checkFlushThreshold()
        }
        if (shouldFlush) flush()
    }

    override suspend fun flush() = mutex.withLock {
        if (!isEnabled.first()) return@withLock
        if (dataStore.metricsDisabledPermanently.first()) return@withLock

        loadPendingBucketsLocked()
        ensureInstallIdLocked()

        val id = ensureInstallIdLocked()
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
                savePendingBuckets()
                dataStore.setLastMetricsFlush(System.currentTimeMillis())
            },
            onFailure = { error ->
                if (error is PermanentMetricsException) {
                    pendingBuckets.clear()
                    // Reflect a non-retryable backend rejection in the user-facing
                    // preference instead of leaving an enabled toggle that uploads nothing.
                    dataStore.setAnalyticsEnabled(false)
                } else {
                    savePendingBuckets()
                }
            },
        )
    }

    private suspend fun prepareForCollection(): Boolean {
        if (isEnabled.first()) {
            if (dataStore.installId.first().isBlank()) {
                mutex.withLock {
                    pendingBuckets.clear()
                    hasLoadedPendingBuckets = false
                }
            }
            return true
        }
        mutex.withLock {
            pendingBuckets.clear()
            hasLoadedPendingBuckets = false
        }
        return false
    }

    private suspend fun loadPendingBucketsLocked() {
        if (hasLoadedPendingBuckets) return
        val stored = dataStore.pendingMetrics.first()
        if (stored.isNotBlank()) {
            runCatching {
                json.decodeFromString<List<DailyBucket>>(stored)
            }.getOrNull()?.let(pendingBuckets::addAll)
        }
        val oldestAllowed = LocalDate.now().minusDays(13)
        pendingBuckets.removeAll { bucket ->
            runCatching { LocalDate.parse(bucket.metricDate) }
                .getOrNull()
                ?.isBefore(oldestAllowed) != false
        }
        hasLoadedPendingBuckets = true
    }

    private suspend fun ensureInstallIdLocked(): String {
        val id = installId.first()
        if (id.isBlank()) {
            val newId = UUID.randomUUID().toString()
            dataStore.setInstallId(newId)
            dataStore.setLastMetricsFlush(System.currentTimeMillis())
            return newId
        }
        return id
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
        val hoursSinceLastFlush = if (lastFlush == 0L) 0
            else (now - lastFlush) / (1000 * 60 * 60)

        return hoursSinceLastFlush >= 24
    }

    private suspend fun savePendingBuckets() {
        val jsonStr = json.encodeToString(pendingBuckets.toList())
        dataStore.setPendingMetrics(jsonStr)
    }
}
