package com.photosoap.android.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.annotation.StringRes
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.photosoap.android.ui.achievements.AchievementsScreen
import com.photosoap.android.ui.review.ReviewScreen
import com.photosoap.android.ui.settings.SettingsScreen
import com.photosoap.android.ui.settings.PrivacyPolicyScreen
import com.photosoap.android.ui.stats.StatsScreen
import com.photosoap.android.R

@kotlinx.serialization.Serializable
sealed interface AppDestination {
    @kotlinx.serialization.Serializable
    data object Stats : AppDestination

    @kotlinx.serialization.Serializable
    data object Review : AppDestination

    @kotlinx.serialization.Serializable
    data object Achievements : AppDestination

    @kotlinx.serialization.Serializable
    data object Settings : AppDestination

    @kotlinx.serialization.Serializable
    data object DeveloperOptions : AppDestination

    @kotlinx.serialization.Serializable
    data object PrivacyPolicy : AppDestination
}

data class BottomNavItem(
    val route: AppDestination,
    val icon: ImageVector,
    @StringRes val labelRes: Int,
)

val bottomNavItems = listOf(
    BottomNavItem(AppDestination.Review, Icons.Filled.Collections, R.string.tab_review),
    BottomNavItem(AppDestination.Stats, Icons.Filled.BarChart, R.string.tab_stats),
    BottomNavItem(AppDestination.Achievements, Icons.Filled.EmojiEvents, R.string.tab_achievements),
)

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    isLimitedAccess: Boolean = false,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = bottomNavItems.any { item ->
        currentDestination?.hasRoute(item.route::class) == true
    }

    Scaffold(
        // Each destination owns its own top app bar and status-bar insets. Do not
        // apply the outer scaffold's top system inset a second time.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val label = stringResource(item.labelRes)
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = label) },
                            label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            selected = currentDestination?.hasRoute(item.route::class) == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Review,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<AppDestination.Stats> {
                StatsScreen(
                    onNavigateToSettings = { navController.navigate(AppDestination.Settings) },
                )
            }
            composable<AppDestination.Review> {
                ReviewScreen(
                    isLimitedAccess = isLimitedAccess,
                    onManageAccess = { navController.navigate(AppDestination.Settings) },
                    onNavigateToSettings = { navController.navigate(AppDestination.Settings) },
                )
            }
            composable<AppDestination.Achievements> {
                AchievementsScreen(
                    onNavigateToSettings = { navController.navigate(AppDestination.Settings) },
                )
            }
            composable<AppDestination.Settings> {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPrivacyPolicy = { navController.navigate(AppDestination.PrivacyPolicy) },
                    onNavigateToDeveloperOptions = { navController.navigate(AppDestination.DeveloperOptions) },
                )
            }
            composable<AppDestination.PrivacyPolicy> {
                PrivacyPolicyScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable<AppDestination.DeveloperOptions> {
                com.photosoap.android.ui.settings.DeveloperOptionsScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }
    }
}
