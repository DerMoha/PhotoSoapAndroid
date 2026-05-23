package com.photosoap.android.util

import android.content.Context
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import com.photosoap.android.domain.repository.SettingsRepository

class HapticsController(
    private val view: View,
    private val enabled: Boolean,
) {
    fun impactLight() {
        if (!enabled) return
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    fun impactMedium() {
        if (!enabled) return
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    fun impactHeavy() {
        if (!enabled) return
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    fun notificationSuccess() {
        if (!enabled) return
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    fun notificationError() {
        if (!enabled) return
        view.performHapticFeedback(HapticFeedbackConstants.REJECT)
    }

    fun selection() {
        if (!enabled) return
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }
}

@Composable
fun rememberHapticsController(
    settingsRepository: SettingsRepository,
): HapticsController {
    val view = LocalView.current
    val enabled = androidx.compose.runtime.collectAsState(settingsRepository.hapticsEnabled).value
    return remember(enabled) { HapticsController(view, enabled) }
}
