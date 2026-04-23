package com.idphoto.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idphoto.app.ui.theme.AppShapes
import com.idphoto.app.ui.theme.LocalAppColors

/**
 * Floating glass bottom navigation — pill-shaped, blurred white-ish background.
 *
 * Active item gets the gradient pill highlight.
 */
data class GlassNavItem(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
)

@Composable
fun GlassBottomNav(
    items: List<GlassNavItem>,
    activeIndex: Int,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val containerBg = if (colors.isDark) Color(0xCC1E1A3A) else Color(0xD9FFFFFF)
    val borderColor = if (colors.isDark) Color.White.copy(alpha = 0.08f) else Color.White

    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(20.dp, AppShapes.pill, clip = false)
            .clip(AppShapes.pill)
            .background(containerBg)
            .border(1.dp, borderColor, AppShapes.pill)
            .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { index, item ->
            NavItem(
                item = item,
                active = index == activeIndex,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun NavItem(
    item: GlassNavItem,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val bg = if (active) {
        Modifier.background(colors.primaryGradient, AppShapes.pill)
    } else {
        Modifier
    }
    val contentColor = if (active) Color.White else colors.onSurfaceVariant

    Column(
        modifier = modifier
            .clip(AppShapes.pill)
            .clickable(onClick = item.onClick)
            .then(bg)
            .padding(vertical = 8.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(item.icon, null, modifier = Modifier.size(22.dp), tint = contentColor)
        Spacer(Modifier.height(2.dp))
        Text(
            item.label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            maxLines = 1,
        )
    }
}
