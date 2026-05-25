package com.photosoap.android.ui.stats

import com.photosoap.android.domain.model.UserStats
import com.photosoap.android.domain.repository.StatsRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {

    private val statsRepository = mockk<StatsRepository>()
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { statsRepository.observeStats() } returns flowOf(null)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has null stats`() = runTest(testDispatcher) {
        val vm = StatsViewModel(statsRepository)
        assertNull(vm.uiState.value.stats)
    }

    @Test
    fun `stats from repository populate state`() = runTest(testDispatcher) {
        val stats = UserStats(
            totalReviewed = 100,
            totalDeleted = 50,
            totalKept = 50,
            storageFreed = 1_000_000_000L,
        )
        every { statsRepository.observeStats() } returns flowOf(stats)

        val vm = StatsViewModel(statsRepository)
        val s = requireNotNull(vm.uiState.value.stats)
        assertEquals(100, s.totalReviewed)
        assertEquals(50, s.totalDeleted)
        assertEquals(50, s.totalKept)
    }

    @Test
    fun `stats reflect review progress`() = runTest(testDispatcher) {
        val stats = UserStats(
            totalReviewed = 1500,
            bestStreak = 50,
            dayStreak = 7,
            todayReviewCount = 15,
            sessionReviewCount = 200,
        )
        every { statsRepository.observeStats() } returns flowOf(stats)

        val vm = StatsViewModel(statsRepository)
        val s = requireNotNull(vm.uiState.value.stats)
        assertEquals(1500, s.totalReviewed)
        assertEquals(50, s.bestStreak)
        assertEquals(7, s.dayStreak)
        assertEquals(15, s.todayReviewCount)
        assertEquals(200, s.sessionReviewCount)
    }

    @Test
    fun `null stats means no data yet`() = runTest(testDispatcher) {
        every { statsRepository.observeStats() } returns flowOf(null)
        val vm = StatsViewModel(statsRepository)

        assertNull(vm.uiState.value.stats)
    }

    @Test
    fun `storage freed reflects bytes`() = runTest(testDispatcher) {
        val stats = UserStats(storageFreed = 12_345_678_901L)
        every { statsRepository.observeStats() } returns flowOf(stats)

        val vm = StatsViewModel(statsRepository)
        val s = requireNotNull(vm.uiState.value.stats)
        assertEquals(12_345_678_901L, s.storageFreed)
    }
}
