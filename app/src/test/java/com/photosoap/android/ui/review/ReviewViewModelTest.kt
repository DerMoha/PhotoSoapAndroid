package com.photosoap.android.ui.review

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import com.photosoap.android.PermissionChecker
import com.photosoap.android.MediaAccess
import kotlinx.coroutines.flow.MutableStateFlow
import android.util.Log
import android.net.Uri
import android.provider.MediaStore
import android.content.ContentUris
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

    private val permissionChecker = mockk<PermissionChecker>()
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
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
        mockkStatic(Uri::class)
        mockkStatic(MediaStore.Files::class)
        mockkStatic(ContentUris::class)
        every { MediaStore.Files.getContentUri(any<String>()) } returns mockk(relaxed = true)
        every { ContentUris.withAppendedId(isNull(), any()) } answers {
            val id = secondArg<Long>()
            val uri = mockk<Uri>()
            every { uri.toString() } returns "content://media/external/images/media/$id"
            uri
        }
        every { Uri.parse(any()) } returns mockk(relaxed = true)
        every { context.contentResolver } returns contentResolver
        every { permissionChecker.getMediaAccess() } returns MediaAccess.FULL
        every { settingsRepository.hapticsEnabled } returns flowOf(true)
        val emptyCursor = mockk<Cursor>(relaxed = true)
        every { emptyCursor.moveToFirst() } returns false
        every { emptyCursor.moveToNext() } returns false
        every { contentResolver.query(any(), any(), any(), any(), any()) } returns emptyCursor
        every { settingsRepository.previewHintSeen } returns flowOf(false)
        every { settingsRepository.hideFavorites } returns flowOf(true)
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
        unmockkStatic(Log::class)
        unmockkStatic(Uri::class)
        unmockkStatic(MediaStore.Files::class)
        unmockkStatic(ContentUris::class)
        Dispatchers.resetMain()
    }

    private fun createViewModel() = ReviewViewModel(
        context = context,
        photoRepository = photoRepository,
        statsRepository = statsRepository,
        settingsRepository = settingsRepository,
        achievementRepository = achievementRepository,
        metricsRepository = metricsRepository,
        permissionChecker = permissionChecker,
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
    fun `change sort order to shuffled updates state and persists preference`() = runTest(testDispatcher) {
        val vm = createViewModel()
        vm.onEvent(ReviewUiEvent.ChangeSortOrder(SortOrder.SHUFFLED))

        assertEquals(SortOrder.SHUFFLED, vm.uiState.value.sortOrder)
        coVerify { settingsRepository.setSortOrder("SHUFFLED") }
    }

    @Test
    fun `photo and video decisions update separate counters and cycle summary`() = runTest(testDispatcher) {
        val reviewed = MutableStateFlow<List<String>>(emptyList())
        every { photoRepository.observeReviewedPhotoUris() } returns reviewed
        coEvery { photoRepository.markReviewed(any()) } answers { reviewed.value = reviewed.value + firstArg<String>() }
        var stored = UserStats()
        coEvery { statsRepository.getStats() } answers { stored }
        coEvery { statsRepository.updateStats(any()) } answers { stored = firstArg() }
        every { contentResolver.query(any(), any(), any(), any(), any()) } answers {
            val projection = secondArg<Array<String>>()
            val cursor = mockk<Cursor>(relaxed = true)
            var row = -1
            every { cursor.moveToNext() } answers { ++row < 2 }
            every { cursor.getColumnIndexOrThrow(any()) } answers { projection.indexOf(firstArg()) }
            every { cursor.getLong(any()) } answers {
                when (projection[firstArg<Int>()]) {
                    "_id" -> row + 1L
                    "date_added" -> 1_700_000_000L
                    "_size" -> 1024L
                    else -> 0L
                }
            }
            every { cursor.getString(any()) } answers {
                when (projection[firstArg<Int>()]) {
                    "mime_type" -> if (row == 0) "image/jpeg" else "video/mp4"
                    "_display_name" -> if (row == 0) "photo.jpg" else "video.mp4"
                    else -> "Camera"
                }
            }
            cursor
        }
        val vm = createViewModel()
        vm.onEvent(ReviewUiEvent.Swiped(SwipeDirection.KEEP))
        vm.onEvent(ReviewUiEvent.Swiped(SwipeDirection.KEEP))
        assertEquals(1, stored.photosReviewed)
        assertEquals(1, stored.videosReviewed)
        assertEquals(1, stored.photosKept)
        assertEquals(1, stored.videosKept)
        assertEquals(2, vm.uiState.value.cycleReviewed)
        assertEquals(2, vm.uiState.value.cycleKept)
        assertTrue(vm.uiState.value.isReviewComplete)
        vm.onEvent(ReviewUiEvent.OpenFilterSheet)
        assertEquals(2, vm.uiState.value.calendarProgress.values.sumOf { it.total })
        assertEquals(2, vm.uiState.value.calendarProgress.values.sumOf { it.reviewed })
        assertTrue(vm.uiState.value.calendarProgress.values.all { it.isComplete })
        vm.onResume()
        assertTrue(vm.uiState.value.isReviewComplete)
        val relaunched = createViewModel()
        assertTrue(relaunched.uiState.value.isReviewComplete)
        assertTrue(relaunched.uiState.value.photos.isEmpty())
    }

    @Test
    fun `filter load errors can be retried without showing stale success`() = runTest(testDispatcher) {
        every { contentResolver.query(any(), any(), any(), any(), any()) } returns null
        val vm = createViewModel()
        vm.onEvent(ReviewUiEvent.OpenFilterSheet)
        assertTrue(vm.uiState.value.filterError)
        assertFalse(vm.uiState.value.filterLoading)
        val empty = mockk<Cursor>(relaxed = true)
        every { empty.moveToNext() } returns false
        every { contentResolver.query(any(), any(), any(), any(), any()) } returns empty
        vm.onEvent(ReviewUiEvent.RetryFilters)
        assertFalse(vm.uiState.value.filterError)
        assertFalse(vm.uiState.value.filterLoading)
        assertTrue(vm.uiState.value.years.isEmpty())
    }

    @Test
    fun `confirmed video deletion increments only video counters`() = runTest(testDispatcher) {
        val item = PendingDeletionItem("video", "content://media/external/video/media/1", "clip.mp4", 8192, 1, "video/mp4")
        val receipt = """{"requestId":"video-receipt","itemIds":["video"],"items":${Json.encodeToString(listOf(item))},"confirmedItemIds":["video"]}"""
        every { settingsRepository.pendingDeletionRequest } returns flowOf(receipt)
        createViewModel()
        coVerify(exactly = 1) { statsRepository.updateStatsForDeletionOnce("video-receipt", match {
            it.videosDeleted == 1 && it.videoStorageFreed == 8192L && it.photosDeleted == 0 && it.photoStorageFreed == 0L
        }) }
    }

    @Test
    fun `favorites and preview hint preferences are persisted`() = runTest(testDispatcher) {
        val vm = createViewModel()
        vm.onEvent(ReviewUiEvent.ChangeHideFavorites(false))
        assertFalse(vm.uiState.value.hideFavorites)
        coVerify { settingsRepository.setHideFavorites(false) }
        vm.onEvent(ReviewUiEvent.TappedCard)
        assertTrue(vm.uiState.value.previewHintSeen)
        coVerify { settingsRepository.setPreviewHintSeen() }
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
                it.totalDeleted == 1 && it.storageFreed == 2_048L && it.photosDeleted == 1 && it.photoStorageFreed == 2_048L && it.videosDeleted == 0
            })
            metricsRepository.trackBatchDeletionOnce("request-1", 1, 2_048)
            settingsRepository.setPendingDeletionRequest("")
        }
    }
    @Test
    fun `delete preference changes apply without recreating the review session`() = runTest(testDispatcher) {
        val preference = MutableStateFlow(true)
        every { settingsRepository.useDeleteQueue } returns preference
        val vm = createViewModel()
        assertTrue(vm.uiState.value.useDeleteQueue)
        preference.value = false
        assertFalse(vm.uiState.value.useDeleteQueue)
        preference.value = true
        assertTrue(vm.uiState.value.useDeleteQueue)
    }

    @Test
    fun `restricted access never converts invisible queued media into confirmed deletions`() = runTest(testDispatcher) {
        every { permissionChecker.getMediaAccess() } returns MediaAccess.LIMITED
        val item = PendingDeletionItem("item-1", "content://media/external/images/media/1", "photo.jpg", 2048, 1, "image/jpeg")
        every { settingsRepository.pendingDeletions } returns flowOf(Json.encodeToString(listOf(item)))
        every { settingsRepository.pendingDeletionRequest } returns flowOf(
            """{"requestId":"request-1","itemIds":["item-1"]}""",
        )
        val vm = createViewModel()
        assertEquals(listOf(item), vm.uiState.value.pendingDeletions)
        coVerify(exactly = 0) { statsRepository.updateStatsForDeletionOnce(any(), any()) }
        coVerify(exactly = 0) { metricsRepository.trackBatchDeletionOnce(any(), any(), any()) }
    }

    @Test
    fun `null media provider cursor is a recoverable error instead of an empty library`() = runTest(testDispatcher) {
        every { contentResolver.query(any(), any(), any(), any(), any()) } returns null
        val vm = createViewModel()
        assertEquals(ReviewLoadError.UNAVAILABLE, vm.uiState.value.loadError)
        val emptyCursor = mockk<Cursor>(relaxed = true)
        every { contentResolver.query(any(), any(), any(), any(), any()) } returns emptyCursor
        vm.onEvent(ReviewUiEvent.RetryLoad)
        assertNull(vm.uiState.value.loadError)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `permission failure presents an access recovery state`() = runTest(testDispatcher) {
        every { contentResolver.query(any(), any(), any(), any(), any()) } throws SecurityException("revoked")
        val vm = createViewModel()
        assertEquals(ReviewLoadError.ACCESS_DENIED, vm.uiState.value.loadError)
    }

    @Test
    fun `late deletion results without an active request cannot alter statistics`() = runTest(testDispatcher) {
        val vm = createViewModel()
        vm.onDeletionRequestResult(true)
        vm.onDeletionRequestResult(true)
        coVerify(exactly = 0) { statsRepository.updateStatsForDeletionOnce(any(), any()) }
        coVerify(exactly = 0) { metricsRepository.trackBatchDeletionOnce(any(), any(), any()) }
    }

    @Test
    fun `duplicate gestures for an old photo cannot advance or count the next photo`() = runTest(testDispatcher) {
        every { contentResolver.query(any(), any(), any(), any(), any()) } answers {
            val projection = secondArg<Array<String>>()
            if (projection.size < 10) mockk<Cursor>(relaxed = true)
            else {
                var row = -1
                mockk<Cursor>(relaxed = true) {
                    every { getColumnIndexOrThrow(any()) } answers { projection.indexOf(firstArg<String>()) }
                    every { moveToNext() } answers { ++row < 2 }
                    every { getLong(any()) } answers { if (firstArg<Int>() == 0) (row + 1).toLong() else 1000L }
                    every { getString(any()) } answers { if (firstArg<Int>() == 2) "image/jpeg" else "photo.jpg" }
                }
            }
        }
        val vm = createViewModel()
        assertNull(vm.uiState.value.loadError)
        val first = requireNotNull(vm.uiState.value.currentPhoto).uri
        vm.onEvent(ReviewUiEvent.Swiped(SwipeDirection.KEEP, first))
        assertEquals(1, vm.uiState.value.currentIndex)
        vm.onEvent(ReviewUiEvent.Swiped(SwipeDirection.DELETE, first))
        assertEquals(1, vm.uiState.value.currentIndex)
        assertTrue(vm.uiState.value.pendingDeletions.isEmpty())
        coVerify(exactly = 1) { photoRepository.markReviewed(first) }
        coVerify(exactly = 1) { metricsRepository.trackKept() }
    }

    @Test
    fun `background refresh keeps review interactive and preserves the next photo during a swipe`() = runTest(testDispatcher) {
        var ids = listOf(1L, 2L, 3L)
        every { contentResolver.query(any(), any(), any(), any(), any()) } answers {
            val projection = secondArg<Array<String>>()
            if (projection.size < 10) mockk<Cursor>(relaxed = true)
            else {
                val rows = ids.toList()
                var row = -1
                mockk<Cursor>(relaxed = true) {
                    every { getColumnIndexOrThrow(any()) } answers { projection.indexOf(firstArg<String>()) }
                    every { moveToNext() } answers { ++row < rows.size }
                    every { getLong(any()) } answers { if (firstArg<Int>() == 0) rows[row] else 1000L }
                    every { getString(any()) } answers { if (firstArg<Int>() == 2) "image/jpeg" else "photo.jpg" }
                }
            }
        }
        val vm = createViewModel()
        val first = requireNotNull(vm.uiState.value.currentPhoto).uri
        val next = vm.uiState.value.photos[1].uri
        val gate = kotlinx.coroutines.CompletableDeferred<List<String>>()
        every { photoRepository.observeReviewedPhotoUris() } returns kotlinx.coroutines.flow.flow { emit(gate.await()) }
        ids = listOf(9L, 1L, 2L, 3L)
        vm.onResume()
        assertFalse(vm.uiState.value.isLoading)
        assertEquals(first, vm.uiState.value.currentPhoto?.uri)
        vm.onEvent(ReviewUiEvent.Swiped(SwipeDirection.KEEP, first))
        gate.complete(emptyList())
        advanceUntilIdle()
        assertEquals(next, vm.uiState.value.currentPhoto?.uri)
        assertEquals(listOf(2L, 3L, 9L), vm.uiState.value.photos.map { it.id })
        assertFalse(vm.uiState.value.isLoading)
        coVerify(exactly = 1) { photoRepository.markReviewed(first) }
        val filterGate = kotlinx.coroutines.CompletableDeferred<List<String>>()
        every { photoRepository.observeReviewedPhotoUris() } returns kotlinx.coroutines.flow.flow { emit(filterGate.await()) }
        vm.onEvent(ReviewUiEvent.ChangeSortOrder(SortOrder.OLDEST_FIRST))
        assertTrue(vm.uiState.value.isLoading)
        vm.onResume()
        assertTrue(vm.uiState.value.isLoading)
        filterGate.complete(emptyList())
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `legacy confirmed deletion is counted once and removes only successfully deleted media`() = runTest(testDispatcher) {
        val items = listOf(
            PendingDeletionItem("one", "content://media/1", "one.jpg", 1024, 1, "image/jpeg"),
            PendingDeletionItem("two", "content://media/2", "two.jpg", 2048, 1, "image/jpeg"),
        )
        every { settingsRepository.pendingDeletions } returns flowOf(Json.encodeToString(items))
        val present = mockk<Cursor>(relaxed = true)
        every { present.moveToFirst() } returns true
        every { contentResolver.query(any(), any(), any(), any(), any()) } returns present
        every { contentResolver.delete(any(), any(), any()) } returnsMany listOf(1, 0)
        val vm = createViewModel()
        vm.onEvent(ReviewUiEvent.ConfirmDelete)
        assertEquals(listOf(items[1]), vm.uiState.value.pendingDeletions)
        assertFalse(vm.uiState.value.isDeleting)
        vm.onDeletionRequestResult(true)
        coVerify(exactly = 1) { statsRepository.updateStatsForDeletionOnce(any(), match { it.totalDeleted == 1 && it.storageFreed == 1024L }) }
        coVerify(exactly = 1) { metricsRepository.trackBatchDeletionOnce(any(), 1, 1024) }
    }

    @Test
    fun `confirmed receipt recovers even when selected-media access hides the deleted rows`() = runTest(testDispatcher) {
        every { permissionChecker.getMediaAccess() } returns MediaAccess.LIMITED
        val item = PendingDeletionItem("one", "content://media/1", "one.jpg", 2048, 1, "image/jpeg")
        every { settingsRepository.pendingDeletions } returns flowOf(Json.encodeToString(listOf(item)))
        val receipt = """{"requestId":"confirmed-1","itemIds":["one"],"items":${Json.encodeToString(listOf(item))},"confirmedItemIds":["one"]}"""
        every { settingsRepository.pendingDeletionRequest } returns flowOf(receipt)
        val vm = createViewModel()
        assertTrue(vm.uiState.value.pendingDeletions.isEmpty())
        coVerify(exactly = 1) { statsRepository.updateStatsForDeletionOnce("confirmed-1", match { it.totalDeleted == 1 && it.storageFreed == 2048L }) }
        coVerify(exactly = 1) { metricsRepository.trackBatchDeletionOnce("confirmed-1", 1, 2048) }
    }

    @Test
    fun `restored system result uses its original request snapshot under limited access`() = runTest(testDispatcher) {
        every { permissionChecker.getMediaAccess() } returns MediaAccess.LIMITED
        val item = PendingDeletionItem("one", "content://media/1", "one.jpg", 4096, 1, "image/jpeg")
        every { settingsRepository.pendingDeletions } returns flowOf(Json.encodeToString(listOf(item)))
        val journal = """{"requestId":"restored-1","itemIds":["one"],"items":${Json.encodeToString(listOf(item))}}"""
        every { settingsRepository.pendingDeletionRequest } returns flowOf(journal)
        val vm = createViewModel()
        vm.onDeletionRequestResult(true, "unrelated-request")
        assertEquals(listOf(item), vm.uiState.value.pendingDeletions)
        coVerify(exactly = 0) { statsRepository.updateStatsForDeletionOnce(any(), any()) }
        vm.onDeletionRequestResult(true, "restored-1")
        vm.onDeletionRequestResult(true, "restored-1")
        assertTrue(vm.uiState.value.pendingDeletions.isEmpty())
        assertFalse(vm.uiState.value.isDeleting)
        coVerify(exactly = 1) { statsRepository.updateStatsForDeletionOnce("restored-1", match { it.totalDeleted == 1 && it.storageFreed == 4096L }) }
        coVerify(exactly = 1) { metricsRepository.trackBatchDeletionOnce("restored-1", 1, 4096) }
    }

}
