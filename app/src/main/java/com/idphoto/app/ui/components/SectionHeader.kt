package com.idphoto.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.idphoto.app.ui.theme.LocalAppColors

/**
 * Section header used in Home / Settings screens.
 *
 *  ┌─────────────────────────────────────────────────────┐
 *  │  Recent photos  (42)            More →              │
 *  └─────────────────────────────────────────────────────┘
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = colors.onSurface,
        )
        if (count != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                "$count",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onSurfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(colors.surfaceContainer)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        if (actionLabel != null && onActionClick != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    actionLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary,
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    null,
                    modifier = Modifier.size(14.dp),
                    tint = colors.primary,
                )
            }
        }
    }
}

/**
 * Settings section title (uppercase, tracked).
 */
@Composable
fun SettingsSectionTitle(text: String, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Text(
        text.uppercase(),
        modifier = modifier.padding(start = 8.dp, top = 14.dp, bottom = 8.dp),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = colors.onSurfaceVariant,
        letterSpacing = 1.5.sp,
    )
}
