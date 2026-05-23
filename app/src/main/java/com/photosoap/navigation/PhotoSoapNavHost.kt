package com.photosoap.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.photosoap.ui.MainScreen
import com.photosoap.ui.settings.SettingsScreen
import com.photosoap.ui.settings.DeveloperOptionsScreen

@Composable
fun PhotoSoapNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = AppDestination.Main,
    ) {
        composable<AppDestination.Main> {
            MainScreen(
                onNavigateToSettings = {
                    navController.navigate(AppDestination.Settings)
                },
                onNavigateToDeveloperOptions = {
                    navController.navigate(AppDestination.DeveloperOptions)
                },
            )
        }

        composable<AppDestination.Settings> {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDeveloperOptions = {
                    navController.navigate(AppDestination.DeveloperOptions)
                },
            )
        }

        composable<AppDestination.DeveloperOptions> {
            DeveloperOptionsScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
