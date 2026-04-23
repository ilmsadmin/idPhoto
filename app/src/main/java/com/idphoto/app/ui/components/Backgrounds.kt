package com.idphoto.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.idphoto.app.ui.theme.LocalAppColors

/**
 * Aurora background — soft multi-color radial gradients used on the Home hero
 * area. Mirrors the CSS `radial-gradient(...)` stack from the mockup.
 *
 * In light mode: pastel violet/coral/mint blobs over a near-white base.
 * In dark mode : deep violet/coral/mint blobs over a near-black base.
 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = LocalAppColors.current
    val isDark = colors.isDark

    val baseTop = if (isDark) Color(0xFF1E1A3A) else Color(0xFFF4EDFF)
    val baseBottom = if (isDark) Color(0xFF0F0C1C) else Color(0xFFFFFFFF)

    val violet = if (isDark) Color(0x738B6DFF) else Color(0x738B6DFF)
    val coral = if (isDark) Color(0x47FF7A59) else Color(0x52FF7A59)
    val mint = if (isDark) Color(0x3800C9A7) else Color(0x4700C9A7)

    Box(
        modifier = modifier
            .background(Brush.verticalGradient(listOf(baseTop, baseBottom)))
            .background(
                Brush.radialGradient(
                    colors = listOf(violet, Color.Transparent),
                    center = Offset(-100f, -150f),
                    radius = 900f,
                )
            )
            .background(
                Brush.radialGradient(
                    colors = listOf(coral, Color.Transparent),
                    center = Offset(1500f, 0f),
                    radius = 800f,
                )
            )
            .background(
                Brush.radialGradient(
                    colors = listOf(mint, Color.Transparent),
                    center = Offset(500f, 1500f),
                    radius = 700f,
                )
            )
    ) {
        content()
    }
}

/**
 * Splash entry gradient — bold purple/azure radial mix.
 */
@Composable
fun SplashGradient(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF3D5AFE),
                        Color(0xFF6A3DFE),
                        Color(0xFF9C27FF),
                    )
                )
            )
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xCC7B8BFF), Color.Transparent),
                    center = Offset(300f, 400f),
                    radius = 700f,
                )
            )
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xCCB47CFF), Color.Transparent),
                    center = Offset(900f, 1400f),
                    radius = 700f,
                )
            )
            .fillMaxSize(),
    ) {
        content()
    }
}

/** Subtle full-screen background (light: very light lavender, dark: deep). */
@Composable
fun ScreenBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = LocalAppColors.current
    Box(modifier = modifier.background(colors.background)) { content() }
}

/** Get a Size-aware brush placeholder (helper kept for API symmetry). */
internal fun emptyBrush(@Suppress("UNUSED_PARAMETER") size: Size): Brush =
    Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
