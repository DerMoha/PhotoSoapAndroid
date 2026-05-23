package com.photosoap.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.photosoap.R
import com.photosoap.ui.achievements.AchievementsScreen
import com.photosoap.ui.review.ReviewScreen
import com.photosoap.ui.stats.StatsScreen

data class TabItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToDeveloperOptions: () -> Unit,
) {
    val tabs = listOf(
        TabItem(
            title = stringResource(R.string.tab_stats),
            selectedIcon = Icons.Filled.BarChart,
            unselectedIcon = Icons.Outlined.BarChart,
        ),
        TabItem(
            title = stringResource(R.string.tab_review),
            selectedIcon = Icons.Filled.PhotoLibrary,
            unselectedIcon = Icons.Outlined.PhotoLibrary,
        ),
        TabItem(
            title = stringResource(R.string.tab_achievements),
            selectedIcon = Icons.Filled.EmojiEvents,
            unselectedIcon = Icons.Outlined.EmojiEvents,
        ),
    )

    var selectedTab by rememberSaveable { mutableIntStateOf(1) } // Start on Review tab

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == index)
                                    tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title,
                            )
                        },
                        label = { Text(tab.title) },
                    )
                }
            }
        },
    ) { innerPadding ->
        when (selectedTab) {
            0 -> StatsScreen(
                modifier = Modifier.padding(innerPadding),
                onNavigateToSettings = onNavigateToSettings,
            )
            1 -> ReviewScreen(
                modifier = Modifier.padding(innerPadding),
            )
            2 -> AchievementsScreen(
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
