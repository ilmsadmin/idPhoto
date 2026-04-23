package com.idphoto.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Shape scale — Material 3 Expressive (Redesign 2.0).
 *
 *  XS  ·  6dp
 *  SM  · 10dp
 *  MD  · 16dp
 *  LG  · 24dp
 *  XL  · 32dp
 *  2XL · 44dp  (hero / pill)
 */
object AppShapes {
    val xs = RoundedCornerShape(6.dp)
    val sm = RoundedCornerShape(10.dp)
    val md = RoundedCornerShape(16.dp)
    val lg = RoundedCornerShape(24.dp)
    val xl = RoundedCornerShape(32.dp)
    val xxl = RoundedCornerShape(44.dp)
    val pill = RoundedCornerShape(999.dp)
}

internal val AppMaterialShapes = Shapes(
    extraSmall = AppShapes.xs,
    small = AppShapes.sm,
    medium = AppShapes.md,
    large = AppShapes.lg,
    extraLarge = AppShapes.xl,
)
