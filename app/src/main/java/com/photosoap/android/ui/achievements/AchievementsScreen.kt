package com.photosoap.android.ui.achievements

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.photosoap.android.domain.model.Achievement
import com.photosoap.android.ui.components.AchievementCard
import com.photosoap.android.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    onNavigateToSettings: () -> Unit = {},
    viewModel: AchievementsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val largeText = LocalDensity.current.fontScale > 1.3f
    var selectedAchievement by remember { mutableStateOf<Achievement?>(null) }

    Scaffold(
        // The app navigation owns the bottom system inset; TopAppBar owns the top.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.achievements_title)) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(start = 16.dp, top = 16.dp, end = 16.dp),
        ) {
            LazyVerticalGrid(
                modifier = Modifier.weight(1f),
                columns = if (largeText) GridCells.Fixed(1) else GridCells.Adaptive(156.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularWavyProgressIndicator(
                        progress = { if (state.totalCount > 0) state.unlockedCount.toFloat() / state.totalCount else 0f },
                        modifier = Modifier.size(64.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.18f),
                        waveSpeed = 0.dp,
                    )
                    Column {
                        Text(
                            text = "${state.unlockedCount}/${state.totalCount}",
                            style = MaterialTheme.typography.displaySmall,
                        )
                        Text(
                            text = stringResource(R.string.achievements_unlocked_label),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

                }
                items(Achievement.ALL) { achievement ->
                    val stats = state.stats
                    AchievementCard(
                        title = achievement.localizedTitle(),
                        description = achievement.localizedDescription(),
                        icon = achievement.materialIcon(),
                        isUnlocked = achievement.id in state.unlockedIds,
                        progress = if (stats != null && achievement.isUnlocked(stats)) 1f
                            else if (stats != null) (achievement.progress(stats)).coerceIn(0f, 0.99f)
                            else 0f,
                        onClick = { selectedAchievement = achievement },
                    )
                }
            }
        }
    }

    if (selectedAchievement != null) {
        selectedAchievement?.let { achievement ->
            AchievementDetailSheet(
                achievement = achievement,
                isUnlocked = achievement.id in state.unlockedIds,
                onDismiss = { selectedAchievement = null },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AchievementDetailSheet(
    achievement: Achievement,
    isUnlocked: Boolean,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Icon(achievement.materialIcon(), contentDescription = null, modifier = Modifier.padding(16.dp).size(48.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = achievement.localizedTitle(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = achievement.localizedDescription(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (isUnlocked) {
                Text(
                    text = stringResource(R.string.achievement_status_unlocked),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun Achievement.localizedTitle(): String {
    val resource = when (id) {
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
        else -> return title
    }
    return stringResource(resource)
}

@Composable
private fun Achievement.localizedDescription(): String {
    val resource = when (id) {
        "first_steps" -> R.string.achievement_first_steps_desc
        "spring_cleaning" -> R.string.achievement_spring_cleaning_desc
        "memory_keeper" -> R.string.achievement_memory_keeper_desc
        "streak_master" -> R.string.achievement_streak_master_desc
        "daily_devotee" -> R.string.achievement_daily_devotee_desc
        "storage_saver" -> R.string.achievement_storage_saver_desc
        "century_club" -> R.string.achievement_century_club_desc
        "photo_pro" -> R.string.achievement_photo_pro_desc
        "decisive" -> R.string.achievement_decisive_desc
        "cleanup_champion" -> R.string.achievement_cleanup_champion_desc
        else -> return description
    }
    return stringResource(resource)
}
