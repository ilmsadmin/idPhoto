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
 * In light mode: pastel aqua/coral/yellow fields over a warm white base.
 * In dark mode : deep aqua/coral/yellow fields over a near-black base.
 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = LocalAppColors.current
    val isDark = colors.isDark

    val baseTop = if (isDark) Color(0xFF12332F) else Color(0xFFEAF8F5)
    val baseBottom = if (isDark) Color(0xFF091715) else Color(0xFFFFFBF5)

    val aqua = if (isDark) Color(0x7063CEC5) else Color(0x7354BDB4)
    val coral = if (isDark) Color(0x55FF705D) else Color(0x4DFF705D)
    val aperture = if (isDark) Color(0x44FFD66B) else Color(0x55FFD66B)

    Box(
        modifier = modifier
            .background(Brush.verticalGradient(listOf(baseTop, baseBottom)))
            .background(
                Brush.radialGradient(
                    colors = listOf(aqua, Color.Transparent),
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
                    colors = listOf(aperture, Color.Transparent),
                    center = Offset(500f, 1500f),
                    radius = 700f,
                )
            )
    ) {
        content()
    }
}

/**
 * Splash entry gradient — aqua, coral and warm aperture mix from the icon.
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
                        Color(0xFF54BDB4),
                        Color(0xFFFF705D),
                        Color(0xFFFFC857),
                    )
                )
            )
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xCCB8F1EA), Color.Transparent),
                    center = Offset(300f, 400f),
                    radius = 700f,
                )
            )
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xCCFFD66B), Color.Transparent),
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
