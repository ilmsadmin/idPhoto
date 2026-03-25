package com.idphoto.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

/**
 * Semantic color system for the app — light & dark mode.
 * Use [LocalAppColors] throughout the app instead of hardcoded colors.
 */
data class AppColors(
    val isDark: Boolean,

    // Surfaces
    val background: Color,
    val surface: Color,
    val surfaceSecondary: Color,   // e.g. cards on top of surface
    val surfaceElevated: Color,    // e.g. search box, inputs

    // Header
    val headerBg: Color,
    val headerText: Color,
    val headerSubtext: Color,
    val headerButtonBg: Color,
    val headerButtonContent: Color,

    // Text
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textOnPrimary: Color,

    // Accent
    val primary: Color,
    val primaryLight: Color,
    val accent: Color,            // Orange
    val error: Color,
    val success: Color,

    // Divider
    val divider: Color,

    // Overlay / translucent
    val overlayBg: Color,

    // Settings icon backgrounds (light mode = pastel, dark mode = subtle)
    val iconBgBlue: Color,
    val iconBgGreen: Color,
    val iconBgOrange: Color,
    val iconBgPurple: Color,
    val iconBgTeal: Color,
    val iconBgPink: Color,
    val iconBgIndigo: Color,
)

val LightAppColors = AppColors(
    isDark = false,

    background = Color(0xFFF4F6F9),
    surface = Color.White,
    surfaceSecondary = Color.White,
    surfaceElevated = Color.White,

    headerBg = Color.White,
    headerText = Color(0xFF1A1C1E),
    headerSubtext = Color(0xFF5F6368),
    headerButtonBg = Color(0xFFF0F2F5),
    headerButtonContent = Color(0xFF5F6368),

    textPrimary = Color(0xFF1A1C1E),
    textSecondary = Color(0xFF5F6368),
    textTertiary = Color(0xFF9AA0A6),
    textOnPrimary = Color.White,

    primary = Color(0xFF1565C0),
    primaryLight = Color(0xFFE8F0FE),
    accent = Color(0xFFF57C00),
    error = Color(0xFFC62828),
    success = Color(0xFF2E7D32),

    divider = Color(0xFFE8EAED),

    overlayBg = Color.Black.copy(alpha = 0.5f),

    iconBgBlue = Color(0xFFE8F0FE),
    iconBgGreen = Color(0xFFE6F4EA),
    iconBgOrange = Color(0xFFFFF3E0),
    iconBgPurple = Color(0xFFF3E8FD),
    iconBgTeal = Color(0xFFE0F7FA),
    iconBgPink = Color(0xFFFCE4EC),
    iconBgIndigo = Color(0xFFE8EAF6),
)

val DarkAppColors = AppColors(
    isDark = true,

    background = Color(0xFF111318),
    surface = Color(0xFF1D1F24),
    surfaceSecondary = Color(0xFF252830),
    surfaceElevated = Color(0xFF2A2D35),

    headerBg = Color(0xFF1D1F24),
    headerText = Color(0xFFE2E2E6),
    headerSubtext = Color(0xFF9AA0A6),
    headerButtonBg = Color(0xFF2E3038),
    headerButtonContent = Color(0xFFBBC0C5),

    textPrimary = Color(0xFFE2E2E6),
    textSecondary = Color(0xFFBBC0C5),
    textTertiary = Color(0xFF6F7580),
    textOnPrimary = Color.White,

    primary = Color(0xFF82B1FF),
    primaryLight = Color(0xFF1A2740),
    accent = Color(0xFFFFAB40),
    error = Color(0xFFEF5350),
    success = Color(0xFF66BB6A),

    divider = Color(0xFF30333A),

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
