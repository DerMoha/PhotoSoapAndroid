package com.photosoap.android.ui.achievements

import com.photosoap.android.domain.model.Achievement
import com.photosoap.android.domain.model.UserStats
import com.photosoap.android.domain.repository.AchievementRepository
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AchievementsViewModelTest {

    private val achievementRepository = mockk<AchievementRepository>()
    private val statsRepository = mockk<StatsRepository>()

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { achievementRepository.observeUnlockedIds() } returns flowOf(emptyList())
        every { statsRepository.observeStats() } returns flowOf(null)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has zero unlocked count`() = runTest(testDispatcher) {
        val vm = AchievementsViewModel(achievementRepository, statsRepository)
        assertEquals(0, vm.uiState.value.unlockedCount)
    }

    @Test
    fun `total count matches all achievements`() = runTest(testDispatcher) {
        val vm = AchievementsViewModel(achievementRepository, statsRepository)
        assertEquals(Achievement.ALL.size, vm.uiState.value.totalCount)
    }

    @Test
    fun `unlocked ids from repository update state`() = runTest(testDispatcher) {
        every { achievementRepository.observeUnlockedIds() } returns flowOf(listOf("first_steps", "spring_cleaning"))
        val vm = AchievementsViewModel(achievementRepository, statsRepository)
        assertEquals(2, vm.uiState.value.unlockedCount)
        assertTrue(vm.uiState.value.unlockedIds.contains("first_steps"))
        assertTrue(vm.uiState.value.unlockedIds.contains("spring_cleaning"))
    }

    @Test
    fun `stats from repository populate state`() = runTest(testDispatcher) {
        val stats = UserStats(totalReviewed = 50)
        every { statsRepository.observeStats() } returns flowOf(stats)
        val vm = AchievementsViewModel(achievementRepository, statsRepository)
        assertNotNull(vm.uiState.value.stats)
        assertEquals(50, vm.uiState.value.stats?.totalReviewed)
    }

    @Test
    fun `state updates when multiple unlocks occur`() = runTest(testDispatcher) {
        every { achievementRepository.observeUnlockedIds() } returns flowOf(listOf("first_steps", "spring_cleaning", "memory_keeper", "streak_master"))
        val vm = AchievementsViewModel(achievementRepository, statsRepository)
        assertEquals(4, vm.uiState.value.unlockedCount)
        assertEquals(Achievement.ALL.size, vm.uiState.value.totalCount)
    }

    @Test
    fun `achievement progress is computed when stats available`() = runTest(testDispatcher) {
        val stats = UserStats(totalReviewed = 25)
        every { statsRepository.observeStats() } returns flowOf(stats)
        val vm = AchievementsViewModel(achievementRepository, statsRepository)

        val achievement = Achievement.ALL.find { it.id == "first_steps" }!!
        val progress = achievement.progress(stats)
        assertEquals(0.5f, progress)
    }

    @Test
    fun `unlocked achievement progress is 1f`() = runTest(testDispatcher) {
        val stats = UserStats(totalReviewed = 50)
        every { statsRepository.observeStats() } returns flowOf(stats)
        every { achievementRepository.observeUnlockedIds() } returns flowOf(listOf("first_steps"))
        val vm = AchievementsViewModel(achievementRepository, statsRepository)

        val achievement = Achievement.ALL.find { it.id == "first_steps" }!!
        assertTrue(achievement.isUnlocked(stats))
        assertEquals(1f, achievement.progress(stats))
    }

    @Test
    fun `storage saver progress reflects bytes freed`() = runTest(testDispatcher) {
        val stats = UserStats(storageFreed = 2_500_000_000L)
        val vm = AchievementsViewModel(achievementRepository, statsRepository)

        val achievement = Achievement.ALL.find { it.id == "storage_saver" }!!
        assertEquals(0.5f, achievement.progress(stats), 0.01f)
    }
}
