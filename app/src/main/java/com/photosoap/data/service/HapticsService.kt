package com.photosoap.data.service

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HapticsService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDataStore: com.photosoap.data.local.datastore.SettingsDataStore,
) {
    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(VibratorManager::class.java)
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private suspend fun isEnabled(): Boolean = settingsDataStore.hapticsEnabled.first()

    suspend fun impactLight() {
        if (!isEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        }
    }

    suspend fun impactMedium() {
        if (!isEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
        }
    }

    suspend fun notificationSuccess() {
        if (!isEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
        }
    }

    suspend fun notificationError() {
        if (!isEnabled()) return
        val pattern = longArrayOf(0, 100, 50, 100)
        @Suppress("DEPRECATION")
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }

    suspend fun notificationWarning() {
        if (!isEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_THUD))
        }
    }

    suspend fun selection() {
        if (!isEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        }
    }
}
