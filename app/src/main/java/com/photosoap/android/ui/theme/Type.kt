package com.photosoap.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight

// Keep the platform type scale and reserve heavier emphasis for key numbers and headings.
private val platformTypography = Typography()
val AppTypography = platformTypography.copy(
    headlineLarge = platformTypography.headlineLarge.copy(fontWeight = FontWeight.Bold),
    headlineMedium = platformTypography.headlineMedium.copy(fontWeight = FontWeight.Bold),
    titleLarge = platformTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
)
