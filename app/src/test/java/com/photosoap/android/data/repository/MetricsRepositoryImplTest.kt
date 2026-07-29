package com.photosoap.android.data.repository

import com.photosoap.android.data.local.datastore.SettingsDataStore
import com.photosoap.android.data.remote.SupabaseApi
import com.photosoap.android.data.remote.PermanentMetricsException
import com.photosoap.android.data.remote.dto.DailyBucket
import com.photosoap.android.data.remote.dto.MetricsPayload
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MetricsRepositoryImplTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val dataStore = mockk<SettingsDataStore>(relaxed = true)
    private val api = mockk<SupabaseApi>()

    @Test
    fun `uploaded buckets remain cumulative for idempotent server updates`() = runTest {
        val today = LocalDate.now().toString()
        val existing = DailyBucket(
            metricDate = today,
            reviewedPhotos = 5,
            deletedPhotos = 1,
            keptPhotos = 4,
            bytesFreed = 1_000,
        )
        val captured = mutableListOf<MetricsPayload>()

        stubEnabled(
            pending = json.encodeToString(listOf(existing)),
            lastFlush = System.currentTimeMillis() - 25 * 60 * 60 * 1_000,
        )
        coEvery { api.ingestMetrics(capture(captured)) } returns Result.success(Unit)

        MetricsRepositoryImpl(dataStore, api).trackKept()

        val bucket = captured.single().dailyBuckets.single()
        assertEquals(6, bucket.reviewedPhotos)
        assertEquals(1, bucket.deletedPhotos)
        assertEquals(5, bucket.keptPhotos)
        coVerify(exactly = 0) { dataStore.setPendingMetrics("[]") }
    }

    @Test
    fun `confirmed deletion records one reviewed and one deleted item`() = runTest {
        stubEnabled(lastFlush = System.currentTimeMillis())

        MetricsRepositoryImpl(dataStore, api).trackDeletion(fileSize = 2_048)

        coVerify {
            dataStore.setPendingMetrics(match { encoded ->
                val bucket = json.decodeFromString<List<DailyBucket>>(encoded).single()
                bucket.reviewedPhotos == 1 &&
                    bucket.deletedPhotos == 1 &&
                    bucket.keptPhotos == 0 &&
                    bucket.bytesFreed == 2_048L
            })
        }
    }

    @Test
    fun `disabled analytics creates no identifier or metrics`() = runTest {
        every { dataStore.analyticsEnabled } returns flowOf(false)
        every { dataStore.installId } returns flowOf("")

        MetricsRepositoryImpl(dataStore, api).trackKept()

        coVerify(exactly = 0) { dataStore.setInstallId(any()) }
        coVerify(exactly = 0) { dataStore.setPendingMetrics(any()) }
        coVerify(exactly = 0) { api.ingestMetrics(any()) }
    }

    @Test
    fun `permanent server rejection disables the visible analytics preference`() = runTest {
        stubEnabled(lastFlush = System.currentTimeMillis() - 25 * 60 * 60 * 1_000)
        coEvery { api.ingestMetrics(any()) } returns Result.failure(
            PermanentMetricsException("HTTP 403"),
        )

        MetricsRepositoryImpl(dataStore, api).trackKept()

        coVerify { dataStore.setAnalyticsEnabled(false) }
    }

    private fun stubEnabled(
        pending: String = "[]",
        lastFlush: Long,
    ) {
        every { dataStore.analyticsEnabled } returns flowOf(true)
        every { dataStore.installId } returns MutableStateFlow("install-id")
        every { dataStore.pendingMetrics } returns flowOf(pending)
        every { dataStore.lastMetricsFlush } returns flowOf(lastFlush)
        every { dataStore.metricsDisabledPermanently } returns flowOf(false)
        coEvery { api.ingestMetrics(any()) } returns Result.success(Unit)
    }
}
