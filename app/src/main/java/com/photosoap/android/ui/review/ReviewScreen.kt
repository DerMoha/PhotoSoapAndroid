package com.photosoap.android.ui.review

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.photosoap.android.domain.model.SwipeDirection
import com.photosoap.android.ui.components.DeleteQueueTray
import com.photosoap.android.ui.components.EmptyState
import com.photosoap.android.ui.components.PhotoCardContent
import com.photosoap.android.ui.components.SwipeableCard
import com.photosoap.android.ui.components.ToastOverlay
import com.photosoap.android.ui.review.components.CompactHeader
import com.photosoap.android.ui.review.components.FilterSheet
import com.photosoap.android.ui.review.components.DeleteQueueSheet
import com.photosoap.android.ui.review.components.DeleteBatchConfirmSheet
import com.photosoap.android.ui.review.components.PhotoPreviewSheet
import com.photosoap.android.ui.theme.AppColors

@Composable
fun ReviewScreen(
    viewModel: ReviewViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val deletionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.onDeletionComplete(result.resultCode == android.app.Activity.RESULT_OK)
    }

    LaunchedEffect(state.pendingDeleteIntentSender) {
        state.pendingDeleteIntentSender?.let { intentSender ->
            deletionLauncher.launch(
                IntentSenderRequest.Builder(intentSender).build()
            )
        }
    }

    LaunchedEffect(state.toastMessage) {
        if (state.toastMessage != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.onEvent(ReviewUiEvent.DismissToast)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading your library...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            state.isReviewComplete -> {
                ReviewCompleteContent(
                    stats = state.stats,
                    pendingDeletionCount = state.pendingDeletions.size,
                    totalFileSize = state.totalDeletionFileSize,
                    onStartOver = { viewModel.onEvent(ReviewUiEvent.StartOver) },
                    onReviewDeletions = { viewModel.onEvent(ReviewUiEvent.OpenDeleteQueue) },
                )
            }

            state.photos.isEmpty() -> {
                EmptyState(
                    title = "No photos to review",
                    subtitle = "Your library is empty or all photos have been reviewed.",
                )
            }

            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    CompactHeader(
                        todayReviewCount = state.todayReviewCount,
                        hasActiveFilter = state.filter !is com.photosoap.android.domain.model.ReviewFilter.All ||
                                state.mediaKind != com.photosoap.android.domain.model.MediaKind.ALL,
                        dailyChallengeProgress = state.dailyChallengeProgress,
                        dailyChallengeTarget = state.dailyChallengeTarget,
                        dailyChallengeType = state.dailyChallengeType,
                        onFilterClick = { viewModel.onEvent(ReviewUiEvent.OpenFilterSheet) },
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        state.currentPhoto?.let { photo ->
                            SwipeableCard(
                                modifier = Modifier.fillMaxWidth(),
                                resetKey = photo.uri,
                                enabled = true,
                                onSwiped = { direction ->
                                    viewModel.onEvent(ReviewUiEvent.Swiped(direction))
                                },
                                onTap = {
                                    viewModel.onEvent(ReviewUiEvent.TappedCard)
                                },
                                overlayContent = { direction ->
                                    if (direction == SwipeDirection.KEEP) {
                                        SwipeHintBadge(
                                            text = "Keep",
                                            direction = direction,
                                            modifier = Modifier.align(Alignment.CenterStart),
                                        )
                                    } else if (direction == SwipeDirection.DELETE) {
                                        SwipeHintBadge(
                                            text = "Delete",
                                            direction = direction,
                                            modifier = Modifier.align(Alignment.CenterEnd),
                                        )
                                    }
                                },
                            ) {
                                PhotoCardContent(photo = photo)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${state.photosRemaining} remaining",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        textAlign = TextAlign.Center,
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        FilledTonalButton(
                            onClick = { viewModel.onEvent(ReviewUiEvent.Swiped(SwipeDirection.DELETE)) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = AppColors.DeleteContainer,
                                contentColor = AppColors.OnDeleteContainer,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Delete")
                        }

                        Button(
                            onClick = { viewModel.onEvent(ReviewUiEvent.Swiped(SwipeDirection.KEEP)) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppColors.Keep,
                                contentColor = AppColors.OnKeep,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Keep")
                        }
                    }

                    if (state.hasPendingDeletions) {
                        DeleteQueueTray(
                            itemCount = state.pendingDeletions.size,
                            totalFileSize = state.totalDeletionFileSize,
                            onUndo = { viewModel.onEvent(ReviewUiEvent.UndoLastDeletion) },
                            onViewList = { viewModel.onEvent(ReviewUiEvent.OpenDeleteQueue) },
                        )
                    }
                }
            }
        }

        ToastOverlay(
            visible = state.toastMessage != null,
            message = state.toastMessage ?: "",
            emoji = state.toastEmoji,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }

    if (state.showFilterSheet) {
        FilterSheet(
            selectedKind = state.mediaKind,
            selectedSort = state.sortOrder,
            selectedFilter = state.filter,
            selectedYear = state.selectedYear,
            albums = state.albums,
            years = state.years,
            months = state.months,
            onKindSelected = { viewModel.onEvent(ReviewUiEvent.ChangeMediaKind(it)) },
            onSortSelected = { viewModel.onEvent(ReviewUiEvent.ChangeSortOrder(it)) },
            onFilterSelected = { viewModel.onEvent(ReviewUiEvent.ChangeFilter(it)) },
            onYearSelected = { viewModel.onEvent(ReviewUiEvent.SelectYear(it)) },
            onDeselectYear = { viewModel.onEvent(ReviewUiEvent.DeselectYear) },
            onDismiss = { viewModel.onEvent(ReviewUiEvent.CloseFilterSheet) },
        )
    }

    if (state.showDeleteQueueSheet) {
        DeleteQueueSheet(
            items = state.pendingDeletions,
            onRemove = { viewModel.onEvent(ReviewUiEvent.RemoveFromQueue(it)) },
            onClearAll = { viewModel.onEvent(ReviewUiEvent.ClearQueue) },
            onDelete = { viewModel.onEvent(ReviewUiEvent.ConfirmDelete) },
            onDismiss = { viewModel.onEvent(ReviewUiEvent.DismissDeleteQueue) },
        )
    }

    if (state.showDeleteConfirmSheet) {
        DeleteBatchConfirmSheet(
            itemCount = state.pendingDeletions.size,
            onConfirm = { viewModel.onEvent(ReviewUiEvent.ConfirmDelete) },
            onCancel = { viewModel.onEvent(ReviewUiEvent.CancelDeleteConfirm) },
        )
    }

    if (state.showPhotoPreview && state.previewPhoto != null) {
        state.previewPhoto?.let { photo ->
            PhotoPreviewSheet(
                photo = photo,
                onDismiss = { viewModel.onEvent(ReviewUiEvent.ClosePhotoPreview) },
            )
        }
    }
}

@Composable
private fun SwipeHintBadge(
    text: String,
    direction: SwipeDirection,
    modifier: Modifier = Modifier,
) {
    val isKeep = direction == SwipeDirection.KEEP
    Surface(
        modifier = modifier.padding(24.dp),
        shape = CircleShape,
        color = if (isKeep) AppColors.KeepContainer else AppColors.DeleteContainer,
        contentColor = if (isKeep) AppColors.OnKeepContainer else AppColors.OnDeleteContainer,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = if (isKeep) Icons.Filled.Check else Icons.Filled.Delete,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ReviewCompleteContent(
    stats: com.photosoap.android.domain.model.UserStats?,
    pendingDeletionCount: Int,
    totalFileSize: Long,
    onStartOver: () -> Unit,
    onReviewDeletions: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "🎉",
            style = MaterialTheme.typography.displayLarge,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "All Reviewed!",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "You've reviewed all photos in this view.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onStartOver) {
            Text("Review Again")
        }
        if (pendingDeletionCount > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onReviewDeletions) {
                Text("Review $pendingDeletionCount Items in Delete List")
            }
        }
    }
}
