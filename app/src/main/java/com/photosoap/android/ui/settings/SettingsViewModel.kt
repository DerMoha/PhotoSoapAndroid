package com.photosoap.android.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photosoap.android.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val hapticsEnabled: Boolean = true,
    val useDeleteQueue: Boolean = true,
    val analyticsEnabled: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.hapticsEnabled,
                settingsRepository.useDeleteQueue,
                settingsRepository.analyticsEnabled,
            ) { haptics, deleteQueue, analytics ->
                SettingsUiState(
                    hapticsEnabled = haptics,
                    useDeleteQueue = deleteQueue,
                    analyticsEnabled = analytics,
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun toggleHaptics(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setHapticsEnabled(enabled) }
    }

    fun toggleDeleteQueue(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setUseDeleteQueue(enabled) }
    }

    fun toggleAnalytics(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAnalyticsEnabled(enabled) }
    }

    fun resetOnboarding() {
        viewModelScope.launch { settingsRepository.resetOnboarding() }
    }

    fun resetAllDevOverrides() {
        viewModelScope.launch {
            settingsRepository.setHapticsEnabled(true)
            settingsRepository.setUseDeleteQueue(true)
            settingsRepository.setAnalyticsEnabled(false)
        }
    }
}
