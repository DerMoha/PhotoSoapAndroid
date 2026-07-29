package com.photosoap.android.ui.review

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.photosoap.android.domain.model.Achievement
import com.photosoap.android.domain.model.MediaKind
import com.photosoap.android.domain.model.PendingDeletionItem
import com.photosoap.android.domain.model.ReviewFilter
import com.photosoap.android.domain.model.SortOrder
import com.photosoap.android.domain.model.SwipeDirection
import com.photosoap.android.domain.model.UserStats
import com.photosoap.android.domain.repository.AchievementRepository
import com.photosoap.android.domain.repository.MetricsRepository
import com.photosoap.android.domain.repository.PhotoRepository
import com.photosoap.android.domain.repository.SettingsRepository
import com.photosoap.android.domain.repository.StatsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewViewModelTest {

    private val contentResolver = mockk<ContentResolver>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)
    private val photoRepository = mockk<PhotoRepository>(relaxed = true)
    private val statsRepository = mockk<StatsRepository>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val achievementRepository = mockk<AchievementRepository>(relaxed = true)
    private val metricsRepository = mockk<MetricsRepository>(relaxed = true)

    private val testDispatcher = UnconfinedTestDispatcher()
    private val viewModels = mutableListOf<ReviewViewModel>()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns mockk(relaxed = true)
        every { context.contentResolver } returns contentResolver
        every { settingsRepository.mediaKind } returns flowOf("all")
        every { settingsRepository.sortOrder } returns flowOf("newest_first")
        every { settingsRepository.useDeleteQueue } returns flowOf(true)
        every { settingsRepository.pendingDeletions } returns flowOf("[]")
        every { settingsRepository.pendingDeletionRequest } returns flowOf("")
        every { photoRepository.observeReviewedPhotoUris() } returns flowOf(emptyList())
        every { statsRepository.observeStats() } returns flowOf(null)
        coEvery { statsRepository.getStats() } returns null
        coEvery { statsRepository.createIfNeeded() } returns UserStats()
        coEvery { statsRepository.updateStatsForDeletionOnce(any(), any()) } returns true
        coEvery { achievementRepository.getUnlockedIds() } returns emptyList()
    }

    @AfterEach
    fun tearDown() {
        viewModels.forEach { it.viewModelScope.cancel() }
        viewModels.clear()
        unmockkStatic(Uri::class)
        Dispatchers.resetMain()
    }

    private fun createViewModel() = ReviewViewModel(
        context = context,
        photoRepository = photoRepository,
        statsRepository = statsRepository,
        settingsRepository = settingsRepository,
        achievementRepository = achievementRepository,
        metricsRepository = metricsRepository,
        ioDispatcher = testDispatcher,
    ).also(viewModels::add)

    @Test
    fun `initial state is created`() = runTest(testDispatcher) {
        val vm = createViewModel()
        assertNotNull(vm.uiState.value)
    }

    @Test
    fun `persisted media preferences are applied before the first review query`() = runTest(testDispatcher) {
        every { settingsRepository.mediaKind } returns flowOf("VIDEOS")
        every { settingsRepository.sortOrder } returns flowOf("OLDEST_FIRST")

        val vm = createViewModel()

        assertEquals(MediaKind.VIDEOS, vm.uiState.value.mediaKind)
        assertEquals(SortOrder.OLDEST_FIRST, vm.uiState.value.sortOrder)
    }

    @Test
    fun `a new review session resets session-only counters`() = runTest(testDispatcher) {
        val previous = UserStats(sessionReviewCount = 12, currentStreak = 8, bestStreak = 20)
        every { statsRepository.observeStats() } returns flowOf(previous)
        coEvery { statsRepository.getStats() } returns previous

        createViewModel()

        coVerify {
            statsRepository.updateStats(match {
                it.sessionReviewCount == 0 && it.currentStreak == 0 && it.bestStreak == 20
            })
        }
    }

    @Test
    fun `change media kind updates state`() = runTest(testDispatcher) {
        val vm = createViewModel()
        vm.onEvent(ReviewUiEvent.ChangeMediaKind(MediaKind.VIDEOS))
        assertEquals(MediaKind.VIDEOS, vm.uiState.value.mediaKind)
    }

    @Test
    fun `change sort order updates state`() = runTest(testDispatcher) {
        val vm = createViewModel()
        vm.onEvent(ReviewUiEvent.ChangeSortOrder(SortOrder.OLDEST_FIRST))
        assertEquals(SortOrder.OLDEST_FIRST, vm.uiState.value.sortOrder)
    }

    @Test
    fun `open filter sheet shows sheet`() = runTest(testDispatcher) {
        val vm = createViewModel()
        vm.onEvent(ReviewUiEvent.OpenFilterSheet)
        assertTrue(vm.uiState.value.showFilterSheet)
    }

    @Test
    fun `close filter sheet hides sheet`() = runTest(testDispatcher) {
        val vm = createViewModel()
        vm.onEvent(ReviewUiEvent.OpenFilterSheet)
        vm.onEvent(ReviewUiEvent.CloseFilterSheet)
        assertTrue(!vm.uiState.value.showFilterSheet)
    }

    @Test
    fun `dismiss toast clears message`() = runTest(testDispatcher) {
        val vm = createViewModel()
        vm.onEvent(ReviewUiEvent.DismissToast)
        assertNull(vm.uiState.value.toastMessage)
    }

    @Test
    fun `tapped card shows photo preview`() = runTest(testDispatcher) {
        val vm = createViewModel()
        vm.onEvent(ReviewUiEvent.TappedCard)
        assertTrue(vm.uiState.value.showPhotoPreview)
    }

    @Test
    fun `close photo preview hides preview`() = runTest(testDispatcher) {
        val vm = createViewModel()
        vm.onEvent(ReviewUiEvent.TappedCard)
        vm.onEvent(ReviewUiEvent.ClosePhotoPreview)
        assertTrue(!vm.uiState.value.showPhotoPreview)
    }

    @Test
    fun `toggle delete queue flips useDeleteQueue`() = runTest(testDispatcher) {
        val vm = createViewModel()
        vm.onEvent(ReviewUiEvent.ToggleDeleteQueue)
        assertFalse(vm.uiState.value.useDeleteQueue)
    }

    @Test
    fun `change filter updates state`() = runTest(testDispatcher) {
        val vm = createViewModel()
        vm.onEvent(ReviewUiEvent.ChangeFilter(ReviewFilter.Year(2023)))
        assertEquals(ReviewFilter.Year(2023), vm.uiState.value.filter)
    }

    @Test
    fun `open delete queue shows sheet`() = runTest(testDispatcher) {
        val vm = createViewModel()
        vm.onEvent(ReviewUiEvent.OpenDeleteQueue)
        assertTrue(vm.uiState.value.showDeleteQueueSheet)
    }

    @Test
    fun `dismiss delete queue hides sheet`() = runTest(testDispatcher) {
        val vm = createViewModel()
        vm.onEvent(ReviewUiEvent.OpenDeleteQueue)
        vm.onEvent(ReviewUiEvent.DismissDeleteQueue)
        assertFalse(vm.uiState.value.showDeleteQueueSheet)
    }

    @Test
    fun `externally removed queued media is pruned without being counted as kept`() = runTest(testDispatcher) {
        val item = PendingDeletionItem(
            id = "item-1",
            uri = "content://media/external/images/media/1",
            displayName = "photo.jpg",
            fileSize = 1_024,
            queuedAt = 1,
            mimeType = "image/jpeg",
        )
        every { settingsRepository.pendingDeletions } returns flowOf(Json.encodeToString(listOf(item)))

        val vm = createViewModel()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.pendingDeletions.isEmpty())
        coVerify(exactly = 0) { metricsRepository.trackKept(any()) }
        coVerify { settingsRepository.setPendingDeletions("[]") }
    }

    @Test
    fun `missing media from interrupted system request is recovered exactly once`() = runTest(testDispatcher) {
        val item = PendingDeletionItem(
            id = "item-1",
            uri = "content://media/external/images/media/1",
            displayName = "photo.jpg",
            fileSize = 2_048,
            queuedAt = 1,
            mimeType = "image/jpeg",
        )
        every { settingsRepository.pendingDeletions } returns flowOf(Json.encodeToString(listOf(item)))
        every { settingsRepository.pendingDeletionRequest } returns flowOf(
            """{"requestId":"request-1","itemIds":["item-1"]}""",
        )

        createViewModel()
        advanceUntilIdle()

        coVerify(exactly = 1) {
            statsRepository.updateStatsForDeletionOnce("request-1", match {
                it.totalDeleted == 1 && it.storageFreed == 2_048L
            })
            metricsRepository.trackBatchDeletionOnce("request-1", 1, 2_048)
            settingsRepository.setPendingDeletionRequest("")
        }
    }
}
