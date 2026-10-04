package com.photosoap.android.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
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
    val inactiveIcon: ImageVector,
    @StringRes val labelRes: Int,
)

val bottomNavItems = listOf(
    BottomNavItem(AppDestination.Review, Icons.Filled.Collections, Icons.Outlined.Collections, R.string.tab_review),
    BottomNavItem(AppDestination.Stats, Icons.Filled.BarChart, Icons.Outlined.BarChart, R.string.tab_stats),
    BottomNavItem(AppDestination.Achievements, Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents, R.string.tab_achievements),
)

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    isLimitedAccess: Boolean = false,
) {
    val transitionMotion = MaterialTheme.motionScheme.fastSpatialSpec<IntOffset>()
    val transitionFade = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
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
                Box(Modifier.navigationBarsPadding().padding(horizontal = 12.dp, vertical = 6.dp)) {
                ShortNavigationBar(
                    modifier = Modifier.clip(MaterialTheme.shapes.extraLarge),
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    bottomNavItems.forEach { item ->
                        val label = stringResource(item.labelRes)
                        ShortNavigationBarItem(
                            icon = { Icon(if (currentDestination?.hasRoute(item.route::class) == true) item.icon else item.inactiveIcon, contentDescription = null) },
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
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Review,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(transitionFade) + slideInHorizontally(transitionMotion) { it / 16 } },
            exitTransition = { fadeOut(transitionFade) },
            popEnterTransition = { fadeIn(transitionFade) + slideInHorizontally(transitionMotion) { -it / 16 } },
            popExitTransition = { fadeOut(transitionFade) },
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
