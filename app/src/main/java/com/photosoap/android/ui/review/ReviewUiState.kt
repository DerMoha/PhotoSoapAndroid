package com.photosoap.android.ui.review

import com.photosoap.android.domain.model.MediaKind
import com.photosoap.android.domain.model.PendingDeletionItem
import com.photosoap.android.domain.model.Photo
import com.photosoap.android.domain.model.ReviewFilter
import com.photosoap.android.domain.model.SortOrder
import com.photosoap.android.domain.model.SwipeDirection
import com.photosoap.android.domain.model.UserStats

data class ReviewUiState(
    val photos: List<Photo> = emptyList(),
    val currentIndex: Int = 0,
    val isLoading: Boolean = true,
    val isReviewComplete: Boolean = false,
    val mediaKind: MediaKind = MediaKind.ALL,
    val sortOrder: SortOrder = SortOrder.NEWEST_FIRST,
    val filter: ReviewFilter = ReviewFilter.All,
    val pendingDeletions: List<PendingDeletionItem> = emptyList(),
    val useDeleteQueue: Boolean = true,
    val stats: UserStats? = null,
    val todayReviewCount: Int = 0,
    val dailyChallengeProgress: Int = 0,
    val dailyChallengeTarget: Int = 20,
    val dailyChallengeType: String = "review",
    val toastMessage: String? = null,
    val toastEmoji: String = "",
    val showFilterSheet: Boolean = false,
    val showDeleteQueueSheet: Boolean = false,
    val showDeleteConfirmSheet: Boolean = false,
    val showPhotoPreview: Boolean = false,
    val previewPhoto: Photo? = null,
) {
    val currentPhoto: Photo?
        get() = if (currentIndex < photos.size) photos[currentIndex] else null

    val photosRemaining: Int
        get() = maxOf(0, photos.size - currentIndex)

    val totalDeletionFileSize: Long
        get() = pendingDeletions.sumOf { it.fileSize }
}

sealed interface ReviewUiEvent {
    data class Swiped(val direction: SwipeDirection) : ReviewUiEvent
    data object TappedCard : ReviewUiEvent
    data object UndoLastDeletion : ReviewUiEvent
    data object OpenDeleteQueue : ReviewUiEvent
    data object ConfirmDelete : ReviewUiEvent
    data object CancelDeleteConfirm : ReviewUiEvent
    data object DismissDeleteQueue : ReviewUiEvent
    data class RemoveFromQueue(val itemId: String) : ReviewUiEvent
    data object ClearQueue : ReviewUiEvent
    data class ChangeMediaKind(val kind: MediaKind) : ReviewUiEvent
    data class ChangeSortOrder(val order: SortOrder) : ReviewUiEvent
    data class ChangeFilter(val filter: ReviewFilter) : ReviewUiEvent
    data object OpenFilterSheet : ReviewUiEvent
    data object CloseFilterSheet : ReviewUiEvent
    data object OpenPhotoPreview : ReviewUiEvent
    data object ClosePhotoPreview : ReviewUiEvent
    data object StartOver : ReviewUiEvent
    data object DismissToast : ReviewUiEvent
    data object ToggleDeleteQueue : ReviewUiEvent
}
