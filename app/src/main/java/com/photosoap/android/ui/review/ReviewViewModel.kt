package com.photosoap.android.ui.review

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private var statsSnapshot: UserStats? = null
    private val reviewedInSession = mutableSetOf<String>()

    init {
        loadSettings()
        loadPhotos()
        observeStats()
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
        }
    }

    private fun handleSwipe(direction: SwipeDirection) {
        val photo = _uiState.value.currentPhoto ?: return

        saveSnapshot()

        when (direction) {
            SwipeDirection.KEEP -> {
                reviewInSession(photo.uri)
                advanceStats(kept = true)
            }
            SwipeDirection.DELETE -> {
                if (_uiState.value.useDeleteQueue) {
                    reviewInSession(photo.uri)
                    advanceStats(deleted = true, fileSize = photo.fileSize)
                    addToDeletionQueue(photo)
                } else {
                    reviewInSession(photo.uri)
                    advanceStats(deleted = true, fileSize = photo.fileSize)
                    // Immediate delete - would use createDeleteRequest
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
            val now = System.currentTimeMillis()
            val current = statsRepository.getStats() ?: statsRepository.createIfNeeded()
            val todayStart = java.time.LocalDate.now()
                .atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant().toEpochMilli()

            val updated = current.copy(
                totalReviewed = current.totalReviewed + 1,
                totalKept = if (kept) current.totalKept + 1 else current.totalKept,
                totalDeleted = if (deleted) current.totalDeleted + 1 else current.totalDeleted,
                storageFreed = current.storageFreed + fileSize,
                sessionReviewCount = current.sessionReviewCount + 1,
                currentStreak = current.currentStreak + 1,
                bestStreak = maxOf(current.bestStreak, current.currentStreak + 1),
                todayReviewCount = if (current.todayDate == todayStart) {
                    current.todayReviewCount + 1
                } else 1,
                todayDate = todayStart,
                bestDayReviewCount = maxOf(current.bestDayReviewCount, current.todayReviewCount + 1),
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
        _uiState.update { state ->
            if (state.pendingDeletions.isEmpty()) return@update state
            val last = state.pendingDeletions.last()
            viewModelScope.launch {
                photoRepository.unmarkReviewed(last.uri)
                reviewedInSession.remove(last.uri)
            }
            state.copy(pendingDeletions = state.pendingDeletions.dropLast(1))
        }
        rollbackStats()
    }

    private fun rollbackStats() {
        statsSnapshot?.let { snapshot ->
            viewModelScope.launch {
                statsRepository.updateStats(snapshot)
                _uiState.update { it.copy(stats = snapshot) }
            }
            statsSnapshot = null
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
        _uiState.update { state ->
            val item = state.pendingDeletions.find { it.id == itemId }
            if (item != null) {
                viewModelScope.launch {
                    photoRepository.unmarkReviewed(item.uri)
                    reviewedInSession.remove(item.uri)
                }
            }
            state.copy(pendingDeletions = state.pendingDeletions.filter { it.id != itemId })
        }
        rollbackStats()
    }

    private fun clearQueue() {
        val items = _uiState.value.pendingDeletions
        _uiState.update { it.copy(pendingDeletions = emptyList()) }
        viewModelScope.launch {
            items.forEach { photoRepository.unmarkReviewed(it.uri) }
            reviewedInSession.removeAll(items.map { it.uri }.toSet())
        }
        rollbackStats()
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

    private suspend fun checkAchievements() {
        val stats = _uiState.value.stats ?: statsRepository.getStats() ?: return
        val unlocked = achievementRepository.getUnlockedIds().toSet()
        val newlyUnlocked = Achievement.ALL
            .filter { it.id !in unlocked && it.isUnlocked(stats) }
            .map { it.id }

        for (id in newlyUnlocked) {
            achievementRepository.unlock(id)
            val achievement = Achievement.ALL.find { it.id == id }
            if (achievement != null) {
                _uiState.update {
                    it.copy(
                        toastMessage = "${achievement.emoji} ${achievement.title} unlocked!",
                        toastEmoji = achievement.emoji,
                    )
                }
            }
            kotlinx.coroutines.delay(3000)
            _uiState.update { it.copy(toastMessage = null) }
        }
    }

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

            if (progress >= challenge.target) {
                _uiState.update {
                    it.copy(
                        toastMessage = "Daily goal complete! 🎉",
                        toastEmoji = "🎉",
                    )
                }
                kotlinx.coroutines.delay(3000)
                _uiState.update { it.copy(toastMessage = null) }
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

    private fun loadPhotos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val photos = withContext(Dispatchers.IO) {
                queryPhotos()
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
            MediaStore.Files.FileColumns.DATA,
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
