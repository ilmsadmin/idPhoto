package com.idphoto.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idphoto.app.ui.theme.AppShapes
import com.idphoto.app.ui.theme.LocalAppColors

/**
 * Gradient pill button (filled CTA). Hero gradient (Azure → Violet).
 */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 50.dp,
    paddingHorizontal: Dp = 22.dp,
) {
    val colors = LocalAppColors.current
    val brush = colors.primaryGradient
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = height)
            .height(height)
            .shadow(if (enabled) 16.dp else 0.dp, AppShapes.pill, clip = false)
            .clip(AppShapes.pill)
            .background(if (enabled) brush else Brush.linearGradient(listOf(colors.outlineVariant, colors.outlineVariant)))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = paddingHorizontal),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, modifier = Modifier.size(20.dp), tint = Color.White)
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * Tonal pill button — primary container background.
 */
@Composable
fun TonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    height: Dp = 46.dp,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = height)
            .height(height)
            .clip(AppShapes.pill)
            .background(colors.primaryContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, modifier = Modifier.size(18.dp), tint = colors.onPrimaryContainer)
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = colors.onPrimaryContainer, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Outlined pill button — transparent w/ outline.
 */
@Composable
fun OutlinedPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    height: Dp = 46.dp,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = height)
            .height(height)
            .clip(AppShapes.pill)
            .border(1.5.dp, colors.outlineVariant, AppShapes.pill)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, modifier = Modifier.size(18.dp), tint = colors.onSurface)
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Glass icon button — translucent white with subtle border, used on hero areas.
 *
 * @param onPrimary if true: dark glass for use over light/colored surfaces.
 */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    size: Dp = 40.dp,
    onPrimary: Boolean = false,
) {
    val colors = LocalAppColors.current
    val bg = when {
        onPrimary -> Color.White.copy(alpha = 0.18f)
        colors.isDark -> Color.White.copy(alpha = 0.08f)
        else -> Color.White.copy(alpha = 0.7f)
    }
    val borderColor = if (onPrimary || colors.isDark) Color.White.copy(alpha = 0.18f) else Color.White
    val tint = if (onPrimary) Color.White else colors.onSurface
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, modifier = Modifier.size(size * 0.5f), tint = tint)
    }
}

/**
 * Solid circular icon button used on top bars (back / save).
 */
@Composable
fun SurfaceIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    size: Dp = 40.dp,
    background: Color? = null,
    tint: Color? = null,
) {
    val colors = LocalAppColors.current
    val bg = background ?: colors.surfaceContainerHigh
    val ic = tint ?: colors.onSurface
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, modifier = Modifier.size(size * 0.5f), tint = ic)
    }
}

/**
 * Filled gradient FAB-style icon button (used on edit topbar's check button).
 */
@Composable
fun GradientCircleButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    size: Dp = 40.dp,
) {
    val colors = LocalAppColors.current
    Box(
        modifier = modifier
            .size(size)
            .shadow(10.dp, CircleShape, clip = false)
            .clip(CircleShape)
            .background(colors.primaryGradient)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, modifier = Modifier.size(size * 0.55f), tint = Color.White)
    }
}

/** Compact chip for status / tags. */
@Composable
fun TonalChip(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    background: Color? = null,
    contentColor: Color? = null,
    paddingValues: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
) {
    val colors = LocalAppColors.current
    val bg = background ?: colors.surfaceContainerHigh
    val fg = contentColor ?: colors.onSurface
    Row(
        modifier = modifier
            .clip(AppShapes.pill)
            .background(bg)
            .padding(paddingValues),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) Icon(icon, null, modifier = Modifier.size(12.dp), tint = fg)
        Text(text, color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
