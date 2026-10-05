package com.photosoap.android.ui.review

import com.photosoap.android.domain.model.ReviewFilter

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.onSizeChanged
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
import com.photosoap.android.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ReviewScreen(
    isLimitedAccess: Boolean = false,
    onManageAccess: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: ReviewViewModel = hiltViewModel(),
) {
    var showRestartConfirmation by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var previewHintShownThisVisit by remember { mutableStateOf(false) }
    var showPreviewHint by remember { mutableStateOf(false) }
    LaunchedEffect(state.previewHintSeen, state.currentPhoto != null) {
        showPreviewHint = !state.previewHintSeen && !previewHintShownThisVisit && state.currentPhoto != null
        if (showPreviewHint) { previewHintShownThisVisit = true; kotlinx.coroutines.delay(3500); showPreviewHint = false }
    }
    var lastDecision by remember { mutableStateOf<Pair<String, SwipeDirection>?>(null) }
    LaunchedEffect(lastDecision) {
        if (lastDecision != null) { kotlinx.coroutines.delay(1400); lastDecision = null }
    }
    val swipeController = remember { com.photosoap.android.ui.components.SwipeCardController() }
    val reviewScrollState = rememberScrollState()
    var cardSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val imageLoader = remember(context) { coil3.SingletonImageLoader.get(context) }
    val upcoming = remember(state.photos, state.currentIndex) {
        (1..2).mapNotNull { state.photos.getOrNull(state.currentIndex + it) }
    }
    androidx.compose.runtime.DisposableEffect(upcoming.map { it.uri }, cardSize) {
        val requests = if (cardSize.width > 0 && cardSize.height > 0) upcoming.map {
            imageLoader.enqueue(com.photosoap.android.ui.components.reviewImageRequest(context, it, cardSize))
        } else emptyList()
        onDispose { requests.forEach { it.dispose() } }
    }
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
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompactHeader(
                    hasActiveFilter = state.filter !is ReviewFilter.All ||
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
                        LoadingIndicator()
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
                    state = state,
                    isLimitedAccess = isLimitedAccess,
                    onManageAccess = onManageAccess,
                    onChangeFilter = { viewModel.onEvent(ReviewUiEvent.OpenFilterSheet) },
                    pendingDeletionCount = state.pendingDeletions.size,
                    onStartOver = { showRestartConfirmation = true },
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
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .onSizeChanged { cardSize = it },
                        contentAlignment = Alignment.Center,
                    ) {
                        state.currentPhoto?.let { photo ->
                            androidx.compose.runtime.key(photo.uri) {
                            SwipeableCard(
                                modifier = Modifier.fillMaxSize(),
                                resetKey = photo.uri,
                                enabled = !state.isDeleting,
                                controller = swipeController,
                                onThreshold = { haptics.swipeThreshold() },
                                onCommit = { showPreviewHint = false; lastDecision = null; haptics.swipeConfirmed() },
                                nextContent = upcoming.firstOrNull()?.let { next ->
                                    { PhotoCardContent(next, imageRequest = remember(next.uri, cardSize) {
                                        if (cardSize.width > 0 && cardSize.height > 0)
                                            com.photosoap.android.ui.components.reviewImageRequest(context, next, cardSize)
                                        else null
                                    }) }
                                },
                                onSwiped = { direction ->
                                    lastDecision = photo.uri to direction
                                    viewModel.onEvent(ReviewUiEvent.Swiped(direction, photo.uri))
                                },
                                onTap = {
                                    viewModel.onEvent(ReviewUiEvent.TappedCard)
                                },
                                overlayContent = { direction, progress ->
                                    if (direction != null) {
                                        Box(Modifier.fillMaxSize().background(
                                            (if (direction == SwipeDirection.KEEP) MaterialTheme.colorScheme.primary
                                             else MaterialTheme.colorScheme.error).copy(alpha = kotlin.math.abs(progress) * 0.24f)
                                        ))
                                    }
                                    if (direction == SwipeDirection.KEEP) {
                                        SwipeHintBadge(
                                            text = stringResource(R.string.review_keep),
                                            direction = direction,
                                            modifier = Modifier.align(Alignment.TopStart).graphicsLayer {
                                                alpha = kotlin.math.abs(progress).coerceIn(0f, 1f)
                                                scaleX = 0.85f + alpha * 0.15f; scaleY = scaleX
                                            },
                                        )
                                    } else if (direction == SwipeDirection.DELETE) {
                                        SwipeHintBadge(
                                            text = stringResource(R.string.review_delete),
                                            direction = direction,
                                            modifier = Modifier.align(Alignment.TopEnd).graphicsLayer {
                                                alpha = kotlin.math.abs(progress).coerceIn(0f, 1f)
                                                scaleX = 0.85f + alpha * 0.15f; scaleY = scaleX
                                            },
                                        )
                                    }
                                },
                            ) {
                                PhotoCardContent(
                                    photo = photo,
                                    remainingText = pluralStringResource(R.plurals.review_remaining, state.photosRemaining, state.photosRemaining),
                                    imageRequest = remember(photo.uri, cardSize) {
                                        if (cardSize.width > 0 && cardSize.height > 0)
                                            com.photosoap.android.ui.components.reviewImageRequest(context, photo, cardSize)
                                        else null
                                    },
                                )
                            }
                            }
                        }
                        val feedback = when (lastDecision?.second) {
                            SwipeDirection.KEEP -> stringResource(R.string.review_kept_feedback)
                            SwipeDirection.DELETE -> stringResource(if (state.useDeleteQueue) R.string.review_queued_feedback else R.string.review_marked_feedback)
                            null -> null
                        }
                        if (feedback != null || showPreviewHint) {
                            Surface(
                                modifier = Modifier.align(Alignment.TopCenter).padding(12.dp),
                                shape = MaterialTheme.shapes.extraLarge,
                                color = MaterialTheme.colorScheme.inverseSurface,
                                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                            ) {
                                Text(
                                    feedback ?: stringResource(R.string.review_preview_hint),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }

                    com.photosoap.android.ui.components.ReviewActionDock(
                        pendingCount = state.pendingDeletions.size,
                        enabled = !state.isDeleting && !swipeController.busy,
                        onDelete = { swipeController.swipe(SwipeDirection.DELETE) },
                        onKeep = { swipeController.swipe(SwipeDirection.KEEP) },
                        onUndo = {
                            haptics.selection()
                            viewModel.onEvent(ReviewUiEvent.UndoLastDeletion)
                        },
                        onOpenList = { viewModel.onEvent(ReviewUiEvent.OpenDeleteQueue) },
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
            if (state.isDeleting) {
                Text(
                    text = stringResource(R.string.deletion_in_progress),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            if (state.hasPendingDeletions && (state.isLoading || state.isReviewComplete || state.photos.isEmpty() || state.loadError != null)) {
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

    if (showRestartConfirmation) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showRestartConfirmation = false },
            title = { Text(stringResource(R.string.review_restart_title)) },
            text = {
                Column {
                    Text(stringResource(if (state.hasPendingDeletions) R.string.review_restart_queue_help else R.string.review_restart_help))
                    if (state.hasPendingDeletions) {
                        TextButton(onClick = {
                            showRestartConfirmation = false
                            viewModel.onEvent(ReviewUiEvent.OpenDeleteQueue)
                        }) { Text(stringResource(R.string.review_review_delete_list)) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = {
                showRestartConfirmation = false
                viewModel.onEvent(ReviewUiEvent.StartOver)
            }) { Text(stringResource(R.string.review_again)) } },
            dismissButton = { TextButton(onClick = { showRestartConfirmation = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (state.showFilterSheet) {
        FilterSheet(
            smartCounts = state.smartCounts,
            hideFavorites = state.hideFavorites,
            onHideFavoritesChange = { viewModel.onEvent(ReviewUiEvent.ChangeHideFavorites(it)) },
            progress = state.calendarProgress,
            isLoading = state.filterLoading,
            hasError = state.filterError,
            onRetry = { viewModel.onEvent(ReviewUiEvent.RetryFilters) },
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
        color = if (isKeep) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
        contentColor = if (isKeep) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
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
                modifier = Modifier.size(32.dp),
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
    state: ReviewUiState,
    isLimitedAccess: Boolean,
    onManageAccess: () -> Unit,
    onChangeFilter: () -> Unit,
    pendingDeletionCount: Int,
    onStartOver: () -> Unit,
    onReviewDeletions: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
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
            text = stringResource(if (state.filter is ReviewFilter.All) R.string.review_all_reviewed else R.string.review_filter_complete),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = completionDescription(state),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(stringResource(R.string.review_cycle_summary, state.cycleReviewed, state.cycleKept, state.cycleDeleted))
        if (pendingDeletionCount > 0) {
            Text(stringResource(R.string.review_pending_summary, pendingDeletionCount,
                com.photosoap.android.util.FileSize.format(state.totalDeletionFileSize)))
        }
        if (isLimitedAccess) {
            Text(stringResource(R.string.review_limited_complete))
            TextButton(onClick = onManageAccess) { Text(stringResource(R.string.settings_photo_access)) }
        }
        TextButton(onClick = onChangeFilter) { Text(stringResource(R.string.review_change_filter)) }
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

@Composable
private fun completionDescription(state: ReviewUiState): String {
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    if (state.filter is ReviewFilter.All) {
        return stringResource(when (state.mediaKind) {
            com.photosoap.android.domain.model.MediaKind.PHOTOS -> R.string.review_library_photos_complete
            com.photosoap.android.domain.model.MediaKind.VIDEOS -> R.string.review_library_videos_complete
            com.photosoap.android.domain.model.MediaKind.ALL -> R.string.review_all_reviewed_description
        })
    }
    val scope = when (val filter = state.filter) {
        ReviewFilter.All -> stringResource(when (state.mediaKind) {
            com.photosoap.android.domain.model.MediaKind.PHOTOS -> R.string.filter_photos
            com.photosoap.android.domain.model.MediaKind.VIDEOS -> R.string.filter_videos
            com.photosoap.android.domain.model.MediaKind.ALL -> R.string.filter_all_media
        })
        is ReviewFilter.Year -> filter.year.toString()
        is ReviewFilter.Month -> java.time.YearMonth.of(filter.year, filter.month)
            .format(java.time.format.DateTimeFormatter.ofPattern("LLLL yyyy", locale))
        is ReviewFilter.Album -> filter.albumName
        is ReviewFilter.Smart -> stringResource(filter.kind.titleResource)
    }
    return stringResource(R.string.review_scope_complete, scope)
}
