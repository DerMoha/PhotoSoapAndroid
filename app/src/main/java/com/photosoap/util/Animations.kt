package com.photosoap.util

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue

object Animations {
    const val defaultDuration = 300
    const val fastDuration = 150
    const val slowDuration = 500

    @Composable
    fun animatedProgress(
        target: Float,
        duration: Int = defaultDuration,
    ): Float {
        val animated by animateFloatAsState(
            targetValue = target,
            animationSpec = tween(duration),
        )
        return animated
    }
}
