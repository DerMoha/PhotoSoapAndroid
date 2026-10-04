package com.photosoap.android.util

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import com.photosoap.android.domain.repository.SettingsRepository
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class HapticsController(
    private val view: View,
    private val enabled: Boolean,
) {
    fun swipeThreshold() {
        if (!enabled) return
        view.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE
            else HapticFeedbackConstants.CLOCK_TICK
        )
    }

    fun swipeConfirmed() {
        if (!enabled) return
        view.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
            else HapticFeedbackConstants.VIRTUAL_KEY
        )
    }

    fun impactLight() {
        if (!enabled) return
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    fun impactMedium() {
        if (!enabled) return
        view.performHapticFeedback(confirmFeedback())
    }

    fun impactHeavy() {
        if (!enabled) return
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    fun notificationSuccess() {
        if (!enabled) return
        view.performHapticFeedback(confirmFeedback())
    }

    fun notificationError() {
        if (!enabled) return
        view.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.REJECT
            } else {
                HapticFeedbackConstants.LONG_PRESS
            }
        )
    }

    fun selection() {
        if (!enabled) return
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    private fun confirmFeedback(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.KEYBOARD_TAP
        }
}

@Composable
fun rememberHapticsController(
    settingsRepository: SettingsRepository,
): HapticsController {
    val view = LocalView.current
    val enabled by settingsRepository.hapticsEnabled.collectAsStateWithLifecycle(initialValue = false)
    return remember(view, enabled) { HapticsController(view, enabled) }
}
