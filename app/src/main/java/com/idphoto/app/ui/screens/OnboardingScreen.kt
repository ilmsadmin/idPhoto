package com.idphoto.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idphoto.app.R
import com.idphoto.app.ui.LocalLanguage
import com.idphoto.app.ui.LocalStrings
import com.idphoto.app.ui.components.AuroraBackground
import com.idphoto.app.ui.getOnboardingStrings
import com.idphoto.app.ui.theme.LocalAppColors
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
) {
    val strings = LocalStrings.current
    val onboardingStrings = getOnboardingStrings(LocalLanguage.current)
    val colors = LocalAppColors.current
    val scope = rememberCoroutineScope()
    val pages = listOf(
        OnboardingPage(
            icon = Icons.Filled.Badge,
            title = onboardingStrings.onboardingCaptureTitle,
            description = onboardingStrings.onboardingCaptureDescription,
            tint = colors.primary,
        ),
        OnboardingPage(
            icon = Icons.Filled.AutoAwesome,
            title = onboardingStrings.onboardingAiTitle,
            description = onboardingStrings.onboardingAiDescription,
            tint = colors.secondary,
        ),
        OnboardingPage(
            icon = Icons.Filled.ColorLens,
            title = onboardingStrings.onboardingExportTitle,
            description = onboardingStrings.onboardingExportDescription,
            tint = colors.tertiary,
        ),
    )
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val pageIndex = pagerState.currentPage
    val primaryButtonText = if (pageIndex < pages.lastIndex) {
        strings.next.ifBlank { onboardingStrings.onboardingPrimaryAction }
    } else {
        onboardingStrings.onboardingPrimaryAction
    }

    AuroraBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(top = 10.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp),
            ) {
                if (pageIndex < pages.lastIndex) {
                    TextButton(
                        onClick = onFinished,
                        modifier = Modifier.align(Alignment.CenterEnd),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    ) {
                        Text(
                            text = onboardingStrings.onboardingSecondaryAction,
                            color = colors.onSurfaceVariant,
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.sp,
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            IconShowcase()
            Spacer(Modifier.height(22.dp))
            Text(
                text = onboardingStrings.onboardingTitle,
                color = colors.onSurface,
                fontSize = 29.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 34.sp,
                letterSpacing = 0.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                pageSpacing = 16.dp,
            ) { index ->
                FeaturePanel(page = pages[index])
            }
            Spacer(Modifier.weight(1f))
            PageDots(count = pages.size, selectedIndex = pageIndex)
            Spacer(Modifier.height(22.dp))
            Button(
                onClick = {
                    if (pageIndex < pages.lastIndex) {
                        scope.launch { pagerState.animateScrollToPage(pageIndex + 1) }
                    } else {
                        onFinished()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.secondary,
                    contentColor = Color.White,
                ),
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp),
            ) {
                Text(
                    text = primaryButtonText,
                    color = Color.White,
                    fontSize = 17.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun IconShowcase() {
    Box(
        modifier = Modifier
            .size(196.dp)
            .shadow(28.dp, CircleShape, clip = false)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.96f),
                        Color(0xFFFFE6BD).copy(alpha = 0.72f),
                        Color(0xFFCDEEEA).copy(alpha = 0.64f),
                    ),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = R.drawable.app_icon_new),
            contentDescription = null,
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(38.dp)),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun FeaturePanel(page: OnboardingPage) {
    val colors = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(colors.surface.copy(alpha = if (colors.isDark) 0.78f else 0.86f))
            .border(1.dp, colors.outlineVariant.copy(alpha = 0.55f), RoundedCornerShape(28.dp))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(page.tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                tint = page.tint,
                modifier = Modifier.size(30.dp),
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = page.title,
            color = colors.onSurface,
            fontSize = 21.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 27.sp,
            letterSpacing = 0.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = page.description,
            color = colors.onSurfaceVariant,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            letterSpacing = 0.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PageDots(count: Int, selectedIndex: Int) {
    val colors = LocalAppColors.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .size(width = if (index == selectedIndex) 26.dp else 8.dp, height = 8.dp)
                    .clip(CircleShape)
                    .background(if (index == selectedIndex) colors.primary else colors.outlineVariant),
            )
        }
    }
}

private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val tint: Color,
)
