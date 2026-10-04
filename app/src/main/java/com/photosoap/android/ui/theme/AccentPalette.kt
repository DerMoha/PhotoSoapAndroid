package com.photosoap.android.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.photosoap.android.domain.model.AccentColor

fun accentSwatch(accent: AccentColor): Color = when (accent) {
    AccentColor.TEAL -> LightColorScheme.primary
    AccentColor.BLUE -> Color(0xFF005AC1)
    AccentColor.PURPLE -> Color(0xFF6750A4)
    AccentColor.ROSE -> Color(0xFF984061)
}

// Coordinated Material tonal roles, including contrasting foregrounds, for both appearances.
internal fun accentColorScheme(accent: AccentColor, dark: Boolean): ColorScheme {
    val base = if (dark) DarkColorScheme else LightColorScheme
    if (accent == AccentColor.TEAL) return base
    val tones = when (accent) {
        AccentColor.BLUE -> if (dark) listOf(
            0xFFAAC7FF, 0xFF002E69, 0xFF004494, 0xFFD8E2FF,
            0xFFBEC6DC, 0xFF283041, 0xFF3E4759, 0xFFDAE2F9,
            0xFFDEBCDF, 0xFF402843, 0xFF583E5B, 0xFFFBD7FC,
        ) else listOf(
            0xFF005AC1, 0xFFFFFFFF, 0xFFD8E2FF, 0xFF001A41,
            0xFF565F71, 0xFFFFFFFF, 0xFFDAE2F9, 0xFF131C2B,
            0xFF705574, 0xFFFFFFFF, 0xFFFBD7FC, 0xFF29132D,
        )
        AccentColor.PURPLE -> if (dark) listOf(
            0xFFD0BCFF, 0xFF381E72, 0xFF4F378B, 0xFFEADDFF,
            0xFFCCC2DC, 0xFF332D41, 0xFF4A4458, 0xFFE8DEF8,
            0xFFEFB8C8, 0xFF492532, 0xFF633B48, 0xFFFFD8E4,
        ) else listOf(
            0xFF6750A4, 0xFFFFFFFF, 0xFFEADDFF, 0xFF21005D,
            0xFF625B71, 0xFFFFFFFF, 0xFFE8DEF8, 0xFF1D192B,
            0xFF7D5260, 0xFFFFFFFF, 0xFFFFD8E4, 0xFF31111D,
        )
        AccentColor.ROSE -> if (dark) listOf(
            0xFFFFB1C8, 0xFF5E1133, 0xFF7B2949, 0xFFFFD9E3,
            0xFFE2BDC7, 0xFF422931, 0xFF5B3F47, 0xFFFFD9E3,
            0xFFF3BD6F, 0xFF442B00, 0xFF614000, 0xFFFFDDB2,
        ) else listOf(
            0xFF984061, 0xFFFFFFFF, 0xFFFFD9E3, 0xFF3E001D,
            0xFF74565F, 0xFFFFFFFF, 0xFFFFD9E3, 0xFF2B151C,
            0xFF7F570E, 0xFFFFFFFF, 0xFFFFDDB2, 0xFF281900,
        )
        AccentColor.TEAL -> error("Handled above")
    }.map(::Color)
    return base.copy(
        primary = tones[0], onPrimary = tones[1], primaryContainer = tones[2], onPrimaryContainer = tones[3],
        secondary = tones[4], onSecondary = tones[5], secondaryContainer = tones[6], onSecondaryContainer = tones[7],
        tertiary = tones[8], onTertiary = tones[9], tertiaryContainer = tones[10], onTertiaryContainer = tones[11],
        surfaceTint = tones[0],
        inversePrimary = if (dark) accentSwatch(accent) else when (accent) {
            AccentColor.BLUE -> Color(0xFFAAC7FF)
            AccentColor.PURPLE -> Color(0xFFD0BCFF)
            AccentColor.ROSE -> Color(0xFFFFB1C8)
            AccentColor.TEAL -> DarkColorScheme.primary
        },
    )
}
