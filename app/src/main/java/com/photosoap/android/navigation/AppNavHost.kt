package com.photosoap.android.navigation

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
import com.photosoap.android.ui.stats.StatsScreen

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
}

data class BottomNavItem(
    val route: AppDestination,
    val icon: ImageVector,
    val label: String,
)

val bottomNavItems = listOf(
    BottomNavItem(AppDestination.Stats, Icons.Filled.BarChart, "Stats"),
    BottomNavItem(AppDestination.Review, Icons.Filled.Collections, "Review"),
    BottomNavItem(AppDestination.Achievements, Icons.Filled.EmojiEvents, "Achievements"),
)

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = bottomNavItems.any { item ->
        currentDestination?.hasRoute(item.route::class) == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
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
            startDestination = AppDestination.Stats,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<AppDestination.Stats> {
                StatsScreen(
                    onNavigateToSettings = { navController.navigate(AppDestination.Settings) },
                )
            }
            composable<AppDestination.Review> {
                ReviewScreen()
            }
            composable<AppDestination.Achievements> {
                AchievementsScreen()
            }
            composable<AppDestination.Settings> {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDeveloperOptions = { navController.navigate(AppDestination.DeveloperOptions) },
                )
            }
            composable<AppDestination.DeveloperOptions> {
                com.photosoap.android.ui.settings.DeveloperOptionsScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }
    }
}
