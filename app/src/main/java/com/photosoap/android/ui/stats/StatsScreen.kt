package com.photosoap.android.ui.stats

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.photosoap.android.ui.components.StatCard
import com.photosoap.android.util.FileSize
import com.photosoap.android.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun StatsScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshDailyValues() }
    LaunchedEffect(viewModel) {
        while (true) {
            kotlinx.coroutines.delay(60_000)
            viewModel.refreshDailyValues()
        }
    }

    val largeText = LocalDensity.current.fontScale > 1.3f
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stats_title)) },
                actions = {
                    FilledTonalIconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settings_title),
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                LoadingIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ),
                ) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Storage, contentDescription = null)
                        Text(FileSize.format(state.stats?.storageFreed ?: 0),
                            style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.stats_storage_freed), style = MaterialTheme.typography.titleMedium)
                    }
                }

                Text(
                    text = stringResource(R.string.stats_overview),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    maxItemsInEachRow = if (largeText) 1 else 2,
                ) {
                    StatCard(
                        title = stringResource(R.string.stats_photos_reviewed),
                        value = "${state.stats?.totalReviewed ?: 0}",
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        title = stringResource(R.string.stats_photos_deleted),
                        value = "${state.stats?.totalDeleted ?: 0}",
                        accentColor = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                    )
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    maxItemsInEachRow = if (largeText) 1 else 2,
                ) {
                    StatCard(
                        title = stringResource(R.string.stats_photos_kept),
                        value = "${state.stats?.totalKept ?: 0}",
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )

                }

                Text(stringResource(R.string.stats_media_breakdown), style = MaterialTheme.typography.titleLarge)
                state.stats?.let { stats ->
                    MediaStatsCard(stringResource(R.string.filter_photos), stats.photosReviewed, stats.photosKept, stats.photosDeleted, stats.photoStorageFreed)
                    MediaStatsCard(stringResource(R.string.filter_videos), stats.videosReviewed, stats.videosKept, stats.videosDeleted, stats.videoStorageFreed)
                    if (stats.totalReviewed > stats.photosReviewed + stats.videosReviewed) {
                        Text(stringResource(R.string.stats_media_history), style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.stats_streaks),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    maxItemsInEachRow = if (largeText) 1 else 2,
                ) {
                    StatCard(
                        title = stringResource(R.string.stats_reviewed_today),
                        value = "${state.stats?.todayReviewCount ?: 0}",
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        title = stringResource(R.string.stats_best_day),
                        value = "${state.stats?.bestDayReviewCount ?: 0}",
                        accentColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f),
                    )
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    maxItemsInEachRow = if (largeText) 1 else 2,
                ) {
                    StatCard(
                        title = stringResource(R.string.stats_best_streak),
                        value = "${state.stats?.bestStreak ?: 0}",
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        title = stringResource(R.string.stats_days_in_row),
                        value = "${state.stats?.dayStreak ?: 0}",
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.stats_keep_delete_ratio),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )

                val hasDecisions = ((state.stats?.totalKept ?: 0) +
                    (state.stats?.totalDeleted ?: 0)) > 0
                if (hasDecisions) {
                    Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Row(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .weight(state.keepDeleteRatio.coerceAtLeast(0.001f))
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.primary),
                        )
                        Box(
                            modifier = Modifier
                                .weight((1f - state.keepDeleteRatio).coerceAtLeast(0.001f))
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.error),
                        )
                        }
                    }

                    Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.primary),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(
                                R.string.stats_kept_percent,
                                (state.keepDeleteRatio * 100).toInt(),
                            ),
                            style = MaterialTheme.typography.labelSmall,
                        )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.error),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(
                                R.string.stats_deleted_percent,
                                ((1 - state.keepDeleteRatio) * 100).toInt(),
                            ),
                            style = MaterialTheme.typography.labelSmall,
                        )
                        }
                    }
                } else {
                    Text(
                        text = stringResource(R.string.stats_no_decisions),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun MediaStatsCard(title: String, reviewed: Int, kept: Int, deleted: Int, bytes: Long) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.stats_media_counts, reviewed, kept, deleted))
            Text(stringResource(R.string.stats_media_storage, FileSize.format(bytes)))
        }
    }
}
