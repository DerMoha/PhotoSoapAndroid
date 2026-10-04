package com.photosoap.android.ui.review

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import com.photosoap.android.util.HapticsController
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.photosoap.android.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    isLimitedAccess: Boolean = false,
    onManageAccess: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: ReviewViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val reviewScrollState = rememberScrollState()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsStateWithLifecycle(initialValue = false)
    val view = LocalView.current
    val haptics = remember(view, hapticsEnabled) { HapticsController(view, hapticsEnabled) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }
    LaunchedEffect(viewModel) {
        while (true) {
            kotlinx.coroutines.delay(60_000)
            viewModel.refreshDailyValues()
        }
    }
    var launchedDeletionRequestId by rememberSaveable { mutableStateOf<String?>(null) }
    val deletionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val requestId = launchedDeletionRequestId
        launchedDeletionRequestId = null
        viewModel.onDeletionRequestResult(result.resultCode == android.app.Activity.RESULT_OK, requestId)
    }

    LaunchedEffect(state.pendingDeleteIntentSender) {
        viewModel.consumeDeletionIntentSender()?.let { intentSender ->
            launchedDeletionRequestId = viewModel.currentDeletionRequestId()
            try {
                deletionLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            } catch (_: Exception) {
                launchedDeletionRequestId = null
                viewModel.onDeletionLaunchFailed()
            }
        }
    }

    LaunchedEffect(state.toastMessage) {
        if (state.toastMessage != null) {
            if (state.toastEmoji == "❌" || state.toastEmoji == "⚠️") haptics.notificationError()
            else haptics.notificationSuccess()
            kotlinx.coroutines.delay(3000)
            viewModel.onEvent(ReviewUiEvent.DismissToast)
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompactHeader(
                    todayReviewCount = state.todayReviewCount,
                    hasActiveFilter = state.filter !is com.photosoap.android.domain.model.ReviewFilter.All ||
                        state.mediaKind != com.photosoap.android.domain.model.MediaKind.ALL,
                    dailyChallengeProgress = state.dailyChallengeProgress,
                    dailyChallengeTarget = state.dailyChallengeTarget,
                    dailyChallengeType = state.dailyChallengeType,
                    onFilterClick = { viewModel.onEvent(ReviewUiEvent.OpenFilterSheet) },
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onNavigateToSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
                }
            }
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            if (isLimitedAccess) {
                LimitedAccessBanner(onManageAccess = onManageAccess)
            }
            Box(modifier = Modifier.weight(1f)) {
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
                            text = stringResource(R.string.review_loading),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            state.loadError != null -> {
                EmptyState(
                    title = stringResource(R.string.review_load_failed),
                    subtitle = stringResource(if (state.loadError == ReviewLoadError.ACCESS_DENIED)
                        R.string.review_access_required else R.string.review_load_failed_description),
                    action = {
                        TextButton(onClick = { viewModel.onEvent(ReviewUiEvent.RetryLoad) }) {
                            Text(stringResource(R.string.retry))
                        }
                        if (state.loadError == ReviewLoadError.ACCESS_DENIED) {
                            TextButton(onClick = onManageAccess) { Text(stringResource(R.string.settings_photo_access)) }
                        }
                    },
                )
            }

            state.isReviewComplete -> {
                ReviewCompleteContent(
                    pendingDeletionCount = state.pendingDeletions.size,
                    onStartOver = { viewModel.onEvent(ReviewUiEvent.StartOver) },
                    onReviewDeletions = { viewModel.onEvent(ReviewUiEvent.OpenDeleteQueue) },
                )
            }

            state.photos.isEmpty() -> {
                EmptyState(
                    title = stringResource(R.string.review_empty),
                    subtitle = stringResource(R.string.review_empty_description),
                    action = {
                        TextButton(onClick = { viewModel.onEvent(ReviewUiEvent.OpenFilterSheet) }) {
                            Text(stringResource(R.string.filter_browse))
                        }
                    },
                )
            }

            else -> {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val compactHeight = maxHeight < 400.dp
                Column(
                    modifier = Modifier.fillMaxSize().then(
                        if (compactHeight) Modifier.verticalScroll(reviewScrollState) else Modifier,
                    ),
                ) {


                    Box(
                        modifier = Modifier
                            .then(if (compactHeight) Modifier.height(320.dp) else Modifier.weight(1f))
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        state.currentPhoto?.let { photo ->
                            SwipeableCard(
                                modifier = Modifier.fillMaxSize(),
                                resetKey = photo.uri,
                                enabled = !state.isDeleting,
                                onSwiped = { direction ->
                                    haptics.impactMedium()
                                    viewModel.onEvent(ReviewUiEvent.Swiped(direction, photo.uri))
                                },
                                onTap = {
                                    viewModel.onEvent(ReviewUiEvent.TappedCard)
                                },
                                overlayContent = { direction ->
                                    if (direction == SwipeDirection.KEEP) {
                                        SwipeHintBadge(
                                            text = stringResource(R.string.review_keep),
                                            direction = direction,
                                            modifier = Modifier.align(Alignment.CenterStart),
                                        )
                                    } else if (direction == SwipeDirection.DELETE) {
                                        SwipeHintBadge(
                                            text = stringResource(R.string.review_delete),
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
                        text = pluralStringResource(
                            R.plurals.review_remaining,
                            state.photosRemaining,
                            state.photosRemaining,
                        ),
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
                            onClick = {
                                haptics.impactMedium()
                                viewModel.onEvent(ReviewUiEvent.Swiped(SwipeDirection.DELETE, state.currentPhoto?.uri))
                            },
                            enabled = !state.isDeleting,
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
                            Text(stringResource(R.string.review_delete))
                        }

                        Button(
                            onClick = {
                                haptics.impactLight()
                                viewModel.onEvent(ReviewUiEvent.Swiped(SwipeDirection.KEEP, state.currentPhoto?.uri))
                            },
                            enabled = !state.isDeleting,
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
                            Text(stringResource(R.string.review_keep))
                        }
                    }

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
            if (state.isDeleting) {
                Text(
                    text = stringResource(R.string.deletion_in_progress),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            if (state.hasPendingDeletions) {
                DeleteQueueTray(
                    enabled = !state.isDeleting,
                    itemCount = state.pendingDeletions.size,
                    totalFileSize = state.totalDeletionFileSize,
                    onUndo = {
                        if (!state.isDeleting) {
                            haptics.selection()
                            viewModel.onEvent(ReviewUiEvent.UndoLastDeletion)
                        }
                    },
                    onViewList = { viewModel.onEvent(ReviewUiEvent.OpenDeleteQueue) },
                )
            }
    }
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
            onDelete = { viewModel.onEvent(ReviewUiEvent.RequestDeleteConfirmation) },
            onDismiss = { viewModel.onEvent(ReviewUiEvent.DismissDeleteQueue) },
        )
    }

    if (state.showDeleteConfirmSheet) {
        DeleteBatchConfirmSheet(
            itemCount = minOf(state.pendingDeletions.size, 1000),
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
private fun LimitedAccessBanner(onManageAccess: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.limited_access_banner),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onManageAccess) {
                Text(stringResource(R.string.manage_access))
            }
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
    pendingDeletionCount: Int,
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
            text = stringResource(R.string.review_all_reviewed),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.review_all_reviewed_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onStartOver) {
            Text(stringResource(R.string.review_again))
        }
        if (pendingDeletionCount > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onReviewDeletions) {
                Text(
                    pluralStringResource(
                        R.plurals.review_delete_list_items,
                        pendingDeletionCount,
                        pendingDeletionCount,
                    )
                )
            }
        }
    }
}
