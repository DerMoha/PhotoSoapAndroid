package com.photosoap.android.ui.review

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.annotation.RequiresApi
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
import com.photosoap.android.di.IoDispatcher
import com.photosoap.android.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    private val statsMutex = Mutex()
    private val reviewedInSession = mutableSetOf<String>()
    private val json = Json { ignoreUnknownKeys = true }

    private var mediaObserver: ContentObserver? = null
    private var loadPhotosJob: Job? = null

    init {
        observeStats()
        registerContentObserver()
        viewModelScope.launch {
            loadSettings()
            resetSessionStats()
            initializeDailyChallenge()
            loadPhotos()
            loadFilterData()
        }
    }

    override fun onCleared() {
        super.onCleared()
        loadPhotosJob?.cancel()
        mediaObserver?.let { context.contentResolver.unregisterContentObserver(it) }
    }

    fun onEvent(event: ReviewUiEvent) {
        when (event) {
            is ReviewUiEvent.Swiped -> handleSwipe(event.direction)
            ReviewUiEvent.TappedCard -> _uiState.update { it.copy(showPhotoPreview = true, previewPhoto = it.currentPhoto) }
            ReviewUiEvent.UndoLastDeletion -> undoLastDeletion()
            ReviewUiEvent.OpenDeleteQueue -> _uiState.update { it.copy(showDeleteQueueSheet = true) }
            ReviewUiEvent.RequestDeleteConfirmation -> _uiState.update {
                it.copy(showDeleteQueueSheet = false, showDeleteConfirmSheet = true)
            }
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

        when (direction) {
            SwipeDirection.KEEP -> {
                reviewInSession(photo.uri)
                advanceStats(kept = true)
                viewModelScope.launch { metricsRepository.trackKept() }
            }
            SwipeDirection.DELETE -> {
                reviewInSession(photo.uri)
                advanceStats()
                addToDeletionQueue(photo)
                if (!_uiState.value.useDeleteQueue) {
                    executeDeletion()
                }
            }
        }

        advanceToNextPhoto()
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

    private fun reviewInSession(uri: String) {
        reviewedInSession.add(uri)
        viewModelScope.launch {
            photoRepository.markReviewed(uri)
        }
    }

    private fun advanceStats(kept: Boolean = false) {
        viewModelScope.launch {
            statsMutex.withLock {
                val now = System.currentTimeMillis()
                val current = statsRepository.getStats() ?: statsRepository.createIfNeeded()
                val todayStart = java.time.LocalDate.now()
                    .atStartOfDay(java.time.ZoneId.systemDefault())
                    .toInstant().toEpochMilli()
                val normalized = normalizeDailyChallenge(current, todayStart)

                val isNewDay = normalized.todayDate != todayStart
                val newTodayCount = if (isNewDay) 1 else normalized.todayReviewCount + 1

                val newDayStreak = when {
                    normalized.lastReviewDate == null -> 1
                    else -> {
                        val lastReviewDay = java.time.Instant.ofEpochMilli(normalized.lastReviewDate)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                        val today = java.time.LocalDate.now(java.time.ZoneId.systemDefault())
                        val daysSince = java.time.temporal.ChronoUnit.DAYS.between(lastReviewDay, today)
                        if (daysSince <= 1L) normalized.dayStreak + (if (daysSince == 1L || isNewDay) 1 else 0)
                        else 1
                    }
                }
                val challengeDelta = if (
                    normalized.dailyChallengeType == DailyChallenge.ChallengeType.REVIEW.label ||
                    normalized.dailyChallengeType == DailyChallenge.ChallengeType.STREAK.label
                ) 1 else 0

                val updated = normalized.copy(
                    totalReviewed = normalized.totalReviewed + 1,
                    totalKept = if (kept) normalized.totalKept + 1 else normalized.totalKept,
                    sessionReviewCount = normalized.sessionReviewCount + 1,
                    currentStreak = normalized.currentStreak + 1,
                    bestStreak = maxOf(normalized.bestStreak, normalized.currentStreak + 1),
                    todayReviewCount = newTodayCount,
                    todayDate = todayStart,
                    bestDayReviewCount = maxOf(current.bestDayReviewCount, newTodayCount),
                    dayStreak = newDayStreak,
                    lastReviewDate = now,
                    dailyChallengeProgress = normalized.dailyChallengeProgress + challengeDelta,
                )

                statsRepository.updateStats(updated)
                _uiState.update {
                    it.copy(
                        stats = updated,
                        todayReviewCount = updated.todayReviewCount,
                    )
                }
            }
            checkAchievements()
            checkDailyChallenge()
        }
    }

    private suspend fun resetSessionStats() {
        statsMutex.withLock {
            val current = statsRepository.getStats() ?: statsRepository.createIfNeeded()
            if (current.sessionReviewCount == 0 && current.currentStreak == 0) return@withLock
            val reset = current.copy(
                sessionReviewCount = 0,
                currentStreak = 0,
            )
            statsRepository.updateStats(reset)
            _uiState.update { it.copy(stats = reset) }
        }
    }

    private suspend fun initializeDailyChallenge() {
        val stats = statsMutex.withLock {
            val current = statsRepository.getStats() ?: statsRepository.createIfNeeded()
            val todayStart = todayStartMillis()
            val normalized = normalizeDailyChallenge(current, todayStart)
            if (normalized != current) statsRepository.updateStats(normalized)
            normalized
        }
        dailyChallengeToastShown = stats.dailyChallengeProgress >= stats.dailyChallengeTarget
        updateDailyChallengeUi(stats)
    }

    private fun normalizeDailyChallenge(stats: UserStats, todayStart: Long): UserStats {
        if (stats.dailyChallengeDate == todayStart && stats.dailyChallengeTarget > 0) return stats
        val challenge = DailyChallenge.generate()
        return stats.copy(
            dailyChallengeProgress = 0,
            dailyChallengeTarget = challenge.target,
            dailyChallengeType = challenge.type.label,
            dailyChallengeDate = todayStart,
        )
    }

    private fun todayStartMillis(): Long = java.time.LocalDate.now()
        .atStartOfDay(java.time.ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    private fun addToDeletionQueue(photo: Photo) {
        _uiState.update { state ->
            val item = PendingDeletionItem(
                id = UUID.randomUUID().toString(),
                uri = photo.uri,
                displayName = photo.displayName,
                fileSize = photo.fileSize,
                queuedAt = System.currentTimeMillis(),
                mimeType = photo.mimeType,
            )
            state.copy(pendingDeletions = state.pendingDeletions + item)
        }
        persistPendingDeletions()
    }

    private fun undoLastDeletion() {
        val last = _uiState.value.pendingDeletions.lastOrNull() ?: return
        _uiState.update { it.copy(pendingDeletions = it.pendingDeletions.dropLast(1)) }
        persistPendingDeletions()
        markQueuedItemsAsKept(listOf(last))
    }

    private fun markQueuedItemsAsKept(items: List<PendingDeletionItem>) {
        if (items.isEmpty()) return
        viewModelScope.launch {
            statsMutex.withLock {
                val current = statsRepository.getStats() ?: return@launch
                val updated = current.copy(
                    totalKept = current.totalKept + items.size,
                )
                statsRepository.updateStats(updated)
                _uiState.update { it.copy(stats = updated) }
            }
            checkAchievements()
            metricsRepository.trackKept(items.size)
        }
    }

    private fun executeDeletion() {
        val items = _uiState.value.pendingDeletions
        if (items.isEmpty()) return

        val uris = items.map(::canonicalMediaUri)

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
                Log.e("ReviewViewModel", "Could not create Android media deletion request", e)
                _uiState.update {
                    it.copy(
                        toastMessage = context.getString(R.string.deletion_failed),
                        toastEmoji = "❌",
                        showDeleteConfirmSheet = false,
                    )
                }
            }
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            viewModelScope.launch { executeLegacyDeletion(items) }
        } else {
            viewModelScope.launch { executeAndroid9Deletion(items) }
        }
    }

    @RequiresApi(android.os.Build.VERSION_CODES.Q)
    private suspend fun executeLegacyDeletion(items: List<PendingDeletionItem>) {
        val deletedItems = mutableListOf<PendingDeletionItem>()
        for (item in items) {
            try {
                val deleted = withContext(ioDispatcher) {
                    context.contentResolver.delete(canonicalMediaUri(item), null, null) > 0
                }
                if (deleted) deletedItems += item
            } catch (recoverable: android.app.RecoverableSecurityException) {
                if (deletedItems.isNotEmpty()) completeConfirmedDeletions(deletedItems)
                _uiState.update {
                    it.copy(
                        pendingDeleteIntentSender = recoverable.userAction.actionIntent.intentSender,
                        pendingLegacyDeleteRetry = true,
                        showDeleteConfirmSheet = false,
                    )
                }
                return
            } catch (_: SecurityException) {
                // Keep this item in the queue and continue; successfully deleted items
                // are still accounted for exactly below.
            } catch (_: IllegalArgumentException) {
                // The media item disappeared between queueing and deletion.
            }
        }

        if (deletedItems.isNotEmpty()) {
            completeConfirmedDeletions(deletedItems)
        } else {
            _uiState.update {
                it.copy(
                    toastMessage = context.getString(R.string.deletion_manual_required),
                    toastEmoji = "⚠️",
                    showDeleteConfirmSheet = false,
                )
            }
        }
    }

    private suspend fun executeAndroid9Deletion(items: List<PendingDeletionItem>) {
        val deletedItems = withContext(ioDispatcher) {
            items.filter { item ->
                try {
                    context.contentResolver.delete(canonicalMediaUri(item), null, null) > 0
                } catch (_: SecurityException) {
                    false
                } catch (_: IllegalArgumentException) {
                    false
                }
            }
        }
        if (deletedItems.isNotEmpty()) {
            completeConfirmedDeletions(deletedItems)
        } else {
            _uiState.update {
                it.copy(
                    toastMessage = context.getString(R.string.deletion_manual_required),
                    toastEmoji = "⚠️",
                    showDeleteConfirmSheet = false,
                )
            }
        }
    }

    fun onDeletionRequestResult(success: Boolean) {
        if (_uiState.value.pendingLegacyDeleteRetry) {
            _uiState.update {
                it.copy(
                    pendingDeleteIntentSender = null,
                    pendingLegacyDeleteRetry = false,
                )
            }
            if (success) executeDeletion() else onDeletionComplete(false)
        } else {
            onDeletionComplete(success)
        }
    }

    fun onDeletionComplete(success: Boolean) {
        val items = _uiState.value.pendingDeletions
        if (success) {
            completeConfirmedDeletions(items)
        } else {
            _uiState.update {
                it.copy(
                    pendingDeleteIntentSender = null,
                    pendingLegacyDeleteRetry = false,
                    toastMessage = context.getString(R.string.deletion_cancelled),
                    toastEmoji = "↩️",
                )
            }
        }
    }

    private fun completeConfirmedDeletions(items: List<PendingDeletionItem>) {
        if (items.isEmpty()) return
        val deletedIds = items.mapTo(mutableSetOf()) { it.id }
        val remaining = _uiState.value.pendingDeletions.filterNot { it.id in deletedIds }
        viewModelScope.launch {
            recordConfirmedDeletions(items)
            metricsRepository.trackBatchDeletion(items.size, items.sumOf { it.fileSize })
        }
        _uiState.update {
            it.copy(
                pendingDeletions = remaining,
                    pendingDeleteIntentSender = null,
                    pendingLegacyDeleteRetry = false,
                showDeleteConfirmSheet = false,
                toastMessage = if (remaining.isEmpty()) {
                    context.resources.getQuantityString(
                        R.plurals.deletion_items_success,
                        items.size,
                        items.size,
                    )
                } else {
                    context.getString(
                        R.string.deletion_partial_success,
                        items.size,
                        remaining.size,
                    )
                },
                toastEmoji = if (remaining.isEmpty()) "🧹" else "⚠️",
            )
        }
        persistPendingDeletions()
    }

    private fun removeFromQueue(itemId: String) {
        val item = _uiState.value.pendingDeletions.find { it.id == itemId } ?: return
        _uiState.update { it.copy(pendingDeletions = it.pendingDeletions.filter { i -> i.id != itemId }) }
        persistPendingDeletions()
        markQueuedItemsAsKept(listOf(item))
    }

    private fun clearQueue() {
        val items = _uiState.value.pendingDeletions
        _uiState.update { it.copy(pendingDeletions = emptyList()) }
        persistPendingDeletions()
        markQueuedItemsAsKept(items)
    }

    private fun persistPendingDeletions() {
        val serialized = json.encodeToString(_uiState.value.pendingDeletions)
        viewModelScope.launch { settingsRepository.setPendingDeletions(serialized) }
    }

    private suspend fun recordConfirmedDeletions(items: List<PendingDeletionItem>) {
        if (items.isEmpty()) return
        statsMutex.withLock {
            val current = statsRepository.getStats() ?: statsRepository.createIfNeeded()
            val normalized = normalizeDailyChallenge(current, todayStartMillis())
            val challengeDelta = if (
                normalized.dailyChallengeType == DailyChallenge.ChallengeType.DELETE.label
            ) items.size else 0
            val updated = normalized.copy(
                totalDeleted = normalized.totalDeleted + items.size,
                storageFreed = normalized.storageFreed + items.sumOf { it.fileSize },
                dailyChallengeProgress = normalized.dailyChallengeProgress + challengeDelta,
            )
            statsRepository.updateStats(updated)
            _uiState.update { it.copy(stats = updated) }
        }
        checkAchievements()
        checkDailyChallenge()
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
            // Pending delete-list entries are still active decisions. Keep them out of
            // the restarted deck so they cannot be reviewed and counted a second time.
            _uiState.value.pendingDeletions.forEach { item ->
                photoRepository.markReviewed(item.uri)
                reviewedInSession.add(item.uri)
            }
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
                    Achievement.ALL.find { it.id == id }?.let {
                        "${it.iconName} ${localizedAchievementTitle(it)}"
                    }
                }
                val message = names.joinToString("\n") +
                    "\n${context.getString(R.string.achievement_unlocked)}"
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
            updateDailyChallengeUi(stats)

            if (stats.dailyChallengeProgress >= stats.dailyChallengeTarget && !dailyChallengeToastShown) {
                dailyChallengeToastShown = true
                _uiState.update {
                    it.copy(
                        toastMessage = context.getString(R.string.daily_goal_complete),
                        toastEmoji = "🎉",
                    )
                }
            }
        }
    }

    private fun updateDailyChallengeUi(stats: UserStats) {
        _uiState.update {
            it.copy(
                dailyChallengeProgress = stats.dailyChallengeProgress,
                dailyChallengeTarget = stats.dailyChallengeTarget,
                dailyChallengeType = stats.dailyChallengeType,
            )
        }
    }

    private fun localizedAchievementTitle(achievement: Achievement): String {
        val resource = when (achievement.id) {
            "first_steps" -> R.string.achievements_first_steps
            "spring_cleaning" -> R.string.achievements_spring_cleaning
            "memory_keeper" -> R.string.achievements_memory_keeper
            "streak_master" -> R.string.achievements_streak_master
            "daily_devotee" -> R.string.achievements_daily_devotee
            "storage_saver" -> R.string.achievements_storage_saver
            "century_club" -> R.string.achievements_century_club
            "photo_pro" -> R.string.achievements_photo_pro
            "decisive" -> R.string.achievements_decisive
            "cleanup_champion" -> R.string.achievements_cleanup_champion
            else -> return achievement.title
        }
        return context.getString(resource)
    }

    private suspend fun loadSettings() {
        val kind = settingsRepository.mediaKind.first()
        val order = settingsRepository.sortOrder.first()
        val useQueue = settingsRepository.useDeleteQueue.first()
        val storedPendingDeletions = runCatching {
            json.decodeFromString<List<PendingDeletionItem>>(
                settingsRepository.pendingDeletions.first(),
            )
        }.getOrDefault(emptyList())
        val pendingDeletions = withContext(ioDispatcher) {
            storedPendingDeletions.filter(::isMediaStillAvailable)
        }
        val prunedItems = storedPendingDeletions.filterNot { stored ->
            pendingDeletions.any { it.id == stored.id }
        }

        _uiState.update {
            it.copy(
                mediaKind = runCatching { MediaKind.valueOf(kind) }.getOrDefault(MediaKind.ALL),
                sortOrder = runCatching { SortOrder.valueOf(order) }.getOrDefault(SortOrder.NEWEST_FIRST),
                useDeleteQueue = useQueue,
                pendingDeletions = pendingDeletions,
            )
        }
        if (prunedItems.isNotEmpty()) {
            settingsRepository.setPendingDeletions(json.encodeToString(pendingDeletions))
            markQueuedItemsAsKept(prunedItems)
        }
    }

    private fun isMediaStillAvailable(item: PendingDeletionItem): Boolean = try {
        context.contentResolver.query(
            Uri.parse(item.uri),
            arrayOf(MediaStore.Files.FileColumns._ID),
            null,
            null,
            null,
        )?.use { it.moveToFirst() } == true
    } catch (_: SecurityException) {
        false
    } catch (_: IllegalArgumentException) {
        false
    }

    /**
     * MediaStore's delete-request API rejects URIs from the generic Files collection.
     * Older PhotoSoap versions persisted those URIs, so normalize both old queue entries
     * and current media before asking Android for deletion approval.
     */
    private fun canonicalMediaUri(item: PendingDeletionItem): Uri {
        val original = Uri.parse(item.uri)
        if (original.authority != MediaStore.AUTHORITY) return original
        if (original.pathSegments.getOrNull(1) == "images" ||
            original.pathSegments.getOrNull(1) == "video"
        ) return original

        val mimeType = item.mimeType.ifBlank {
            runCatching {
                context.contentResolver.query(
                    original,
                    arrayOf(MediaStore.Files.FileColumns.MIME_TYPE),
                    null,
                    null,
                    null,
                )?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0).orEmpty() else ""
                }.orEmpty()
            }.getOrDefault("")
        }
        val id = runCatching { ContentUris.parseId(original) }.getOrNull() ?: return original
        val collection = when {
            mimeType.startsWith("image/") -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            mimeType.startsWith("video/") -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            else -> return original
        }
        return ContentUris.withAppendedId(collection, id)
    }

    private fun loadFilterData() {
        viewModelScope.launch {
            try {
                withContext(ioDispatcher) {
                    loadAlbums()
                    loadYears()
                }
            } catch (_: Exception) { }
        }
    }

    private fun selectYear(year: Int) {
        _uiState.update { it.copy(selectedYear = year, months = emptyList()) }
        viewModelScope.launch {
            withContext(ioDispatcher) {
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
        val albumCounts = linkedMapOf<Long, Pair<String, Int>>()

        context.contentResolver.query(
            uri, projection, selection, null,
            "${MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME} ASC"
        )?.use { cursor ->
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val bucketId = cursor.getLong(bucketIdCol)
                val name = cursor.getString(bucketNameCol) ?: "Unknown"
                val current = albumCounts[bucketId]
                albumCounts[bucketId] = name to ((current?.second ?: 0) + 1)
            }
        }
        val albums = albumCounts.map { (id, value) ->
            AlbumInfo(id = id, name = value.first, count = value.second)
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
        loadPhotosJob?.cancel()
        val queryState = _uiState.value
        loadPhotosJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val photos = try {
                withContext(ioDispatcher) {
                    val reviewedUris = photoRepository.observeReviewedPhotoUris().first().toHashSet()
                    queryPhotos(queryState).filterNot { it.uri in reviewedUris }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
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

    private fun queryPhotos(state: ReviewUiState): List<Photo> {
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
                val mimeType = cursor.getString(mimeCol) ?: "image/jpeg"
                val contentUri = ContentUris.withAppendedId(
                    if (mimeType.startsWith("video/")) {
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    } else {
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    },
                    id,
                )
                photos.add(
                    Photo(
                        id = id,
                        uri = contentUri.toString(),
                        displayName = cursor.getString(nameCol) ?: "Unknown",
                        mimeType = mimeType,
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
