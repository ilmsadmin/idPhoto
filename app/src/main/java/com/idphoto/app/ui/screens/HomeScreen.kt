package com.idphoto.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idphoto.app.R
import com.idphoto.app.processing.PhotoSize
import com.idphoto.app.processing.PhotoSizeManager
import com.idphoto.app.ui.LocalLanguage
import com.idphoto.app.ui.LocalStrings
import com.idphoto.app.ui.localizedSizeDescription
import com.idphoto.app.ui.components.AuroraBackground
import com.idphoto.app.ui.components.GlassBottomNav
import com.idphoto.app.ui.components.GlassIconButton
import com.idphoto.app.ui.components.GlassNavItem
import com.idphoto.app.ui.components.SectionHeader
import com.idphoto.app.ui.components.SizeThumb
import com.idphoto.app.ui.components.TonalChip
import com.idphoto.app.ui.theme.AppShapes
import com.idphoto.app.ui.theme.LocalAppColors

/**
 * Home Screen — Material 3 Expressive (Redesign 2.0).
 *
 * Layout:
 *   • Aurora hero (top) with brand mark, greeting, gradient hero CTA, feature strip
 *   • Body (bottom): popular sizes grid, tip card
 *   • Floating glass bottom nav (Home / Sizes / Settings)
 */
@Composable
fun HomeScreen(
    onNavigateToCamera: () -> Unit,            // kept for backward compat (unused)
    onNavigateToGallery: () -> Unit,
    onNavigateToSizes: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSizeWithIndex: (Int) -> Unit,
    onShowLanguagePicker: () -> Unit,
) {
    @Suppress("UNUSED_VARIABLE") val unused = onNavigateToCamera
    val strings = LocalStrings.current
    @Suppress("UNUSED_VARIABLE") val lang = LocalLanguage.current
    val colors = LocalAppColors.current
    val scroll = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll),
        ) {
            // ── HERO (aurora) ─────────────────────────────────────────
            AuroraBackground(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 8.dp, bottom = 36.dp),
                ) {
                    HomeTopBar(
                        onLanguageClick = onShowLanguagePicker,
                        onSettingsClick = onNavigateToSettings,
                    )
                    Spacer(Modifier.height(14.dp))
                    GreetingRow()
                    Spacer(Modifier.height(8.dp))
                    Text(
                        buildAnnotatedTitle(),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.onSurface,
                        letterSpacing = (-0.6).sp,
                        lineHeight = 32.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        strings.homeSub,
                        fontSize = 13.sp,
                        color = colors.onSurfaceVariant,
                        lineHeight = 18.sp,
                    )
                    Spacer(Modifier.height(18.dp))
                    HeroCta(onClick = onNavigateToGallery)
                    Spacer(Modifier.height(14.dp))
                    FeatureStrip()
                }
            }

            // ── BODY ──────────────────────────────────────────────────
            Surface(
                colors = colors,
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-20).dp)
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(colors.surface),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 28.dp, bottom = 110.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    PopularSizesSection(
                        onSizeClick = { idx -> onNavigateToSizeWithIndex(idx) },
                        onSeeAll = onNavigateToSizes,
                    )
                    TipCard()
                }
            }
        }

        // ── Floating glass bottom nav ─────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .navigationBarsPadding()
                .padding(bottom = 10.dp),
        ) {
            GlassBottomNav(
                items = listOf(
                    GlassNavItem(Icons.Filled.Home, strings.navHome, onClick = {}),
                    GlassNavItem(Icons.Filled.PhotoLibrary, strings.navPhotos, onClick = onNavigateToGallery),
                    GlassNavItem(Icons.Filled.Straighten, strings.navSizes, onClick = onNavigateToSizes),
                    GlassNavItem(Icons.Filled.Person, strings.navProfile, onClick = onNavigateToSettings),
                ),
                activeIndex = 0,
            )
        }
    }
}

// ───────────────────────── Sub-composables ─────────────────────────

@Composable
private fun HomeTopBar(
    onLanguageClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    val strings = LocalStrings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Brand mark
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .shadow(10.dp, RoundedCornerShape(12.dp), clip = false)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_icon_new),
                    contentDescription = strings.appName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    strings.appName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.onSurface,
                    letterSpacing = (-0.2).sp,
                )
                Text(
                    strings.brandTagline,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurfaceVariant,
                    letterSpacing = 1.0.sp,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassIconButton(Icons.Filled.Language, onClick = onLanguageClick, contentDescription = strings.contentLanguage)
            GlassIconButton(Icons.AutoMirrored.Filled.HelpOutline, onClick = {}, contentDescription = strings.contentHelp)
        }
    }
}

@Composable
private fun GreetingRow() {
    val colors = LocalAppColors.current
    val strings = LocalStrings.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            strings.greeting,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        TonalChip(
            text = strings.onDeviceLabel,
            icon = Icons.Filled.VerifiedUser,
            background = if (colors.isDark) Color(0x4463CEC5) else Color(0x33CDEEEA),
            contentColor = if (colors.isDark) Color(0xFF9EEDE5) else Color(0xFF0E625B),
        )
    }
}

@Composable
private fun buildAnnotatedTitle(): androidx.compose.ui.text.AnnotatedString {
    val colors = LocalAppColors.current
    val strings = LocalStrings.current
    return androidx.compose.ui.text.buildAnnotatedString {
        append(strings.titlePart1)
        // Bold gradient highlight via SpanStyle (fallback color since text gradient is non-trivial)
        withStyle(
            androidx.compose.ui.text.SpanStyle(
                color = colors.primary,
                fontWeight = FontWeight.ExtraBold,
            )
        ) {
            append(strings.titlePart2)
        }
        append("\n")
        append(strings.titlePart3)
    }
}

private inline fun androidx.compose.ui.text.AnnotatedString.Builder.withStyle(
    style: androidx.compose.ui.text.SpanStyle,
    block: androidx.compose.ui.text.AnnotatedString.Builder.() -> Unit,
) {
    val i = pushStyle(style); block(); pop()
}

@Composable
private fun HeroCta(onClick: () -> Unit) {
    val colors = LocalAppColors.current
    val strings = LocalStrings.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(24.dp, RoundedCornerShape(28.dp), clip = false)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF54BDB4),
                        Color(0xFFFF705D),
                        Color(0xFFFFC857),
                    )
                )
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 22.dp),
    ) {
        Column {
            // tag line
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.18f))
                    .padding(horizontal = 11.dp, vertical = 5.dp),
            ) {
                Text(
                    strings.aiPoweredTag,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                strings.selectFromGallery,
                color = Color.White,
                fontSize = 21.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.4).sp,
                lineHeight = 26.sp,
                modifier = Modifier.fillMaxWidth(0.78f),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                strings.aiProcessingTime,
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 12.5.sp,
                lineHeight = 17.sp,
                modifier = Modifier.fillMaxWidth(0.72f),
            )
            Spacer(Modifier.height(18.dp))
            // CTA pill — white background w/ small gradient circle icon
            Row(
                modifier = Modifier
                    .shadow(10.dp, RoundedCornerShape(999.dp), clip = false)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White)
                    .clickable(onClick = onClick)
                    .padding(start = 6.dp, end = 18.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(colors.primaryGradient),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.PhotoLibrary, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    strings.btnGallery,
                    color = Color(0xFF2A3CDE),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
    }
}

@Composable
private fun FeatureStrip() {
    val strings = LocalStrings.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.height(IntrinsicSize.Max),
    ) {
        FeatureItem(Icons.Filled.AutoAwesome, strings.bgRemovalTitle, strings.bgRemovalTech, modifier = Modifier.weight(1f).fillMaxHeight())
        FeatureItem(Icons.Filled.FaceRetouchingNatural, strings.faceAlignmentTitle, strings.faceAlignmentTech, modifier = Modifier.weight(1f).fillMaxHeight())
        FeatureItem(Icons.Filled.Lock, strings.privacyTitle, strings.privacyTech, modifier = Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
private fun FeatureItem(icon: ImageVector, title: String, subtitle: String, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    val bg = if (colors.isDark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.7f)
    val border = if (colors.isDark) Color.White.copy(alpha = 0.10f) else Color.White
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, modifier = Modifier.size(22.dp), tint = colors.primary)
        Spacer(Modifier.height(4.dp))
        Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
        Text(subtitle, fontSize = 9.5.sp, color = colors.onSurfaceVariant, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun PopularSizesSection(
    onSizeClick: (Int) -> Unit,
    onSeeAll: () -> Unit,
) {
    val colors = LocalAppColors.current
    val strings = LocalStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionHeader(
            title = strings.popularSizes,
            actionLabel = strings.viewAllSizes.let { if (it.length > 14) strings.viewAll else it },
            onActionClick = onSeeAll,
        )

        // Country mini chips (display-only for now; tapping navigates to full list)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CountryMiniChip(strings.countryVnFlag, active = true, onClick = onSeeAll)
            CountryMiniChip(strings.countryUsFlag, active = false, onClick = onSeeAll)
            CountryMiniChip(strings.countryJpFlag, active = false, onClick = onSeeAll)
            CountryMiniChip(strings.countryEuFlag, active = false, onClick = onSeeAll)
            CountryMiniChip(strings.countryKrFlag, active = false, onClick = onSeeAll)
        }

        // Grid 2×2 of popular sizes
        val popular = PhotoSizeManager.popularSizes.take(4)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                if (popular.isNotEmpty()) {
                    SizeCard(
                        size = popular[0],
                        featured = true,
                        modifier = Modifier.weight(1f),
                        onClick = { onSizeClick(globalIndex(popular[0])) },
                    )
                }
                if (popular.size > 1) {
                    SizeCard(
                        size = popular[1],
                        featured = false,
                        modifier = Modifier.weight(1f),
                        onClick = { onSizeClick(globalIndex(popular[1])) },
                    )
                } else Spacer(Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                if (popular.size > 2) {
                    SizeCard(
                        size = popular[2],
                        featured = false,
                        modifier = Modifier.weight(1f),
                        onClick = { onSizeClick(globalIndex(popular[2])) },
                    )
                } else Spacer(Modifier.weight(1f))
                if (popular.size > 3) {
                    SizeCard(
                        size = popular[3],
                        featured = false,
                        modifier = Modifier.weight(1f),
                        onClick = { onSizeClick(globalIndex(popular[3])) },
                    )
                } else Spacer(Modifier.weight(1f))
            }
        }
    }
}

private fun globalIndex(size: PhotoSize): Int =
    PhotoSizeManager.standardSizes.indexOf(size).coerceAtLeast(0)

@Composable
private fun CountryMiniChip(text: String, active: Boolean, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    val bg = if (active) colors.onSurface else colors.surfaceContainer
    val fg = if (active) colors.surface else colors.onSurfaceVariant
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(text, color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SizeCard(
    size: PhotoSize,
    featured: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    val strings = LocalStrings.current
    val bg: Modifier = if (featured) {
        Modifier.background(
            Brush.linearGradient(
                listOf(
                    if (colors.isDark) Color(0x3063CEC5) else Color(0xFFE7F7F4),
                    if (colors.isDark) Color(0x30FF705D) else Color(0xFFFFE4DC),
                )
            )
        )
    } else {
        Modifier
            .background(colors.surfaceContainerLowest)
            .border(1.dp, colors.outlineVariant, RoundedCornerShape(20.dp))
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .then(bg)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
            SizeThumb(width = 30.dp, height = 42.dp, accent = colors.primary)
            Spacer(Modifier.weight(1f))
            if (featured) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.coralGradient)
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                ) {
                    Text(strings.hotBadge, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                }
            }
        }
        Column {
            Text(
                size.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.onSurface,
                letterSpacing = (-0.2).sp,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                strings.localizedSizeDescription(size.description),
                fontSize = 11.sp,
                color = colors.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(colors.primary.copy(alpha = 0.10f))
                .padding(horizontal = 8.dp, vertical = 3.dp),
        ) {
            Text(
                "${size.widthPx} × ${size.heightPx} px",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = colors.primary,
            )
        }
    }
}

@Composable
private fun TipCard() {
    val colors = LocalAppColors.current
    val strings = LocalStrings.current
    val bg = if (colors.isDark) {
        Brush.linearGradient(listOf(Color(0x40FF705D), Color(0x40FFD66B)))
    } else {
        Brush.linearGradient(listOf(Color(0xFFFFF5E0), Color(0xFFFFE4DC)))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, Color(0xFFFF705D).copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .shadow(12.dp, RoundedCornerShape(14.dp), clip = false)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.coralGradient),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.TipsAndUpdates, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(strings.tipTitle, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = colors.onSurface)
            Spacer(Modifier.height(2.dp))
            Text(
                strings.tipDescription,
                fontSize = 11.sp,
                color = colors.onSurfaceVariant,
                lineHeight = 15.sp,
            )
        }
    }
}

// Lightweight Surface wrapper to avoid pulling in Material3 Surface (which adds shadows)
@Composable
private fun Surface(
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") colors: com.idphoto.app.ui.theme.AppColors,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) { content() }
}
