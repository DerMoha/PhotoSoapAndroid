package com.photosoap.android.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

object AppMotion {
    val cardSwipeOut = tween<Float>(300, easing = FastOutSlowInEasing)
    val cardSnapBack = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    val sheetEnter = tween<Float>(350, easing = FastOutSlowInEasing)
    val toastEnter = tween<Float>(400, easing = FastOutSlowInEasing)
    val numberAnimate = tween<Int>(500, easing = FastOutSlowInEasing)
    val progressRing = tween<Float>(800, easing = FastOutSlowInEasing)
    val achievementPop = spring<Float>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
}
