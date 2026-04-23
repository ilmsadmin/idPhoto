package com.idphoto.app.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Semantic color system — Material 3 Expressive (Redesign 2.0).
 *
 * Palette: Azure primary (#3D5AFE) · Mint secondary (#00C9A7) · Coral tertiary (#FF7A59).
 * Glassmorphism + gradient hero surfaces.
 *
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
    val primary: Color,                  // Azure #3D5AFE
    val onPrimary: Color,
    val primaryLight: Color,             // = primaryContainer
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val primaryGrad1: Color,             // gradient stop 1
    val primaryGrad2: Color,             // gradient stop 2
    val primaryGrad3: Color,             // gradient stop 3

    val secondary: Color,                // Mint #00C9A7
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,

    val tertiary: Color,                 // Coral #FF7A59
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
    /** Hero gradient brush — Azure → Violet → Lilac. */
    val heroGradient: Brush
        get() = Brush.linearGradient(listOf(primaryGrad1, primaryGrad2, primaryGrad3))

    /** Compact 2-stop primary gradient for buttons / icons. */
    val primaryGradient: Brush
        get() = Brush.linearGradient(listOf(primaryGrad1, primaryGrad2))

    /** Coral → magenta gradient for highlight bubbles. */
    val coralGradient: Brush
        get() = Brush.linearGradient(listOf(Color(0xFFFF7A59), Color(0xFFFF3366)))
}

// ─────────────────────────────────────────────────────────────────────
// LIGHT
// ─────────────────────────────────────────────────────────────────────
val LightAppColors = AppColors(
    isDark = false,

    background = Color(0xFFF4F2FA),
    surface = Color(0xFFFBFAFE),
    surfaceSecondary = Color(0xFFF5F3FB),
    surfaceElevated = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F3FB),
    surfaceContainer = Color(0xFFEFECF7),
    surfaceContainerHigh = Color(0xFFE9E6F2),
    surfaceContainerHighest = Color(0xFFE3DFEE),

    headerBg = Color(0xFFFBFAFE),
    headerText = Color(0xFF1A1B26),
    headerSubtext = Color(0xFF47464F),
    headerButtonBg = Color(0xFFEFECF7),
    headerButtonContent = Color(0xFF47464F),

    textPrimary = Color(0xFF1A1B26),
    textSecondary = Color(0xFF47464F),
    textTertiary = Color(0xFF79767F),
    textOnPrimary = Color.White,
    onSurface = Color(0xFF1A1B26),
    onSurfaceVariant = Color(0xFF47464F),

    primary = Color(0xFF3D5AFE),
    onPrimary = Color.White,
    primaryLight = Color(0xFFE0E7FF),
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF001258),
    primaryGrad1 = Color(0xFF5B6CFF),
    primaryGrad2 = Color(0xFF8B6DFF),
    primaryGrad3 = Color(0xFFB47CFF),

    secondary = Color(0xFF00C9A7),
    secondaryContainer = Color(0xFFBFF4E8),
    onSecondaryContainer = Color(0xFF00332A),

    tertiary = Color(0xFFFF7A59),
    tertiaryContainer = Color(0xFFFFDED2),
    onTertiaryContainer = Color(0xFF802E16),

    accent = Color(0xFFFF7A59),
    error = Color(0xFFB3261E),
    success = Color(0xFF0EA371),
    warning = Color(0xFFF59E0B),

    divider = Color(0xFFCAC6D0),
    outline = Color(0xFF79767F),
    outlineVariant = Color(0xFFCAC6D0),

    overlayBg = Color.Black.copy(alpha = 0.5f),

    iconBgBlue = Color(0xFFE0E7FF),
    iconBgGreen = Color(0xFFBFF4E8),
    iconBgOrange = Color(0xFFFFDED2),
    iconBgPurple = Color(0xFFEDE2FF),
    iconBgTeal = Color(0xFFCFF1EA),
    iconBgPink = Color(0xFFFFE1ED),
    iconBgIndigo = Color(0xFFDCE1FF),
)

// ─────────────────────────────────────────────────────────────────────
// DARK
// ─────────────────────────────────────────────────────────────────────
val DarkAppColors = AppColors(
    isDark = true,

    background = Color(0xFF0F0C1C),
    surface = Color(0xFF121218),
    surfaceSecondary = Color(0xFF1A1A22),
    surfaceElevated = Color(0xFF1F1F29),
    surfaceContainerLowest = Color(0xFF0C0C11),
    surfaceContainerLow = Color(0xFF1A1A22),
    surfaceContainer = Color(0xFF1F1F29),
    surfaceContainerHigh = Color(0xFF28283A),
    surfaceContainerHighest = Color(0xFF31314A),

    headerBg = Color(0xFF121218),
    headerText = Color(0xFFE4E1EE),
    headerSubtext = Color(0xFFC8C4D4),
    headerButtonBg = Color(0xFF1F1F29),
    headerButtonContent = Color(0xFFC8C4D4),

    textPrimary = Color(0xFFE4E1EE),
    textSecondary = Color(0xFFC8C4D4),
    textTertiary = Color(0xFF948FA0),
    textOnPrimary = Color(0xFF001F6B),
    onSurface = Color(0xFFE4E1EE),
    onSurfaceVariant = Color(0xFFC8C4D4),

    primary = Color(0xFFB6C3FF),
    onPrimary = Color(0xFF001F6B),
    primaryLight = Color(0xFF2A3FC4),
    primaryContainer = Color(0xFF2A3FC4),
    onPrimaryContainer = Color(0xFFDCE1FF),
    primaryGrad1 = Color(0xFF5B6CFF),
    primaryGrad2 = Color(0xFF8B6DFF),
    primaryGrad3 = Color(0xFFB47CFF),

    secondary = Color(0xFF4EDCC0),
    secondaryContainer = Color(0xFF004D40),
    onSecondaryContainer = Color(0xFFBFF4E8),

    tertiary = Color(0xFFFFB59C),
    tertiaryContainer = Color(0xFF713422),
    onTertiaryContainer = Color(0xFFFFDED2),

    accent = Color(0xFFFFB59C),
    error = Color(0xFFEF5350),
    success = Color(0xFF66BB6A),
    warning = Color(0xFFFBBF24),

    divider = Color(0xFF47464F),
    outline = Color(0xFF948FA0),
    outlineVariant = Color(0xFF47464F),

    overlayBg = Color.Black.copy(alpha = 0.7f),

    iconBgBlue = Color(0xFF1A2740),
    iconBgGreen = Color(0xFF1A3324),
    iconBgOrange = Color(0xFF332611),
    iconBgPurple = Color(0xFF291A40),
    iconBgTeal = Color(0xFF0D2C30),
    iconBgPink = Color(0xFF33151E),
    iconBgIndigo = Color(0xFF1A1D33),
)

val LocalAppColors = compositionLocalOf { LightAppColors }
