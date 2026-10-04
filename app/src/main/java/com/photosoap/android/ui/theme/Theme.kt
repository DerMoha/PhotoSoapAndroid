package com.photosoap.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

object AppColors {
    val Keep = Color(0xFF0E7C66)
    val OnKeep = Color.White
    val KeepContainer = Color(0xFFCFEDE5)
    val OnKeepContainer = Color(0xFF002019)

    val Delete = Color(0xFFB13A2B)
    val OnDelete = Color.White
    val DeleteContainer = Color(0xFFFFDAD4)
    val OnDeleteContainer = Color(0xFF410002)

    val Warning = Color(0xFF8B5E00)
    val WarningContainer = Color(0xFFFFDEA6)
    val OnWarningContainer = Color(0xFF2C1B00)

    val Achievement = Color(0xFF7251A2)
    val AchievementContainer = Color(0xFFECDCFF)
    val OnAchievementContainer = Color(0xFF270057)
}

val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006B5F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9EF2DF),
    onPrimaryContainer = Color(0xFF00201B),
    secondary = Color(0xFF4A635E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDE8E1),
    onSecondaryContainer = Color(0xFF06201B),
    tertiary = Color(0xFF66587A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEDDCFF),
    onTertiaryContainer = Color(0xFF211533),
    error = AppColors.Delete,
    onError = Color.White,
    errorContainer = AppColors.DeleteContainer,
    onErrorContainer = AppColors.OnDeleteContainer,
    background = Color(0xFFFBFCF9),
    onBackground = Color(0xFF191C1B),
    surface = Color(0xFFFBFCF9),
    onSurface = Color(0xFF191C1B),
    surfaceVariant = Color(0xFFDCE5E1),
    onSurfaceVariant = Color(0xFF404946),
    outline = Color(0xFF707976),
    outlineVariant = Color(0xFFC0C9C5),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF5F7F4),
    surfaceContainer = Color(0xFFEFF1EE),
    surfaceContainerHigh = Color(0xFFE9EBE8),
    surfaceContainerHighest = Color(0xFFE3E5E2),
)

val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF82D5C4),
    onPrimary = Color(0xFF003730),
    primaryContainer = Color(0xFF005047),
    onPrimaryContainer = Color(0xFF9EF2DF),
    secondary = Color(0xFFB1CCC5),
    onSecondary = Color(0xFF1C3530),
    secondaryContainer = Color(0xFF334B46),
    onSecondaryContainer = Color(0xFFCDE8E1),
    tertiary = Color(0xFFD1C0E8),
    onTertiary = Color(0xFF372A49),
    tertiaryContainer = Color(0xFF4E4061),
    onTertiaryContainer = Color(0xFFEDDCFF),
    error = Color(0xFFFFB4A9),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD4),
    background = Color(0xFF101413),
    onBackground = Color(0xFFE0E3E0),
    surface = Color(0xFF101413),
    onSurface = Color(0xFFE0E3E0),
    surfaceVariant = Color(0xFF404946),
    onSurfaceVariant = Color(0xFFC0C9C5),
    outline = Color(0xFF8A938F),
    outlineVariant = Color(0xFF404946),
    surfaceContainerLowest = Color(0xFF0B0F0E),
    surfaceContainerLow = Color(0xFF191C1B),
    surfaceContainer = Color(0xFF1D201F),
    surfaceContainerHigh = Color(0xFF272B29),
    surfaceContainerHighest = Color(0xFF323533),
)

@Composable
fun PhotoSoapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
