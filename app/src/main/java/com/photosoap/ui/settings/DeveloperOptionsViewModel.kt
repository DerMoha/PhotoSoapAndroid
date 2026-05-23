package com.photosoap.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photosoap.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeveloperOptionsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _resultMessage = MutableStateFlow<String?>(null)
    val resultMessage: StateFlow<String?> = _resultMessage.asStateFlow()

    fun resetOnboarding() {
        viewModelScope.launch {
            settingsRepository.resetOnboarding()
            _resultMessage.value = "Onboarding reset. Restart app to see onboarding."
        }
    }

    fun resetAllOverrides() {
        viewModelScope.launch {
            settingsRepository.setHapticsEnabled(true)
            settingsRepository.setUseDeleteList(true)
            settingsRepository.setAnalyticsEnabled(false)
            _resultMessage.value = "All overrides reset to defaults."
        }
    }
}
