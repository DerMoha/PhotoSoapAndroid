package com.photosoap.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photosoap.domain.model.DailyChallenge
import com.photosoap.domain.model.PendingDeletionItem
import com.photosoap.domain.model.Photo
import com.photosoap.domain.model.PhotoFilter
import com.photosoap.domain.model.ReviewMediaKind
import com.photosoap.domain.model.SwipeDirection
import com.photosoap.domain.repository.AchievementRepository
import com.photosoap.domain.repository.PhotoRepository
import com.photosoap.domain.repository.SettingsRepository
import com.photosoap.domain.repository.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReviewUiState(
    val photos: List<Photo> = emptyList(),
    val currentIndex: Int = 0,
    val isLoading: Boolean = true,
    val isAllReviewed: Boolean = false,
    val mediaKind: ReviewMediaKind = ReviewMediaKind.All,
    val filter: PhotoFilter = PhotoFilter.All,
    val sortNewestFirst: Boolean = true,
    val deleteQueue: List<PendingDeletionItem> = emptyList(),
    val useDeleteList: Boolean = true,
    val dailyChallenge: DailyChallenge = DailyChallenge.generateForToday(),
    val showFilterSheet: Boolean = false,
    val showDeleteQueueSheet: Boolean = false,
    val showDeleteExplainerSheet: Boolean = false,
    val newlyUnlockedAchievement: String? = null,
    val streakMilestone: Int? = null,
    val dailyGoalComplete: Boolean = false,
    val reviewedCount: Int = 0,
    val deletedCount: Int = 0,
    val keptCount: Int = 0,
    val swipedPhotoId: Long? = null,
    val lastSwipeDirection: SwipeDirection? = null,
    val pendingDeleteIntentSender: android.content.IntentSender? = null,
)

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val statsRepository: StatsRepository,
    private val settingsRepository: SettingsRepository,
    private val achievementRepository: AchievementRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        loadPhotos()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val useDeleteList = settingsRepository.isUseDeleteList()
            _uiState.update { it.copy(useDeleteList = useDeleteList) }
        }
    }

    fun loadPhotos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val photos = photoRepository.loadPhotos(
                    filter = _uiState.value.filter,
                    mediaKind = _uiState.value.mediaKind,
                    sortNewestFirst = _uiState.value.sortNewestFirst,
                )
                _uiState.update {
                    it.copy(
                        photos = photos,
                        isLoading = false,
                        isAllReviewed = photos.isEmpty(),
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onSwipe(direction: SwipeDirection) {
        val state = _uiState.value
        val currentPhoto = state.photos.getOrNull(state.currentIndex) ?: return

        viewModelScope.launch {
            when (direction) {
                SwipeDirection.Delete -> {
                    statsRepository.incrementDeleted(currentPhoto.fileSize)

                    if (state.useDeleteList) {
                        val item = PendingDeletionItem(currentPhoto)
                        _uiState.update {
                            it.copy(
                                deleteQueue = it.deleteQueue + item,
                                deletedCount = it.deletedCount + 1,
                                swipedPhotoId = currentPhoto.id,
                                lastSwipeDirection = direction,
                            )
                        }
                    } else {
                        // Immediate deletion
                        photoRepository.deletePhotos(listOf(currentPhoto.id))
                        _uiState.update {
                            it.copy(
                                deletedCount = it.deletedCount + 1,
                                swipedPhotoId = currentPhoto.id,
                                lastSwipeDirection = direction,
                            )
                        }
                    }
                }
                SwipeDirection.Keep -> {
                    statsRepository.incrementKept()
                    _uiState.update {
                        it.copy(
                            keptCount = it.keptCount + 1,
                            swipedPhotoId = currentPhoto.id,
                            lastSwipeDirection = direction,
                        )
                    }
                }
            }

            advanceToNext()
        }
    }

    fun advanceToNext() {
        val state = _uiState.value
        val nextIndex = state.currentIndex + 1

        if (nextIndex >= state.photos.size) {
            _uiState.update { it.copy(isAllReviewed = true) }
            viewModelScope.launch {
                statsRepository.incrementReviewed()
                checkAchievements()
            }
        } else {
            _uiState.update {
                it.copy(
                    currentIndex = nextIndex,
                    reviewedCount = it.reviewedCount + 1,
                    swipedPhotoId = null,
                    lastSwipeDirection = null,
                )
            }
            viewModelScope.launch {
                statsRepository.incrementReviewed()
                statsRepository.updateTodayReviewCount(state.todayReviewCount + 1)
                updateDailyChallenge()
                checkAchievements()
            }
        }
    }

    fun undoLastSwipe() {
        val state = _uiState.value

        viewModelScope.launch {
            when (state.lastSwipeDirection) {
                SwipeDirection.Delete -> {
                    if (state.useDeleteList && state.deleteQueue.isNotEmpty()) {
                        _uiState.update {
                            it.copy(
                                deleteQueue = it.deleteQueue.dropLast(1),
                                deletedCount = (it.deletedCount - 1).coerceAtLeast(0),
                            )
                        }
                    }
                    // Stats rollback could go here
                }
                SwipeDirection.Keep -> {
                    _uiState.update {
                        it.copy(
                            keptCount = (it.keptCount - 1).coerceAtLeast(0),
                        )
                    }
                }
                null -> {}
            }

            if (state.currentIndex > 0) {
                _uiState.update {
                    it.copy(
                        currentIndex = it.currentIndex - 1,
                        swipedPhotoId = null,
                        lastSwipeDirection = null,
                        isAllReviewed = false,
                    )
                }
            }
        }
    }

    fun clearDeleteQueue() {
        _uiState.update { it.copy(deleteQueue = emptyList()) }
    }

    fun removeFromQueue(item: PendingDeletionItem) {
        _uiState.update {
            it.copy(deleteQueue = it.deleteQueue.filter { q -> q.photo.id != item.photo.id })
        }
    }

    fun deleteAllQueued() {
        val state = _uiState.value
        if (state.deleteQueue.isEmpty()) return

        viewModelScope.launch {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    val uris = state.deleteQueue.map { item ->
                        android.content.ContentUris.withAppendedId(
                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            item.photo.id
                        )
                    }
                    // We need a ContentResolver reference — this is a simplification
                    // In production, inject ContentResolver or use an IntentSender approach
                }
                _uiState.update { it.copy(deleteQueue = emptyList(), showDeleteExplainerSheet = false) }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun setMediaKind(kind: ReviewMediaKind) {
        _uiState.update { it.copy(mediaKind = kind) }
        loadPhotos()
    }

    fun setFilter(filter: PhotoFilter) {
        _uiState.update { it.copy(filter = filter) }
        loadPhotos()
    }

    fun setSortOrder(newestFirst: Boolean) {
        _uiState.update { it.copy(sortNewestFirst = newestFirst) }
        loadPhotos()
    }

    fun toggleFilterSheet() {
        _uiState.update { it.copy(showFilterSheet = !it.showFilterSheet) }
    }

    fun toggleDeleteQueueSheet() {
        _uiState.update { it.copy(showDeleteQueueSheet = !it.showDeleteQueueSheet) }
    }

    fun toggleDeleteExplainerSheet() {
        _uiState.update { it.copy(showDeleteExplainerSheet = !it.showDeleteExplainerSheet) }
    }

    fun dismissAchievement() {
        _uiState.update { it.copy(newlyUnlockedAchievement = null) }
    }

    fun dismissStreakMilestone() {
        _uiState.update { it.copy(streakMilestone = null) }
    }

    fun dismissDailyGoal() {
        _uiState.update { it.copy(dailyGoalComplete = false) }
    }

    private suspend fun updateDailyChallenge() {
        val stats = statsRepository.getStats()
        val challenge = _uiState.value.dailyChallenge

        val newProgress = when (challenge.type) {
            DailyChallenge.ChallengeType.Review -> stats.todayReviewCount
            DailyChallenge.ChallengeType.Delete -> stats.totalDeleted
            DailyChallenge.ChallengeType.Streak -> stats.currentStreak
        }

        statsRepository.updateDailyChallengeProgress(newProgress)

        _uiState.update {
            it.copy(
                dailyChallenge = challenge.copy(progress = newProgress),
                dailyGoalComplete = newProgress >= challenge.target,
            )
        }
    }

    private suspend fun checkAchievements() {
        val stats = statsRepository.getStats()

        val conditions = mapOf(
            "first_steps" to (stats.totalReviewed >= 50),
            "spring_cleaning" to (stats.totalDeleted >= 200),
            "memory_keeper" to (stats.totalKept >= 500),
            "streak_master" to (stats.bestStreak >= 100),
            "daily_devotee" to (stats.dayStreak >= 14),
            "storage_saver" to (stats.storageFreed >= 5L * 1024 * 1024 * 1024),
            "century_club" to (stats.sessionReviewCount >= 500),
            "photo_pro" to (stats.totalReviewed >= 5000),
            "decisive" to (stats.currentStreak >= 200),
            "cleanup_champion" to (stats.totalDeleted >= 2000),
        )

        for ((id, condition) in conditions) {
            if (condition && !achievementRepository.isUnlocked(id)) {
                achievementRepository.unlockAchievement(id)
                _uiState.update { it.copy(newlyUnlockedAchievement = id) }
            }
        }

        if (stats.currentStreak > 0 && stats.currentStreak % 25 == 0) {
            _uiState.update { it.copy(streakMilestone = stats.currentStreak) }
        }
    }
}
