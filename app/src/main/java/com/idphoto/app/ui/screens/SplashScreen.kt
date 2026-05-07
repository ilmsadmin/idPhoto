package com.idphoto.app.ui.screens

import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idphoto.app.R
import com.idphoto.app.ui.LocalStrings
import com.idphoto.app.ui.components.SplashGradient
import com.idphoto.app.ui.theme.LocalAppColors
import kotlinx.coroutines.delay

/**
 * Splash Screen inspired by the new icon: warm canvas, aqua frame, coral glow.
 */
@Composable
fun SplashScreen(
    settingsLoaded: Boolean,
    onContentReady: () -> Unit = {},
    onNavigateNext: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = LocalAppColors.current
    var visible by remember { mutableStateOf(false) }
    val logoScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.86f,
        animationSpec = tween(durationMillis = 700, easing = EaseOutBack),
        label = "splashLogoScale",
    )

    LaunchedEffect(Unit) {
        visible = true
        onContentReady()
    }
    LaunchedEffect(settingsLoaded) {
        if (settingsLoaded) {
            delay(1200)
            onNavigateNext()
        }
    }

    SplashGradient(
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                GlassLogo(modifier = Modifier.scale(logoScale))
                Spacer(Modifier.height(28.dp))
                Text(
                    strings.appName,
                    color = colors.onSurface,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.sp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    strings.splashSub,
                    color = colors.onSurfaceVariant,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.sp,
                )

                Spacer(Modifier.height(40.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = colors.primary,
                    trackColor = colors.primaryContainer,
                    strokeWidth = 3.dp,
                )
            }

            Text(
                "Developed by Zenix Labs",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
                color = colors.onSurfaceVariant.copy(alpha = 0.72f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp,
            )
        }
    }
}

@Composable
private fun GlassLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.app_icon_new),
        contentDescription = null,
        modifier = modifier
            .size(156.dp)
            .clip(RoundedCornerShape(40.dp)),
        contentScale = ContentScale.Crop,
    )
}
