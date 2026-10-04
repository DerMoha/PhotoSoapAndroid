package com.photosoap.android

import android.content.Intent
import android.content.Context
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photosoap.android.data.local.datastore.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.photosoap.android.domain.model.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDataStore: SettingsDataStore,
    private val permissionChecker: PermissionChecker,
) : ViewModel() {

    val themeMode = settingsDataStore.themeMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)
    val dynamicColor = settingsDataStore.dynamicColor.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Onboarding)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val hasSeenOnboarding = settingsDataStore.hasSeenOnboarding.first()
            if (!hasSeenOnboarding) {
                _uiState.value = MainUiState.Onboarding
                return@launch
            }
            checkPermissionAndNavigate()
        }
    }

    fun onGetStarted(shareAnalytics: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setAnalyticsEnabled(shareAnalytics)
            settingsDataStore.setOnboardingSeen()
            checkPermissionAndNavigate()
        }
    }

    fun onPermissionResult(@Suppress("UNUSED_PARAMETER") granted: Boolean) {
        viewModelScope.launch {
            // The Android 14+ photo picker can grant selected-media access even when
            // RequestMultiplePermissions reports no individual media grant (for example,
            // when the user currently selects zero items). Re-read the authoritative OS
            // state after the picker closes instead of trusting the callback map.
            if (permissionChecker.hasMediaPermissions()) {
                _uiState.value = MainUiState.Main(
                    isLimitedAccess = permissionChecker.getMediaAccess() == MediaAccess.LIMITED,
                )
            } else {
                _uiState.value = MainUiState.PermissionDenied
            }
        }
    }

    fun onOpenSettings(): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
    }

    fun onSettingsDismissed() {
        viewModelScope.launch { checkPermissionAndNavigate() }
    }

    fun onResume() {
        if (_uiState.value is MainUiState.Main || _uiState.value is MainUiState.PermissionDenied) {
            viewModelScope.launch {
                val access = permissionChecker.getMediaAccess()
                _uiState.value = if (access == MediaAccess.NONE) MainUiState.PermissionDenied
                else MainUiState.Main(isLimitedAccess = access == MediaAccess.LIMITED)
            }
        }
    }

    private suspend fun checkPermissionAndNavigate() {
        if (permissionChecker.hasMediaPermissions()) {
            _uiState.value = MainUiState.Main(
                isLimitedAccess = permissionChecker.getMediaAccess() == MediaAccess.LIMITED,
            )
        } else {
            _uiState.value = MainUiState.PermissionRequest
        }
    }
}

sealed interface MainUiState {
    data object Onboarding : MainUiState
    data object PermissionRequest : MainUiState
    data object PermissionDenied : MainUiState
    data class Main(val isLimitedAccess: Boolean) : MainUiState
}
