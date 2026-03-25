package com.idphoto.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idphoto.app.processing.PhotoSizeManager
import com.idphoto.app.ui.AppLanguage
import com.idphoto.app.ui.LocalLanguage
import com.idphoto.app.ui.LocalStrings
import com.idphoto.app.ui.theme.LocalAppColors

/**
 * Home Screen — matching mockup design.
 *
 * Blue gradient header with Settings (left) + Language (right) buttons,
 * "ID Photo Pro" title, 2 main action cards (Camera blue + Gallery orange),
 * popular sizes 2×2 grid, "VIEW ALL SIZES" button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToCamera: () -> Unit,
    onNavigateToGallery: () -> Unit,
    onNavigateToSizes: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSizeWithIndex: (Int) -> Unit,
    onShowLanguagePicker: () -> Unit,
) {
    val strings = LocalStrings.current
    val language = LocalLanguage.current
    val colors = LocalAppColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState()),
    ) {
        // ── Top buttons + Title (flat, same bg as screen) ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 12.dp, bottom = 24.dp),
        ) {
            // ── Title + Settings button (same row) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Spacer to balance the settings button on the right
                Spacer(modifier = Modifier.size(44.dp))
                Text(
                    "ID Photo Pro",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = colors.textPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.3).sp,
                )
                // Settings button
                Surface(
                    onClick = onNavigateToSettings,
                    shape = CircleShape,
                    color = colors.surface,
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }

            // ── Subtitle (centered) ──
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                strings.homeSub,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = colors.textTertiary,
                fontSize = 14.sp,
            )
        }

        // ── 2 Main Action Cards ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Camera button
            ActionCard(
                modifier = Modifier.weight(1f),
                backgroundColor = colors.primary,
                icon = Icons.Default.CameraAlt,
                title = strings.btnCamera,
                subtitle = strings.btnCameraSub,
                onClick = onNavigateToCamera,
            )

            // Gallery button
            ActionCard(
                modifier = Modifier.weight(1f),
                backgroundColor = colors.accent,
                icon = Icons.Default.Image,
                title = strings.btnGallery,
                subtitle = strings.btnGallerySub,
                onClick = onNavigateToGallery,
            )
        }

        // ── Popular Sizes Section ──
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
        ) {
            Text(
                strings.popularSizes,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.textPrimary,
            )
            Text(
                strings.popularSizesSub,
                fontSize = 12.sp,
                color = colors.textTertiary,
                modifier = Modifier.padding(top = 2.dp),
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 2x3 Grid of popular sizes
            val popularSizes = PhotoSizeManager.popularSizes
            val sizeColors = listOf(
                Color(0xFFE3F2FD) to Color(0xFF1565C0),  // Blue
                Color(0xFFFFEBEE) to Color(0xFFC62828),  // Red
                Color(0xFFF5F5F5) to Color(0xFF616161),  // Gray
                Color(0xFFE8EAF6) to Color(0xFF283593),  // Indigo
                Color(0xFFFFF3E0) to Color(0xFFE65100),  // Orange
                Color(0xFFE8F5E9) to Color(0xFF2E7D32),  // Green
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (row in 0..2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        for (col in 0..1) {
                            val idx = row * 2 + col
                            if (idx < popularSizes.size) {
                                val size = popularSizes[idx]
                                val (bgColor, iconColor) = sizeColors[idx]
                                PopularSizeItem(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(76.dp),
                                    name = size.name,
                                    description = size.description,
                                    bgColor = bgColor,
                                    iconColor = iconColor,
                                    onClick = {
                                        val globalIdx = PhotoSizeManager.standardSizes.indexOf(size)
                                        onNavigateToSizeWithIndex(globalIdx.coerceAtLeast(0))
                                    },
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // View All Sizes — styled like a size card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp),
                shape = RoundedCornerShape(12.dp),
                color = colors.surface,
                shadowElevation = 1.dp,
                onClick = onNavigateToSizes,
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.primaryLight),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.GridView,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = colors.primary,
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            strings.viewAllSizes,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                        )
                        Text(
                            "${PhotoSizeManager.standardSizes.size}+ ${strings.popularSizesSub.lowercase()}",
                            fontSize = 11.sp,
                            color = colors.textTertiary,
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = colors.textTertiary,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun ActionCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        shadowElevation = 4.dp,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                    tint = Color.White,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                title,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                subtitle,
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun PopularSizeItem(
    modifier: Modifier = Modifier,
    name: String,
    description: String,
    bgColor: Color,
    iconColor: Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = LocalAppColors.current.surface,
        shadowElevation = 1.dp,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = iconColor,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = LocalAppColors.current.textPrimary,
                )
                Text(
                    description,
                    fontSize = 11.sp,
                    color = LocalAppColors.current.textTertiary,
                )
            }
        }
    }
}
