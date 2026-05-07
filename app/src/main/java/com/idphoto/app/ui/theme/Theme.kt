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
// Material 3 Expressive — Aqua / Coral / Warm Aperture palette
// ─────────────────────────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF54BDB4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDEEEA),
    onPrimaryContainer = Color(0xFF053733),

    secondary = Color(0xFFFF705D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDCD4),
    onSecondaryContainer = Color(0xFF6D1F14),

    tertiary = Color(0xFFFFC857),
    onTertiary = Color(0xFF2F2600),
    tertiaryContainer = Color(0xFFFFE9AD),
    onTertiaryContainer = Color(0xFF5A4200),

    background = Color(0xFFFFFBF5),
    onBackground = Color(0xFF182522),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF182522),
    surfaceVariant = Color(0xFFD5E4DF),
    onSurfaceVariant = Color(0xFF53645F),

    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF4F7F3),
    surfaceContainer = Color(0xFFEAF2EF),
    surfaceContainerHigh = Color(0xFFE0ECE8),
    surfaceContainerHighest = Color(0xFFD5E4DF),

    outline = Color(0xFF7C8B86),
    outlineVariant = Color(0xFFC5D4CF),

    error = Color(0xFFB3261E),
    onError = Color.White,
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF86DCD3),
    onPrimary = Color(0xFF062C28),
    primaryContainer = Color(0xFF1E5953),
    onPrimaryContainer = Color(0xFFCDEEEA),

    secondary = Color(0xFFFFA08E),
    onSecondary = Color(0xFF4A130D),
    secondaryContainer = Color(0xFF6A2A22),
    onSecondaryContainer = Color(0xFFFFDCD4),

    tertiary = Color(0xFFFFD977),
    onTertiary = Color(0xFF3A2B00),
    tertiaryContainer = Color(0xFF5B4200),
    onTertiaryContainer = Color(0xFFFFE9AD),

    background = Color(0xFF091715),
    onBackground = Color(0xFFEAF7F3),
    surface = Color(0xFF0F201D),
    onSurface = Color(0xFFEAF7F3),
    surfaceVariant = Color(0xFF2B4742),
    onSurfaceVariant = Color(0xFFB9CBC6),

    surfaceContainerLowest = Color(0xFF07110F),
    surfaceContainerLow = Color(0xFF162B27),
    surfaceContainer = Color(0xFF1C332F),
    surfaceContainerHigh = Color(0xFF243D39),
    surfaceContainerHighest = Color(0xFF2B4742),

    outline = Color(0xFF8FA39D),
    outlineVariant = Color(0xFF3B524D),

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
