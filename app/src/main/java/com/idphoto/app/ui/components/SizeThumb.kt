package com.idphoto.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.idphoto.app.ui.theme.LocalAppColors

/**
 * A small "ID card" thumbnail used to visually represent a photo size:
 *
 *   ┌───────┐
 *   │  ●    │   ← head silhouette
 *   │       │
 *   └───────┘
 */
@Composable
fun SizeThumb(
    modifier: Modifier = Modifier,
    width: Dp = 36.dp,
    height: Dp = 50.dp,
    accent: Color? = null,
) {
    val colors = LocalAppColors.current
    val border = accent ?: colors.primary
    Box(
        modifier = modifier
            .size(width = width, height = height)
            .clip(RoundedCornerShape(6.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFC6D4FF), Color(0xFFE7EEFF))
                )
            )
            .border(2.dp, border, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.TopCenter,
    ) {
        // head
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(10.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFFFB59A), Color(0xFFFF8A65)),
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape),
        )
    }
}
