package com.idphoto.app.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Semantic color system inspired by the app icon.
 *
 * Palette: aqua lens frame, coral portrait accents, and warm aperture yellow.
 * Old fields are kept for backward-compat; new M3 Expressive tokens are added.
 */
data class AppColors(
    val isDark: Boolean,

    // ── Surfaces (M3 Expressive) ──────────────────────────────────────
    val background: Color,
    val surface: Color,                  // base surface
    val surfaceSecondary: Color,         // = surfaceContainerLow
    val surfaceElevated: Color,          // = surfaceContainerLowest
    val surfaceContainerLowest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,

    // ── Header (legacy aliases) ───────────────────────────────────────
    val headerBg: Color,
    val headerText: Color,
    val headerSubtext: Color,
    val headerButtonBg: Color,
    val headerButtonContent: Color,

    // ── Text ──────────────────────────────────────────────────────────
    val textPrimary: Color,              // = onSurface
    val textSecondary: Color,            // = onSurfaceVariant
    val textTertiary: Color,
    val textOnPrimary: Color,            // = onPrimary
    val onSurface: Color,
    val onSurfaceVariant: Color,

    // ── Brand colors ──────────────────────────────────────────────────
    val primary: Color,                  // Aqua frame
    val onPrimary: Color,
    val primaryLight: Color,             // = primaryContainer
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val primaryGrad1: Color,             // gradient stop 1
    val primaryGrad2: Color,             // gradient stop 2
    val primaryGrad3: Color,             // gradient stop 3

    val secondary: Color,                // Coral portrait
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,

    val tertiary: Color,                 // Warm aperture yellow
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,

    val accent: Color,                   // legacy alias = tertiary
    val error: Color,
    val success: Color,
    val warning: Color,

    // ── Outlines / dividers ───────────────────────────────────────────
    val divider: Color,                  // = outlineVariant
    val outline: Color,
    val outlineVariant: Color,

    // ── Misc ──────────────────────────────────────────────────────────
    val overlayBg: Color,

    // ── Settings icon backgrounds (legacy palette) ────────────────────
    val iconBgBlue: Color,
    val iconBgGreen: Color,
    val iconBgOrange: Color,
    val iconBgPurple: Color,
    val iconBgTeal: Color,
    val iconBgPink: Color,
    val iconBgIndigo: Color,
) {
    /** Hero gradient brush — Aqua → Coral → Warm Yellow. */
    val heroGradient: Brush
        get() = Brush.linearGradient(listOf(primaryGrad1, primaryGrad2, primaryGrad3))

    /** Compact 2-stop primary gradient for buttons / icons. */
    val primaryGradient: Brush
        get() = Brush.linearGradient(listOf(primaryGrad1, primaryGrad2))

    /** Coral → warm amber gradient for highlight bubbles. */
    val coralGradient: Brush
        get() = Brush.linearGradient(listOf(Color(0xFFFF705D), Color(0xFFFFD66B)))
}

// ─────────────────────────────────────────────────────────────────────
// LIGHT
// ─────────────────────────────────────────────────────────────────────
val LightAppColors = AppColors(
    isDark = false,

    background = Color(0xFFFFFBF5),
    surface = Color(0xFFFFFFFF),
    surfaceSecondary = Color(0xFFF4F7F3),
    surfaceElevated = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF4F7F3),
    surfaceContainer = Color(0xFFEAF2EF),
    surfaceContainerHigh = Color(0xFFE0ECE8),
    surfaceContainerHighest = Color(0xFFD5E4DF),

    headerBg = Color(0xFFFFFFFF),
    headerText = Color(0xFF182522),
    headerSubtext = Color(0xFF53645F),
    headerButtonBg = Color(0xFFEAF2EF),
    headerButtonContent = Color(0xFF53645F),

    textPrimary = Color(0xFF182522),
    textSecondary = Color(0xFF53645F),
    textTertiary = Color(0xFF7C8B86),
    textOnPrimary = Color.White,
    onSurface = Color(0xFF182522),
    onSurfaceVariant = Color(0xFF53645F),

    primary = Color(0xFF54BDB4),
    onPrimary = Color.White,
    primaryLight = Color(0xFFCDEEEA),
    primaryContainer = Color(0xFFCDEEEA),
    onPrimaryContainer = Color(0xFF053733),
    primaryGrad1 = Color(0xFF55BDB5),
    primaryGrad2 = Color(0xFFFF705D),
    primaryGrad3 = Color(0xFFFFD66B),

    secondary = Color(0xFFFF705D),
    secondaryContainer = Color(0xFFFFDCD4),
    onSecondaryContainer = Color(0xFF6D1F14),

    tertiary = Color(0xFFFFC857),
    tertiaryContainer = Color(0xFFFFE9AD),
    onTertiaryContainer = Color(0xFF5A4200),

    accent = Color(0xFFFF705D),
    error = Color(0xFFB3261E),
    success = Color(0xFF0EA371),
    warning = Color(0xFFF59E0B),

    divider = Color(0xFFC5D4CF),
    outline = Color(0xFF7C8B86),
    outlineVariant = Color(0xFFC5D4CF),

    overlayBg = Color.Black.copy(alpha = 0.5f),

    iconBgBlue = Color(0xFFD7F0F4),
    iconBgGreen = Color(0xFFCDEEEA),
    iconBgOrange = Color(0xFFFFE9AD),
    iconBgPurple = Color(0xFFFFDCD4),
    iconBgTeal = Color(0xFFDDF4EF),
    iconBgPink = Color(0xFFFFE4DC),
    iconBgIndigo = Color(0xFFE4F0EA),
)

// ─────────────────────────────────────────────────────────────────────
// DARK
// ─────────────────────────────────────────────────────────────────────
val DarkAppColors = AppColors(
    isDark = true,

    background = Color(0xFF091715),
    surface = Color(0xFF0F201D),
    surfaceSecondary = Color(0xFF162B27),
    surfaceElevated = Color(0xFF1C332F),
    surfaceContainerLowest = Color(0xFF07110F),
    surfaceContainerLow = Color(0xFF162B27),
    surfaceContainer = Color(0xFF1C332F),
    surfaceContainerHigh = Color(0xFF243D39),
    surfaceContainerHighest = Color(0xFF2B4742),

    headerBg = Color(0xFF0F201D),
    headerText = Color(0xFFEAF7F3),
    headerSubtext = Color(0xFFB9CBC6),
    headerButtonBg = Color(0xFF1C332F),
    headerButtonContent = Color(0xFFB9CBC6),

    textPrimary = Color(0xFFEAF7F3),
    textSecondary = Color(0xFFB9CBC6),
    textTertiary = Color(0xFF8FA39D),
    textOnPrimary = Color(0xFF062C28),
    onSurface = Color(0xFFEAF7F3),
    onSurfaceVariant = Color(0xFFB9CBC6),

    primary = Color(0xFF86DCD3),
    onPrimary = Color(0xFF062C28),
    primaryLight = Color(0xFF1E5953),
    primaryContainer = Color(0xFF1E5953),
    onPrimaryContainer = Color(0xFFCDEEEA),
    primaryGrad1 = Color(0xFF63CEC5),
    primaryGrad2 = Color(0xFFFF7C68),
    primaryGrad3 = Color(0xFFFFD66B),

    secondary = Color(0xFFFFA08E),
    secondaryContainer = Color(0xFF6A2A22),
    onSecondaryContainer = Color(0xFFFFDCD4),

    tertiary = Color(0xFFFFD977),
    tertiaryContainer = Color(0xFF5B4200),
    onTertiaryContainer = Color(0xFFFFE9AD),

    accent = Color(0xFFFFA08E),
    error = Color(0xFFEF5350),
    success = Color(0xFF66BB6A),
    warning = Color(0xFFFBBF24),

    divider = Color(0xFF3B524D),
    outline = Color(0xFF8FA39D),
    outlineVariant = Color(0xFF3B524D),

    overlayBg = Color.Black.copy(alpha = 0.7f),

    iconBgBlue = Color(0xFF183940),
    iconBgGreen = Color(0xFF1B413B),
    iconBgOrange = Color(0xFF4A3710),
    iconBgPurple = Color(0xFF4A251E),
    iconBgTeal = Color(0xFF123C37),
    iconBgPink = Color(0xFF4B2520),
    iconBgIndigo = Color(0xFF233A35),
)

val LocalAppColors = compositionLocalOf { LightAppColors }
