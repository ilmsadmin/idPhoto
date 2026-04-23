package com.idphoto.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// ─────────────────────────────────────────────────────────────────────
// Material 3 Expressive — Azure / Mint / Coral palette
// ─────────────────────────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF3D5AFE),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF001258),

    secondary = Color(0xFF00C9A7),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFBFF4E8),
    onSecondaryContainer = Color(0xFF00332A),

    tertiary = Color(0xFFFF7A59),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDED2),
    onTertiaryContainer = Color(0xFF802E16),

    background = Color(0xFFF4F2FA),
    onBackground = Color(0xFF1A1B26),
    surface = Color(0xFFFBFAFE),
    onSurface = Color(0xFF1A1B26),
    surfaceVariant = Color(0xFFE3DFEE),
    onSurfaceVariant = Color(0xFF47464F),

    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F3FB),
    surfaceContainer = Color(0xFFEFECF7),
    surfaceContainerHigh = Color(0xFFE9E6F2),
    surfaceContainerHighest = Color(0xFFE3DFEE),

    outline = Color(0xFF79767F),
    outlineVariant = Color(0xFFCAC6D0),

    error = Color(0xFFB3261E),
    onError = Color.White,
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFB6C3FF),
    onPrimary = Color(0xFF001F6B),
    primaryContainer = Color(0xFF2A3FC4),
    onPrimaryContainer = Color(0xFFDCE1FF),

    secondary = Color(0xFF4EDCC0),
    onSecondary = Color(0xFF003830),
    secondaryContainer = Color(0xFF004D40),
    onSecondaryContainer = Color(0xFFBFF4E8),

    tertiary = Color(0xFFFFB59C),
    onTertiary = Color(0xFF4A1700),
    tertiaryContainer = Color(0xFF713422),
    onTertiaryContainer = Color(0xFFFFDED2),

    background = Color(0xFF0F0C1C),
    onBackground = Color(0xFFE4E1EE),
    surface = Color(0xFF121218),
    onSurface = Color(0xFFE4E1EE),
    surfaceVariant = Color(0xFF31314A),
    onSurfaceVariant = Color(0xFFC8C4D4),

    surfaceContainerLowest = Color(0xFF0C0C11),
    surfaceContainerLow = Color(0xFF1A1A22),
    surfaceContainer = Color(0xFF1F1F29),
    surfaceContainerHigh = Color(0xFF28283A),
    surfaceContainerHighest = Color(0xFF31314A),

    outline = Color(0xFF948FA0),
    outlineVariant = Color(0xFF47464F),

    error = Color(0xFFEF5350),
    onError = Color(0xFF690005),
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Composable
fun IDPhotoTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val appColors = if (darkTheme) DarkAppColors else LightAppColors

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppMaterialShapes,
            content = content,
        )
    }
}
