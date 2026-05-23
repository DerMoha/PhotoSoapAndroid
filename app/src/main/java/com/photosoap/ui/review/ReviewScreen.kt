package com.photosoap.ui.review

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.photosoap.R
import com.photosoap.domain.model.SwipeDirection
import com.photosoap.ui.components.DeleteQueueTray
import com.photosoap.ui.components.ProgressRing
import com.photosoap.ui.components.SwipeResult
import com.photosoap.ui.components.SwipeableCard

@Composable
fun ReviewScreen(
    modifier: Modifier = Modifier,
    viewModel: ReviewViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            ReviewHeader(
                todayCount = uiState.reviewedCount,
                filterActive = uiState.filter != com.photosoap.domain.model.PhotoFilter.All ||
                    uiState.mediaKind != com.photosoap.domain.model.ReviewMediaKind.All,
                dailyProgress = uiState.dailyChallenge.progressFraction,
                onFilterClick = { viewModel.toggleFilterSheet() },
            )

            // Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                when {
                    uiState.isLoading -> {
                        LoadingState()
                    }
                    uiState.isAllReviewed && uiState.photos.isEmpty() -> {
                        EmptyState(onRefresh = { viewModel.loadPhotos() })
                    }
                    uiState.isAllReviewed -> {
                        AllReviewedState(
                            deletedCount = uiState.deletedCount,
                            keptCount = uiState.keptCount,
                            deleteQueueCount = uiState.deleteQueue.size,
                            onReviewAgain = { viewModel.loadPhotos() },
                            onReviewDeleteList = { viewModel.toggleDeleteQueueSheet() },
                        )
                    }
                    else -> {
                        uiState.photos.getOrNull(uiState.currentIndex)?.let { photo ->
                            SwipeableCard(
                                photo = photo,
                                onSwipe = { result ->
                                    when (result) {
                                        SwipeResult.Keep -> viewModel.onSwipe(SwipeDirection.Keep)
                                        SwipeResult.Delete -> viewModel.onSwipe(SwipeDirection.Delete)
                                        SwipeResult.None -> {}
                                    }
                                },
                            )
                        }
                    }
                }
            }

            // Delete Queue Tray
            AnimatedVisibility(
                visible = uiState.deleteQueue.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            ) {
                DeleteQueueTray(
                    queueCount = uiState.deleteQueue.size,
                    onClearQueue = { viewModel.clearDeleteQueue() },
                    onViewQueue = { viewModel.toggleDeleteQueueSheet() },
                )
            }

            // Bottom action buttons
            if (!uiState.isAllReviewed && uiState.currentIndex > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    IconButton(
                        onClick = { viewModel.undoLastSwipe() },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Undo,
                            contentDescription = stringResource(R.string.undo),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // Toast overlays
        uiState.newlyUnlockedAchievement?.let { achievementId ->
            AchievementToast(
                achievementId = achievementId,
                onDismiss = { viewModel.dismissAchievement() },
            )
        }

        uiState.streakMilestone?.let { milestone ->
            StreakToast(
                count = milestone,
                onDismiss = { viewModel.dismissStreakMilestone() },
            )
        }

        uiState.dailyGoalComplete.let {
            if (it) {
                DailyGoalToast(onDismiss = { viewModel.dismissDailyGoal() })
            }
        }
    }
}

@Composable
private fun ReviewHeader(
    todayCount: Int,
    filterActive: Boolean,
    dailyProgress: Float,
    onFilterClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Streak flame
        Text(
            text = "$todayCount 🔥",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.weight(1f),
        )

        // Daily goal progress
        ProgressRing(
            progress = dailyProgress,
            size = 36.dp,
            strokeWidth = 3.dp,
            color = MaterialTheme.colorScheme.tertiary,
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Filter button
        FilterChip(
            selected = filterActive,
            onClick = onFilterClick,
            label = { Text(stringResource(R.string.filter_title)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            },
        )
    }
}

@Composable
private fun LoadingState() {
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

@Composable
private fun EmptyState(onRefresh: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.review_empty),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onRefresh) {
                Text(stringResource(R.string.review_again))
            }
        }
    }
}

@Composable
private fun AllReviewedState(
    deletedCount: Int,
    keptCount: Int,
    deleteQueueCount: Int,
    onReviewAgain: () -> Unit,
    onReviewDeleteList: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Text(
                text = "🎉",
                style = MaterialTheme.typography.displayLarge,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.review_all_reviewed),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.review_all_reviewed_summary, deletedCount, keptCount),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (deleteQueueCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.review_delete_list_summary, deleteQueueCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onReviewAgain) {
                Text(stringResource(R.string.review_again))
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (deleteQueueCount > 0) {
                OutlinedButton(
                    onClick = onReviewDeleteList,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.review_delete_list_button))
                }
            }
        }
    }
}

@Composable
private fun AchievementToast(
    achievementId: String,
    onDismiss: () -> Unit,
) {
    val achievement = com.photosoap.domain.model.Achievement.all.find { it.id == achievementId }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 80.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFFD700),
                            Color(0xFFFFA000),
                        ),
                    ),
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "🏆",
                style = MaterialTheme.typography.displaySmall,
            )
            Text(
                text = stringResource(R.string.achievement_unlocked),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
            Text(
                text = achievement?.title ?: achievementId,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
            )
        }
    }
}

@Composable
private fun StreakToast(
    count: Int,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 80.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFF9800),
                            Color(0xFFF44336),
                        ),
                    ),
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "🔥",
                style = MaterialTheme.typography.displaySmall,
            )
            Text(
                text = stringResource(R.string.streak_milestone, count),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun DailyGoalToast(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 80.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF4CAF50),
                            Color(0xFF2196F3),
                        ),
                    ),
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "🎯",
                style = MaterialTheme.typography.displaySmall,
            )
            Text(
                text = stringResource(R.string.daily_goal_complete),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
        }
    }
}
