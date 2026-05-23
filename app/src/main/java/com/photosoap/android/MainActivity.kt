package com.photosoap.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
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
                val state by viewModel.uiState.collectAsState()

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
                            OnboardingScreen(
                                onGetStarted = viewModel::onGetStarted,
                                showPermissionRequest = true,
                            )
                        }

                        is MainUiState.PermissionDenied -> {
                            PermissionDeniedScreen(
                                onOpenSettings = viewModel::onOpenSettings,
                            )
                        }

                        is MainUiState.Main -> {
                            val navController = rememberNavController()
                            AppNavHost(navController = navController)
                        }
                    }
                }
            }
        }
    }
}
