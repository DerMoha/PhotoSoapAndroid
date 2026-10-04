package com.photosoap.android.ui.review

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
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
import com.photosoap.android.PermissionChecker
import com.photosoap.android.MediaAccess
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
import kotlinx.serialization.Serializable
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
    private val permissionChecker: PermissionChecker,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()
    val hapticsEnabled = settingsRepository.hapticsEnabled

    private val statsMutex = Mutex()
    private val reviewedInSession = mutableSetOf<String>()
    private val json = Json { ignoreUnknownKeys = true }

    private var mediaObserver: ContentObserver? = null
    private var loadPhotosJob: Job? = null
    private var activeDeletionRequestId: String? = null
    private var activeDeletionItems: List<PendingDeletionItem> = emptyList()
    private val legacyDeletedItems = mutableListOf<PendingDeletionItem>()
    private var monthLoadJob: Job? = null
    private var isInitialized = false
    private var queuePersistenceJob: Job? = null
    private var restoredDeletionRequest: DeletionRequestJournal? = null
    private var deferredDeletionResult: Pair<Boolean, String?>? = null

    @Serializable
    private data class DeletionRequestJournal(
        val requestId: String,
        val itemIds: List<String>,
        val items: List<PendingDeletionItem> = emptyList(),
        val confirmedItemIds: List<String> = emptyList(),
        val legacyRetry: Boolean = false,
    )

    init {
        observeStats()
        registerContentObserver()
        viewModelScope.launch {
            loadSettings()
            launch {
                settingsRepository.useDeleteQueue.collect { enabled ->
                    _uiState.update { it.copy(useDeleteQueue = enabled) }
                }
            }
            resetSessionStats()
            initializeDailyChallenge()
            isInitialized = true
            deferredDeletionResult?.let { (success, requestId) ->
                deferredDeletionResult = null
                onDeletionRequestResult(success, requestId)
            }
            loadPhotos()
            loadFilterData()
        }
    }

    override fun onCleared() {
        super.onCleared()
        loadPhotosJob?.cancel()
        monthLoadJob?.cancel()
        mediaObserver?.let { context.contentResolver.unregisterContentObserver(it) }
    }

    fun onEvent(event: ReviewUiEvent) {
        if (_uiState.value.isDeleting && event in listOf(
                ReviewUiEvent.UndoLastDeletion, ReviewUiEvent.ClearQueue,
                ReviewUiEvent.StartOver, ReviewUiEvent.ConfirmDelete,
                ReviewUiEvent.OpenDeleteQueue, ReviewUiEvent.RequestDeleteConfirmation,
            )) return
        if (_uiState.value.isDeleting && event is ReviewUiEvent.RemoveFromQueue) return
        when (event) {
            ReviewUiEvent.RetryLoad -> onResume()
            is ReviewUiEvent.Swiped -> handleSwipe(event.direction, event.photoUri)
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

    private fun handleSwipe(direction: SwipeDirection, expectedUri: String?) {
        val state = _uiState.value
        if (state.isLoading || state.isDeleting || state.loadError != null) return
        val photo = state.currentPhoto ?: return
        if (expectedUri != null && photo.uri != expectedUri) return
        if (!reviewedInSession.add(photo.uri)) return

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

            }
        }

        advanceToNextPhoto()
        if (direction == SwipeDirection.DELETE && !_uiState.value.useDeleteQueue) {
            // Older Android versions may delete directly once access has been granted.
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) {
                _uiState.update { it.copy(showDeleteConfirmSheet = true) }
            } else viewModelScope.launch { executeDeletion() }
        }
    }

    private fun advanceToNextPhoto() {
        _uiState.update { state ->
            val nextIndex = state.currentIndex + 1
            state.copy(currentIndex = nextIndex, isReviewComplete = nextIndex >= state.photos.size)
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

    private suspend fun executeDeletion() {
        if (_uiState.value.isDeleting) return
        // Keep requests comfortably below Android's batch URI limit.
        val items = _uiState.value.pendingDeletions.take(1000)
        if (items.isEmpty()) return
        _uiState.update { it.copy(isDeleting = true, showDeleteConfirmSheet = false) }
        activeDeletionItems = items
        legacyDeletedItems.clear()
        try {
            queuePersistenceJob?.join()
            val requestId = beginDeletionRequest(items)
            when {
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R -> {
                    val request = MediaStore.createDeleteRequest(context.contentResolver, items.map(::canonicalMediaUri))
                    _uiState.update { it.copy(pendingDeleteIntentSender = request.intentSender) }
                }
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q -> {
                    executeLegacyDeletion(items, requestId)
                }
                else -> executeAndroid9Deletion(items, requestId)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation // Preserve the journal for recovery after process termination.
        } catch (error: Exception) {
            Log.e("ReviewViewModel", "Media deletion failed", error)
            endFailedDeletion(R.string.deletion_failed)
        }
    }

    @RequiresApi(android.os.Build.VERSION_CODES.Q)
    private suspend fun executeLegacyDeletion(items: List<PendingDeletionItem>, requestId: String) {
        for (item in items) {
            if (legacyDeletedItems.any { it.id == item.id }) continue
            try {
                val deleted = withContext(ioDispatcher) {
                    context.contentResolver.delete(canonicalMediaUri(item), null, null) > 0
                }
                if (deleted) legacyDeletedItems += item
            } catch (recoverable: android.app.RecoverableSecurityException) {
                settingsRepository.setPendingDeletionRequest(json.encodeToString(DeletionRequestJournal(
                    requestId, items.map { it.id }, items, legacyRetry = true,
                )))
                _uiState.update {
                    it.copy(
                        pendingDeleteIntentSender = recoverable.userAction.actionIntent.intentSender,
                        pendingLegacyDeleteRetry = true,
                    )
                }
                return
            } catch (_: SecurityException) {
                // Preserve inaccessible items in the queue.
            } catch (_: IllegalArgumentException) {
                // Preserve unavailable items rather than claiming deletion.
            }
        }
        if (legacyDeletedItems.isNotEmpty()) {
            completeConfirmedDeletions(legacyDeletedItems.toList(), requestId)
        } else endFailedDeletion(R.string.deletion_manual_required)
    }

    private suspend fun executeAndroid9Deletion(items: List<PendingDeletionItem>, requestId: String) {
        val deletedItems = withContext(ioDispatcher) {
            items.filter { item ->
                try {
                    context.contentResolver.delete(canonicalMediaUri(item), null, null) > 0
                } catch (_: SecurityException) { false }
                catch (_: IllegalArgumentException) { false }
            }
        }
        if (deletedItems.isNotEmpty()) completeConfirmedDeletions(deletedItems, requestId)
        else endFailedDeletion(R.string.deletion_manual_required)
    }

    /** Consume before launching so recomposition/rotation cannot launch the same sender twice. */
    fun currentDeletionRequestId(): String? = activeDeletionRequestId

    fun consumeDeletionIntentSender(): android.content.IntentSender? {
        val sender = _uiState.value.pendingDeleteIntentSender ?: return null
        _uiState.update { it.copy(pendingDeleteIntentSender = null) }
        return sender
    }

    fun onDeletionLaunchFailed() {
        viewModelScope.launch { endFailedDeletion(R.string.deletion_failed) }
    }

    fun onDeletionRequestResult(success: Boolean, requestId: String? = null) {
        if (!isInitialized) {
            deferredDeletionResult = success to requestId
            return
        }
        viewModelScope.launch {
            if (requestId != null && requestId != activeDeletionRequestId) {
                // ActivityResultRegistry restores results after process recreation. Match their
                // saved request identity, never apply a late result to a newer batch.
                val restored = restoredDeletionRequest
                if (activeDeletionRequestId != null || restored?.requestId != requestId) return@launch
                restoredDeletionRequest = null
                if (!success || restored.confirmedItemIds.isNotEmpty()) return@launch
                activeDeletionItems = restored.items.ifEmpty {
                    _uiState.value.pendingDeletions.filter { it.id in restored.itemIds }
                }
                if (activeDeletionItems.isEmpty()) return@launch
                if (restored.legacyRetry) {
                    activeDeletionItems = _uiState.value.pendingDeletions.filter { it.id in restored.itemIds }
                    if (activeDeletionItems.isEmpty()) return@launch
                    beginDeletionRequest(activeDeletionItems)
                } else activeDeletionRequestId = requestId
                _uiState.update { it.copy(isDeleting = true, pendingLegacyDeleteRetry = restored.legacyRetry) }
            }
            if (!_uiState.value.isDeleting || activeDeletionRequestId == null) return@launch
            if (_uiState.value.pendingLegacyDeleteRetry) {
                _uiState.update { it.copy(pendingDeleteIntentSender = null, pendingLegacyDeleteRetry = false) }
                if (success) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        try {
                            executeLegacyDeletion(activeDeletionItems, requireNotNull(activeDeletionRequestId))
                        } catch (cancellation: CancellationException) {
                            throw cancellation
                        } catch (error: Exception) {
                            Log.e("ReviewViewModel", "Legacy deletion retry failed", error)
                            endFailedDeletion(R.string.deletion_failed)
                        }
                    }
                } else if (legacyDeletedItems.isNotEmpty()) {
                    completeConfirmedDeletions(legacyDeletedItems.toList(), requireNotNull(activeDeletionRequestId))
                } else endFailedDeletion(R.string.deletion_cancelled)
            } else finishDeletion(success)
        }
    }

    fun onDeletionComplete(success: Boolean) = onDeletionRequestResult(success)

    private suspend fun finishDeletion(success: Boolean) {
        val requestId = activeDeletionRequestId ?: return
        if (success) {
            // MediaStore delivers RESULT_OK after the requested deletion finishes.
            // Use the frozen request, including selected-media grants whose rows are now invisible.
            completeConfirmedDeletions(activeDeletionItems, requestId)
        } else endFailedDeletion(R.string.deletion_cancelled)
    }

    private suspend fun endFailedDeletion(message: Int) {
        clearDeletionRequest()
        activeDeletionItems = emptyList()
        legacyDeletedItems.clear()
        _uiState.update {
            it.copy(isDeleting = false, pendingDeleteIntentSender = null, pendingLegacyDeleteRetry = false,
                showDeleteConfirmSheet = false, toastMessage = context.getString(message), toastEmoji = "↩️")
        }
    }

    private suspend fun completeConfirmedDeletions(
        items: List<PendingDeletionItem>,
        requestId: String,
    ) {
        if (items.isEmpty()) return
        val deletedIds = items.mapTo(mutableSetOf()) { it.id }
        // Save the confirmed receipt before updating counters or clearing the queue.
        settingsRepository.setPendingDeletionRequest(json.encodeToString(DeletionRequestJournal(
            requestId, activeDeletionItems.map { it.id }, activeDeletionItems, items.map { it.id },
        )))
        val remaining = _uiState.value.pendingDeletions.filterNot { it.id in deletedIds }
        recordConfirmedDeletions(requestId, items)
        metricsRepository.trackBatchDeletionOnce(requestId, items.size, items.sumOf { it.fileSize })
        settingsRepository.setPendingDeletions(json.encodeToString(remaining))
        clearDeletionRequest()
        activeDeletionItems = emptyList()
        legacyDeletedItems.clear()
        _uiState.update {
            it.copy(
                isDeleting = false,
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
                    context.resources.getQuantityString(
                        R.plurals.deletion_partial_success,
                        items.size,
                        items.size,
                        remaining.size,
                    )
                },
                toastEmoji = if (remaining.isEmpty()) "🧹" else "⚠️",
            )
        }
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
        val previous = queuePersistenceJob
        queuePersistenceJob = viewModelScope.launch {
            previous?.join()
            settingsRepository.setPendingDeletions(serialized)
        }
    }

    private suspend fun recordConfirmedDeletions(
        requestId: String,
        items: List<PendingDeletionItem>,
    ) {
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
            val applied = statsRepository.updateStatsForDeletionOnce(requestId, updated)
            val persisted = if (applied) updated else statsRepository.getStats() ?: updated
            _uiState.update { it.copy(stats = persisted) }
        }
        checkAchievements()
        checkDailyChallenge()
    }

    private suspend fun beginDeletionRequest(items: List<PendingDeletionItem>): String {
        val requestId = UUID.randomUUID().toString()
        val journal = DeletionRequestJournal(requestId, items.map { it.id }, items)
        settingsRepository.setPendingDeletionRequest(json.encodeToString(journal))
        activeDeletionRequestId = requestId
        return requestId
    }

    private suspend fun clearDeletionRequest() {
        settingsRepository.setPendingDeletionRequest("")
        activeDeletionRequestId = null
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
                        localizedAchievementTitle(it)
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
        val deletionRequest = runCatching {
            settingsRepository.pendingDeletionRequest.first()
                .takeIf(String::isNotBlank)
                ?.let { json.decodeFromString<DeletionRequestJournal>(it) }
        }.getOrNull()
        restoredDeletionRequest = deletionRequest
        val missingItems = withContext(ioDispatcher) {
            storedPendingDeletions.filter { mediaAvailability(it) == MediaAvailability.MISSING }
        }
        val recoveredDeletions = if (deletionRequest == null) emptyList() else {
            val snapshot = deletionRequest.items.ifEmpty { storedPendingDeletions }
            if (deletionRequest.confirmedItemIds.isNotEmpty()) {
                snapshot.filter { it.id in deletionRequest.confirmedItemIds }
            } else missingItems.filter { it.id in deletionRequest.itemIds }
        }
        val removedIds = (missingItems + recoveredDeletions).mapTo(mutableSetOf()) { it.id }
        val pendingDeletions = storedPendingDeletions.filterNot { it.id in removedIds }

        _uiState.update {
            it.copy(
                mediaKind = runCatching { MediaKind.valueOf(kind.uppercase()) }.getOrDefault(MediaKind.ALL),
                sortOrder = runCatching { SortOrder.valueOf(order.uppercase()) }.getOrDefault(SortOrder.NEWEST_FIRST),
                useDeleteQueue = useQueue,
                pendingDeletions = pendingDeletions,
            )
        }
        // Counters and metrics must be committed before removing their only historical queue data.
        if (deletionRequest != null) {
            activeDeletionRequestId = deletionRequest.requestId
            if (recoveredDeletions.isNotEmpty()) {
                recordConfirmedDeletions(deletionRequest.requestId, recoveredDeletions)
                metricsRepository.trackBatchDeletionOnce(
                    deletionRequest.requestId, recoveredDeletions.size,
                    recoveredDeletions.sumOf { it.fileSize },
                )
            }
        }
        if (removedIds.isNotEmpty()) {
            settingsRepository.setPendingDeletions(json.encodeToString(pendingDeletions))
        }
        if (deletionRequest != null) clearDeletionRequest()
    }

    private enum class MediaAvailability { PRESENT, MISSING, UNKNOWN }

    private fun mediaAvailability(item: PendingDeletionItem): MediaAvailability = try {
        val cursor = context.contentResolver.query(
            item.uri.toUri(), arrayOf(MediaStore.Files.FileColumns._ID), null, null, null,
        )
        if (cursor == null) MediaAvailability.UNKNOWN
        else cursor.use {
            if (it.moveToFirst()) MediaAvailability.PRESENT
            else if (permissionChecker.getMediaAccess() == MediaAccess.FULL) MediaAvailability.MISSING
            else MediaAvailability.UNKNOWN
        }
    } catch (_: SecurityException) {
        MediaAvailability.UNKNOWN
    } catch (_: IllegalArgumentException) {
        MediaAvailability.UNKNOWN
    }

    /**
     * MediaStore's delete-request API rejects URIs from the generic Files collection.
     * Older PhotoSoap versions persisted those URIs, so normalize both old queue entries
     * and current media before asking Android for deletion approval.
     */
    private fun canonicalMediaUri(item: PendingDeletionItem): Uri {
        val original = item.uri.toUri()
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
        monthLoadJob?.cancel()
        monthLoadJob = viewModelScope.launch {
            try {
                val months = withContext(ioDispatcher) { mediaMonths().filter { it.year == year } }
                if (_uiState.value.selectedYear == year) _uiState.update { it.copy(months = months) }
            } catch (cancellation: CancellationException) { throw cancellation }
            catch (_: Exception) { _uiState.update { it.copy(months = emptyList()) } }
        }
    }

    private fun deselectYear() {
        monthLoadJob?.cancel()
        _uiState.update { it.copy(selectedYear = null, months = emptyList()) }
    }

    private fun mediaMonths(): List<YearMonth> {
        val projection = arrayOf(MediaStore.Files.FileColumns.DATE_TAKEN, MediaStore.Files.FileColumns.DATE_ADDED)
        val selection = "(${MediaStore.Files.FileColumns.MEDIA_TYPE}=${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}" +
            " OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO})"
        val months = mutableSetOf<YearMonth>()
        context.contentResolver.query(MediaStore.Files.getContentUri("external"), projection, selection, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val taken = cursor.getLong(0)
                val date = if (taken > 0) taken else cursor.getLong(1) * 1000L
                if (date > 0) months += YearMonth.from(java.time.Instant.ofEpochMilli(date).atZone(java.time.ZoneId.systemDefault()))
            }
        }
        return months.sortedDescending()
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
        val years = mediaMonths().map { it.year }.distinct().sortedDescending()
        _uiState.update { it.copy(years = years) }
    }

    fun onResume() {
        if (!isInitialized) return
        refreshDailyValues()
        loadPhotos(backgroundRefresh = true)
        loadFilterData()
    }

    fun refreshDailyValues() {
        viewModelScope.launch {
            initializeDailyChallenge()
            val stats = statsRepository.getStats()
            _uiState.update { it.copy(todayReviewCount = dailyReviewCount(stats)) }
        }
    }

    private fun dailyReviewCount(stats: UserStats?): Int =
        if (stats?.todayDate == todayStartMillis()) stats.todayReviewCount else 0

    private fun registerContentObserver() {
        try {
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    loadPhotos(backgroundRefresh = true)
                    loadFilterData()
                }

                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    loadPhotos(backgroundRefresh = true)
                    loadFilterData()
                }
            }
            mediaObserver = observer
            val uri = MediaStore.Files.getContentUri("external")
            context.contentResolver.registerContentObserver(uri, true, observer)
        } catch (_: Exception) { }
    }

    private fun loadPhotos(backgroundRefresh: Boolean = false) {
        loadPhotosJob?.cancel()
        val queryState = _uiState.value
        loadPhotosJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = !backgroundRefresh || it.isLoading || it.currentPhoto == null, loadError = null) }

            val sessionUris = reviewedInSession.toSet()
            val photos = try {
                withContext(ioDispatcher) {
                    val reviewedUris = photoRepository.observeReviewedPhotoUris().first().toHashSet()
                    queryPhotos(queryState).filterNot { it.uri in reviewedUris || it.uri in sessionUris }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: SecurityException) {
                _uiState.update { it.copy(isLoading = false, loadError = ReviewLoadError.ACCESS_DENIED) }
                return@launch
            } catch (error: Exception) {
                runCatching { Log.e("ReviewViewModel", "Could not load media", error) }
                _uiState.update { it.copy(isLoading = false, loadError = ReviewLoadError.UNAVAILABLE) }
                return@launch
            }

            _uiState.update {
                val available = photos.filterNot { photo -> photo.uri in reviewedInSession }
                // Keep the in-flight review deck stable; append newly discovered media after it.
                val previousOrder = it.photos.drop(it.currentIndex).mapIndexed { index, photo -> photo.uri to index }.toMap()
                val refreshed = if (backgroundRefresh)
                    available.sortedBy { photo -> previousOrder[photo.uri] ?: Int.MAX_VALUE }
                else available
                it.copy(
                    photos = refreshed,
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

        }

        val photos = mutableListOf<Photo>()
        val cursor = context.contentResolver.query(uri, projection, selection, null, null)
            ?: throw IllegalStateException("Media provider returned no cursor")
        cursor.use { cursor ->
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

        val zone = java.time.ZoneId.systemDefault()
        val filtered = photos.filter { photo ->
            when (val filter = state.filter) {
                ReviewFilter.All -> true
                is ReviewFilter.Album -> photo.bucketId == filter.albumId
                is ReviewFilter.Year -> photo.effectiveDateMillis > 0 &&
                    java.time.Instant.ofEpochMilli(photo.effectiveDateMillis).atZone(zone).year == filter.year
                is ReviewFilter.Month -> photo.effectiveDateMillis > 0 &&
                    YearMonth.from(java.time.Instant.ofEpochMilli(photo.effectiveDateMillis).atZone(zone)) == YearMonth.of(filter.year, filter.month)
            }
        }
        return when (state.sortOrder) {
            SortOrder.NEWEST_FIRST -> filtered.sortedByDescending { it.effectiveDateMillis }
            SortOrder.OLDEST_FIRST -> filtered.sortedBy { it.effectiveDateMillis }
            SortOrder.SHUFFLED -> filtered.shuffled()
        }
    }

    private fun observeStats() {
        viewModelScope.launch {
            statsRepository.observeStats().collect { stats ->
                _uiState.update {
                    it.copy(
                        stats = stats,
                        todayReviewCount = dailyReviewCount(stats),
                    )
                }
            }
        }
    }
}
