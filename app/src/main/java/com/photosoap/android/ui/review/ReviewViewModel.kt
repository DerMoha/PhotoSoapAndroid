package com.photosoap.android.ui.review

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photosoap.android.domain.model.AlbumInfo
import com.photosoap.android.domain.model.MediaKind
import com.photosoap.android.domain.model.PendingDeletionItem
import com.photosoap.android.domain.model.Photo
import com.photosoap.android.domain.model.ReviewFilter
import com.photosoap.android.domain.model.SortOrder
import com.photosoap.android.domain.model.SwipeDirection
import com.photosoap.android.domain.model.UserStats
import com.photosoap.android.domain.repository.PhotoRepository
import com.photosoap.android.domain.repository.SettingsRepository
import com.photosoap.android.domain.repository.StatsRepository
import com.photosoap.android.domain.model.DailyChallenge
import com.photosoap.android.domain.model.Achievement
import com.photosoap.android.domain.repository.AchievementRepository
import com.photosoap.android.domain.repository.MetricsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ReviewViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val photoRepository: PhotoRepository,
    private val statsRepository: StatsRepository,
    private val settingsRepository: SettingsRepository,
    private val achievementRepository: AchievementRepository,
    private val metricsRepository: MetricsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    private val statsMutex = Mutex()
    private var statsSnapshot: UserStats? = null
    private val reviewedInSession = mutableSetOf<String>()

    private var mediaObserver: ContentObserver? = null

    init {
        loadSettings()
        loadPhotos()
        loadFilterData()
        observeStats()
        registerContentObserver()
    }

    override fun onCleared() {
        super.onCleared()
        mediaObserver?.let { context.contentResolver.unregisterContentObserver(it) }
    }

    fun onEvent(event: ReviewUiEvent) {
        when (event) {
            is ReviewUiEvent.Swiped -> handleSwipe(event.direction)
            ReviewUiEvent.TappedCard -> _uiState.update { it.copy(showPhotoPreview = true, previewPhoto = it.currentPhoto) }
            ReviewUiEvent.UndoLastDeletion -> undoLastDeletion()
            ReviewUiEvent.OpenDeleteQueue -> _uiState.update { it.copy(showDeleteQueueSheet = true) }
            ReviewUiEvent.ConfirmDelete -> viewModelScope.launch { executeDeletion() }
            ReviewUiEvent.CancelDeleteConfirm -> _uiState.update { it.copy(showDeleteConfirmSheet = false) }
            ReviewUiEvent.DismissDeleteQueue -> _uiState.update { it.copy(showDeleteQueueSheet = false) }
            is ReviewUiEvent.RemoveFromQueue -> removeFromQueue(event.itemId)
            ReviewUiEvent.ClearQueue -> clearQueue()
            is ReviewUiEvent.ChangeMediaKind -> changeMediaKind(event.kind)
            is ReviewUiEvent.ChangeSortOrder -> changeSortOrder(event.order)
            is ReviewUiEvent.ChangeFilter -> changeFilter(event.filter)
            ReviewUiEvent.OpenFilterSheet -> _uiState.update { it.copy(showFilterSheet = true) }
            ReviewUiEvent.CloseFilterSheet -> _uiState.update { it.copy(showFilterSheet = false) }
            ReviewUiEvent.OpenPhotoPreview -> _uiState.update { it.copy(showPhotoPreview = true, previewPhoto = it.currentPhoto) }
            ReviewUiEvent.ClosePhotoPreview -> _uiState.update { it.copy(showPhotoPreview = false, previewPhoto = null) }
            ReviewUiEvent.StartOver -> startOver()
            ReviewUiEvent.DismissToast -> _uiState.update { it.copy(toastMessage = null) }
            ReviewUiEvent.ToggleDeleteQueue -> toggleDeleteQueue()
            is ReviewUiEvent.SelectYear -> selectYear(event.year)
            ReviewUiEvent.DeselectYear -> deselectYear()
        }
    }

    private fun handleSwipe(direction: SwipeDirection) {
        val photo = _uiState.value.currentPhoto ?: return

        saveSnapshot()

        when (direction) {
            SwipeDirection.KEEP -> {
                reviewInSession(photo.uri)
                advanceStats(kept = true)
                viewModelScope.launch { metricsRepository.trackReview() }
            }
            SwipeDirection.DELETE -> {
                reviewInSession(photo.uri)
                advanceStats(deleted = true, fileSize = photo.fileSize)
                addToDeletionQueue(photo)
                viewModelScope.launch {
                    metricsRepository.trackDeletion(photo.fileSize)
                }
                if (!_uiState.value.useDeleteQueue) {
                    executeDeletion()
                }
            }
        }

        advanceToNextPhoto()
        checkAchievements()
        checkDailyChallenge()
    }

    private fun advanceToNextPhoto() {
        viewModelScope.launch {
            val state = _uiState.value
            val nextIndex = state.currentIndex + 1
            if (nextIndex >= state.photos.size) {
                _uiState.update {
                    it.copy(
                        currentIndex = nextIndex,
                        isReviewComplete = true,
                    )
                }
            } else {
                _uiState.update { it.copy(currentIndex = nextIndex) }
            }
        }
    }

    private fun saveSnapshot() {
        statsSnapshot = _uiState.value.stats
    }

    private fun reviewInSession(uri: String) {
        reviewedInSession.add(uri)
        viewModelScope.launch {
            photoRepository.markReviewed(uri)
        }
    }

    private fun advanceStats(kept: Boolean = false, deleted: Boolean = false, fileSize: Long = 0) {
        viewModelScope.launch {
            statsMutex.withLock {
                val now = System.currentTimeMillis()
                val current = statsRepository.getStats() ?: statsRepository.createIfNeeded()
                val todayStart = java.time.LocalDate.now()
                    .atStartOfDay(java.time.ZoneId.systemDefault())
                    .toInstant().toEpochMilli()

                val isNewDay = current.todayDate != todayStart
                val newTodayCount = if (isNewDay) 1 else current.todayReviewCount + 1

                val newDayStreak = when {
                    current.lastReviewDate == null -> 1
                    else -> {
                        val lastReviewDay = java.time.Instant.ofEpochMilli(current.lastReviewDate)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                        val today = java.time.LocalDate.now(java.time.ZoneId.systemDefault())
                        val daysSince = java.time.temporal.ChronoUnit.DAYS.between(lastReviewDay, today)
                        if (daysSince <= 1L) current.dayStreak + (if (daysSince == 1L || isNewDay) 1 else 0)
                        else 1
                    }
                }

                val updated = current.copy(
                    totalReviewed = current.totalReviewed + 1,
                    totalKept = if (kept) current.totalKept + 1 else current.totalKept,
                    totalDeleted = if (deleted) current.totalDeleted + 1 else current.totalDeleted,
                    storageFreed = current.storageFreed + fileSize,
                    sessionReviewCount = current.sessionReviewCount + 1,
                    currentStreak = current.currentStreak + 1,
                    bestStreak = maxOf(current.bestStreak, current.currentStreak + 1),
                    todayReviewCount = newTodayCount,
                    todayDate = todayStart,
                    bestDayReviewCount = maxOf(current.bestDayReviewCount, newTodayCount),
                    dayStreak = newDayStreak,
                    lastReviewDate = now,
                )

                statsRepository.updateStats(updated)
                _uiState.update {
                    it.copy(
                        stats = updated,
                        todayReviewCount = updated.todayReviewCount,
                    )
                }
            }
        }
    }

    private fun addToDeletionQueue(photo: Photo) {
        _uiState.update { state ->
            val item = PendingDeletionItem(
                id = UUID.randomUUID().toString(),
                uri = photo.uri,
                displayName = photo.displayName,
                fileSize = photo.fileSize,
                queuedAt = System.currentTimeMillis(),
            )
            state.copy(pendingDeletions = state.pendingDeletions + item)
        }
    }

    private fun undoLastDeletion() {
        val last = _uiState.value.pendingDeletions.lastOrNull() ?: return
        _uiState.update { it.copy(pendingDeletions = it.pendingDeletions.dropLast(1)) }
        viewModelScope.launch {
            photoRepository.unmarkReviewed(last.uri)
            reviewedInSession.remove(last.uri)
            revertDeletionStats(last.fileSize)
        }
    }

    private fun revertDeletionStats(fileSize: Long) {
        viewModelScope.launch {
            statsMutex.withLock {
                val current = statsRepository.getStats() ?: return@launch
                val updated = current.copy(
                    totalDeleted = (current.totalDeleted - 1).coerceAtLeast(0),
                    storageFreed = (current.storageFreed - fileSize).coerceAtLeast(0),
                )
                statsRepository.updateStats(updated)
                _uiState.update { it.copy(stats = updated) }
            }
        }
    }

    private fun executeDeletion() {
        val items = _uiState.value.pendingDeletions
        if (items.isEmpty()) return

        val uris = items.map { Uri.parse(it.uri) }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            try {
                val deleteRequest = android.provider.MediaStore.createDeleteRequest(
                    context.contentResolver, uris
                )
                _uiState.update {
                    it.copy(
                        pendingDeleteIntentSender = deleteRequest.intentSender,
                        showDeleteConfirmSheet = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        toastMessage = "Deletion failed. Try again.",
                        toastEmoji = "❌",
                        showDeleteConfirmSheet = false,
                    )
                }
            }
        } else {
            viewModelScope.launch {
                var deletedCount = 0
                withContext(Dispatchers.IO) {
                    for (uri in uris) {
                        try {
                            val rows = context.contentResolver.delete(uri, null, null)
                            if (rows > 0) deletedCount++
                        } catch (_: Exception) { }
                    }
                }
                if (deletedCount > 0) {
                    onDeletionComplete(true)
                } else {
                    _uiState.update {
                        it.copy(
                            toastMessage = "Manual deletion required on this device",
                            toastEmoji = "⚠️",
                            showDeleteConfirmSheet = false,
                        )
                    }
                }
            }
        }
    }

    fun onDeletionComplete(success: Boolean) {
        val items = _uiState.value.pendingDeletions
        val totalSize = items.sumOf { it.fileSize }
        if (success) {
            viewModelScope.launch {
                metricsRepository.trackBatchDeletion(items.size, totalSize)
            }
            _uiState.update {
                it.copy(
                    pendingDeletions = emptyList(),
                    pendingDeleteIntentSender = null,
                    toastMessage = "${items.size} items deleted",
                    toastEmoji = "🧹",
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    pendingDeleteIntentSender = null,
                    toastMessage = "Deletion cancelled",
                    toastEmoji = "↩️",
                )
            }
        }
    }

    private fun removeFromQueue(itemId: String) {
        val item = _uiState.value.pendingDeletions.find { it.id == itemId } ?: return
        _uiState.update { it.copy(pendingDeletions = it.pendingDeletions.filter { i -> i.id != itemId }) }
        viewModelScope.launch {
            photoRepository.unmarkReviewed(item.uri)
            reviewedInSession.remove(item.uri)
            revertDeletionStats(item.fileSize)
        }
    }

    private fun clearQueue() {
        val items = _uiState.value.pendingDeletions
        _uiState.update { it.copy(pendingDeletions = emptyList()) }
        viewModelScope.launch {
            items.forEach { photoRepository.unmarkReviewed(it.uri) }
            reviewedInSession.removeAll(items.map { it.uri }.toSet())
            statsMutex.withLock {
                val totalSize = items.sumOf { it.fileSize }
                val current = statsRepository.getStats() ?: return@launch
                val updated = current.copy(
                    totalDeleted = (current.totalDeleted - items.size).coerceAtLeast(0),
                    storageFreed = (current.storageFreed - totalSize).coerceAtLeast(0),
                )
                statsRepository.updateStats(updated)
                _uiState.update { it.copy(stats = updated) }
            }
        }
    }

    private fun changeMediaKind(kind: MediaKind) {
        _uiState.update { it.copy(mediaKind = kind) }
        viewModelScope.launch {
            settingsRepository.setMediaKind(kind.name)
        }
        loadPhotos()
    }

    private fun changeSortOrder(order: SortOrder) {
        _uiState.update { it.copy(sortOrder = order) }
        viewModelScope.launch {
            settingsRepository.setSortOrder(order.name)
        }
        loadPhotos()
    }

    private fun changeFilter(filter: ReviewFilter) {
        _uiState.update { it.copy(filter = filter, showFilterSheet = false) }
        loadPhotos()
    }

    private fun toggleDeleteQueue() {
        viewModelScope.launch {
            val newValue = !_uiState.value.useDeleteQueue
            _uiState.update { it.copy(useDeleteQueue = newValue) }
            settingsRepository.setUseDeleteQueue(newValue)
        }
    }

    private fun startOver() {
        viewModelScope.launch {
            photoRepository.clearReviewed()
            reviewedInSession.clear()
            _uiState.update {
                it.copy(
                    currentIndex = 0,
                    isReviewComplete = false,
                )
            }
            loadPhotos()
        }
    }

    private fun checkAchievements() {
        viewModelScope.launch {
            val stats = _uiState.value.stats ?: statsRepository.getStats() ?: return@launch
            val unlocked = achievementRepository.getUnlockedIds().toSet()
            val newlyUnlocked = Achievement.ALL
                .filter { it.id !in unlocked && it.isUnlocked(stats) }
                .map { it.id }

            if (newlyUnlocked.isNotEmpty()) {
                for (id in newlyUnlocked) {
                    achievementRepository.unlock(id)
                }
                val names = newlyUnlocked.mapNotNull { id ->
                    Achievement.ALL.find { it.id == id }?.let { "${it.iconName} ${it.title}" }
                }
                val message = names.joinToString("\n") + " unlocked!"
                _uiState.update {
                    it.copy(
                        toastMessage = message,
                        toastEmoji = "🏆",
                    )
                }
            }
        }
    }

    private var dailyChallengeToastShown = false

    private fun checkDailyChallenge() {
        viewModelScope.launch {
            val stats = statsRepository.getStats() ?: return@launch
            val challenge = DailyChallenge.generate()
            val progress = when (challenge.type) {
                DailyChallenge.ChallengeType.REVIEW -> stats.todayReviewCount
                DailyChallenge.ChallengeType.DELETE -> stats.totalDeleted
                DailyChallenge.ChallengeType.STREAK -> stats.currentStreak
            }

            _uiState.update {
                it.copy(
                    dailyChallengeProgress = progress,
                    dailyChallengeTarget = challenge.target,
                    dailyChallengeType = challenge.type.label,
                )
            }

            if (progress >= challenge.target && !dailyChallengeToastShown) {
                dailyChallengeToastShown = true
                _uiState.update {
                    it.copy(
                        toastMessage = "Daily goal complete!",
                        toastEmoji = "🎉",
                    )
                }
            }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val kind = settingsRepository.mediaKind.first()
            val order = settingsRepository.sortOrder.first()
            val useQueue = settingsRepository.useDeleteQueue.first()

            _uiState.update {
                it.copy(
                    mediaKind = try { MediaKind.valueOf(kind) } catch (_: Exception) { MediaKind.ALL },
                    sortOrder = try { SortOrder.valueOf(order) } catch (_: Exception) { SortOrder.NEWEST_FIRST },
                    useDeleteQueue = useQueue,
                )
            }
        }
    }

    private fun loadFilterData() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    loadAlbums()
                    loadYears()
                }
            } catch (_: Exception) { }
        }
    }

    private fun selectYear(year: Int) {
        _uiState.update { it.copy(selectedYear = year, months = emptyList()) }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                loadMonthsForYear(year)
            }
        }
    }

    private fun deselectYear() {
        _uiState.update { it.copy(selectedYear = null, months = emptyList()) }
    }

    private fun loadMonthsForYear(year: Int) {
        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(MediaStore.Files.FileColumns.DATE_TAKEN)
        val selection = "(${MediaStore.Files.FileColumns.MEDIA_TYPE}=${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}" +
            " OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO})"

        val calendar = java.util.Calendar.getInstance()
        val yearMonths = mutableSetOf<YearMonth>()

        context.contentResolver.query(
            uri, projection, selection, null,
            "${MediaStore.Files.FileColumns.DATE_TAKEN} DESC"
        )?.use { cursor ->
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_TAKEN)
            while (cursor.moveToNext()) {
                val dateTaken = cursor.getLong(dateCol)
                calendar.timeInMillis = dateTaken
                val yr = calendar.get(java.util.Calendar.YEAR)
                if (yr != year) continue
                val month = calendar.get(java.util.Calendar.MONTH) + 1
                yearMonths.add(YearMonth.of(yr, month))
            }
        }
        _uiState.update { it.copy(months = yearMonths.toList().sortedByDescending { m -> m.year * 12 + m.monthValue }) }
    }

    private fun loadAlbums() {
        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns.BUCKET_ID,
            MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
        )
        val selection = "(${MediaStore.Files.FileColumns.MEDIA_TYPE}=${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}" +
            " OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO})"
        val seenIds = mutableSetOf<Long>()
        val albums = mutableListOf<AlbumInfo>()

        context.contentResolver.query(
            uri, projection, selection, null,
            "${MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME} ASC"
        )?.use { cursor ->
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val bucketId = cursor.getLong(bucketIdCol)
                if (seenIds.add(bucketId)) {
                    albums.add(
                        AlbumInfo(
                            id = bucketId,
                            name = cursor.getString(bucketNameCol) ?: "Unknown",
                            count = 0,
                        )
                    )
                }
            }
        }
        _uiState.update { it.copy(albums = albums) }
    }

    private fun loadYears() {
        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(MediaStore.Files.FileColumns.DATE_TAKEN)
        val selection = "(${MediaStore.Files.FileColumns.MEDIA_TYPE}=${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}" +
            " OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO})"
        val years = mutableSetOf<Int>()

        context.contentResolver.query(
            uri, projection, selection, null,
            "${MediaStore.Files.FileColumns.DATE_TAKEN} DESC"
        )?.use { cursor ->
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_TAKEN)
            while (cursor.moveToNext()) {
                val dateTaken = cursor.getLong(dateCol)
                val calendar = java.util.Calendar.getInstance().apply { timeInMillis = dateTaken }
                val year = calendar.get(java.util.Calendar.YEAR)
                if (years.add(year)) {
                    if (years.size >= 50) break
                }
            }
        }
        _uiState.update { it.copy(years = years.toList().sortedDescending()) }
    }

    private fun registerContentObserver() {
        try {
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    loadPhotos()
                    loadFilterData()
                }

                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    loadPhotos()
                    loadFilterData()
                }
            }
            mediaObserver = observer
            val uri = MediaStore.Files.getContentUri("external")
            context.contentResolver.registerContentObserver(uri, true, observer)
        } catch (_: Exception) { }
    }

    private fun loadPhotos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val photos = try {
                withContext(Dispatchers.IO) {
                    val reviewedUris = photoRepository.observeReviewedPhotoUris().first()
                    queryPhotos().filter { it.uri !in reviewedUris }
                }
            } catch (e: Exception) {
                emptyList()
            }

            _uiState.update {
                it.copy(
                    photos = photos,
                    isLoading = false,
                    currentIndex = 0,
                    isReviewComplete = false,
                )
            }
        }
    }

    private fun queryPhotos(): List<Photo> {
        val state = _uiState.value
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATE_TAKEN,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.WIDTH,
            MediaStore.Files.FileColumns.HEIGHT,
            MediaStore.Files.FileColumns.DURATION,
            MediaStore.Files.FileColumns.BUCKET_ID,
            MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
        )

        val uri = MediaStore.Files.getContentUri("external")
        val selection = buildString {
            append("(${MediaStore.Files.FileColumns.MEDIA_TYPE}=${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}")
            append(" OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO})")

            when (state.mediaKind) {
                MediaKind.PHOTOS -> append(" AND ${MediaStore.Files.FileColumns.MEDIA_TYPE}=${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}")
                MediaKind.VIDEOS -> append(" AND ${MediaStore.Files.FileColumns.MEDIA_TYPE}=${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO}")
                MediaKind.ALL -> {}
            }

            when (val filter = state.filter) {
                is ReviewFilter.All -> {}
                is ReviewFilter.Year -> {
                    val calendar = java.util.Calendar.getInstance()
                    calendar.set(java.util.Calendar.YEAR, filter.year)
                    calendar.set(java.util.Calendar.DAY_OF_YEAR, 1)
                    calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    calendar.set(java.util.Calendar.MINUTE, 0)
                    calendar.set(java.util.Calendar.SECOND, 0)
                    calendar.set(java.util.Calendar.MILLISECOND, 0)
                    val yearStart = calendar.timeInMillis
                    calendar.add(java.util.Calendar.YEAR, 1)
                    val yearEnd = calendar.timeInMillis
                    append(" AND ${MediaStore.Files.FileColumns.DATE_TAKEN} >= $yearStart")
                    append(" AND ${MediaStore.Files.FileColumns.DATE_TAKEN} < $yearEnd")
                }
                is ReviewFilter.Month -> {
                    val calendar = java.util.Calendar.getInstance()
                    calendar.set(java.util.Calendar.YEAR, filter.year)
                    calendar.set(java.util.Calendar.MONTH, filter.month - 1)
                    calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
                    calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    calendar.set(java.util.Calendar.MINUTE, 0)
                    calendar.set(java.util.Calendar.SECOND, 0)
                    calendar.set(java.util.Calendar.MILLISECOND, 0)
                    val monthStart = calendar.timeInMillis
                    calendar.add(java.util.Calendar.MONTH, 1)
                    val monthEnd = calendar.timeInMillis
                    append(" AND ${MediaStore.Files.FileColumns.DATE_TAKEN} >= $monthStart")
                    append(" AND ${MediaStore.Files.FileColumns.DATE_TAKEN} < $monthEnd")
                }
                is ReviewFilter.Album -> {
                    append(" AND ${MediaStore.Files.FileColumns.BUCKET_ID} = ${filter.albumId}")
                }
            }
        }

        val sortOrder = when (state.sortOrder) {
            SortOrder.NEWEST_FIRST -> "${MediaStore.Files.FileColumns.DATE_TAKEN} DESC"
            SortOrder.OLDEST_FIRST -> "${MediaStore.Files.FileColumns.DATE_TAKEN} ASC"
        }

        val photos = mutableListOf<Photo>()
        context.contentResolver.query(
            uri, projection, selection, null, sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val dateTakenCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_TAKEN)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.HEIGHT)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DURATION)
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val contentUri = ContentUris.withAppendedId(uri, id)
                photos.add(
                    Photo(
                        id = id,
                        uri = contentUri.toString(),
                        displayName = cursor.getString(nameCol) ?: "Unknown",
                        mimeType = cursor.getString(mimeCol) ?: "image/jpeg",
                        dateTaken = cursor.getLong(dateTakenCol),
                        dateAdded = cursor.getLong(dateAddedCol),
                        fileSize = cursor.getLong(sizeCol),
                        width = cursor.getInt(widthCol),
                        height = cursor.getInt(heightCol),
                        duration = cursor.getLong(durationCol),
                        bucketId = cursor.getLong(bucketIdCol),
                        bucketName = cursor.getString(bucketNameCol) ?: "",
                    )
                )
            }
        }

        return photos
    }

    private fun observeStats() {
        viewModelScope.launch {
            statsRepository.observeStats().collect { stats ->
                _uiState.update {
                    it.copy(
                        stats = stats,
                        todayReviewCount = stats?.todayReviewCount ?: 0,
                    )
                }
            }
        }
    }
}
