package com.photosoap.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.photosoap.android.navigation.AppNavHost
import com.photosoap.android.ui.components.PermissionGate
import com.photosoap.android.ui.onboarding.OnboardingScreen
import com.photosoap.android.ui.onboarding.PermissionDeniedScreen
import com.photosoap.android.ui.theme.PhotoSoapTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PhotoSoapTheme {
                val viewModel: MainViewModel = hiltViewModel()
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val navController = rememberNavController()

                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                    viewModel.onResume()
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    when (state) {
                        is MainUiState.Onboarding -> {
                            OnboardingScreen(
                                onGetStarted = viewModel::onGetStarted,
                            )
                        }

                        is MainUiState.PermissionRequest -> {
                            PermissionGate(
                                isGranted = false,
                                onResult = viewModel::onPermissionResult,
                            ) { }
                        }

                        is MainUiState.PermissionDenied -> {
                            PermissionDeniedScreen(
                                settingsIntent = viewModel.onOpenSettings(),
                                onDismiss = viewModel::onSettingsDismissed,
                            )
                        }

                        is MainUiState.Main -> {
                            AppNavHost(
                                navController = navController,
                                isLimitedAccess = (state as MainUiState.Main).isLimitedAccess,
                            )
                        }
                    }
                }
            }
        }
    }
}
