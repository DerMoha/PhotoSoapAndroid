package com.photosoap.android.ui.theme

import androidx.compose.ui.graphics.Color
import com.photosoap.android.domain.model.AccentColor
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.pow

class AccentPaletteTest {
    @Test
    fun `accent foregrounds meet normal text contrast in both appearances`() {
        for (accent in AccentColor.entries) for (dark in listOf(false, true)) {
            val palette = accentColorScheme(accent, dark)
            for ((background, foreground) in listOf(
                palette.primary to palette.onPrimary,
                palette.primaryContainer to palette.onPrimaryContainer,
                palette.secondary to palette.onSecondary,
                palette.secondaryContainer to palette.onSecondaryContainer,
                palette.tertiary to palette.onTertiary,
                palette.tertiaryContainer to palette.onTertiaryContainer,
            )) {
                val a = luminance(background)
                val b = luminance(foreground)
                val contrast = (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
                assertTrue(contrast >= 4.5, "$accent dark=$dark contrast=$contrast")
            }
        }
    }
    private fun luminance(color: Color): Double {
        fun channel(value: Float): Double = if (value <= 0.04045f) value / 12.92
            else ((value + 0.055) / 1.055).pow(2.4)
        return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
    }
}
