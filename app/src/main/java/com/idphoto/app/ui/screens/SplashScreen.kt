package com.idphoto.app.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idphoto.app.R
import com.idphoto.app.ui.LocalStrings
import com.idphoto.app.ui.components.SplashGradient
import kotlinx.coroutines.delay

/**
 * Splash Screen — Material 3 Expressive (Redesign 2.0).
 *
 * Bold purple/azure gradient w/ soft white blobs.
 * Glass logo card (rounded square 108dp) with camera icon.
 * Title + subtitle, animated 3-dot loader, version pill.
 */
@Composable
fun SplashScreen(
    onContentReady: () -> Unit = {},
    onNavigateToHome: () -> Unit,
) {
    val strings = LocalStrings.current

    LaunchedEffect(Unit) { onContentReady() }
    LaunchedEffect(Unit) {
        delay(1100)
        onNavigateToHome()
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
                GlassLogo()
                Spacer(Modifier.height(24.dp))
                Text(
                    strings.appName,
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.6).sp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    strings.splashSub,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.4.sp,
                )

                Spacer(Modifier.height(40.dp))
                DotsLoader()
            }

            // Version pill bottom-center
            Text(
                strings.appVersion,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp,
            )
        }
    }
}

@Composable
private fun GlassLogo() {
    Box(
        modifier = Modifier
            .size(140.dp)
            .shadow(32.dp, RoundedCornerShape(36.dp), clip = false)
            .clip(RoundedCornerShape(36.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.40f),
                        Color.White.copy(alpha = 0.15f),
                    )
                )
            )
            .border(2.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(36.dp)),
        contentAlignment = Alignment.Center,
    ) {
        // soft highlight
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.5f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(50f, 35f),
                        radius = 140f,
                    )
                )
        )
        // Logo image
        androidx.compose.foundation.Image(
            painter = painterResource(id = R.drawable.logo_splash),
            contentDescription = "IDPhoto Logo",
            modifier = Modifier.size(100.dp),
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(Color.White),
        )
    }
}

@Composable
private fun DotsLoader() {
    val transition = rememberInfiniteTransition(label = "dots")
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        repeat(3) { i ->
            val phase by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1400, easing = LinearEasing, delayMillis = i * 200),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "dot$i",
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .alpha(0.55f + 0.45f * phase)
                    .scale(1f + 0.3f * phase)
                    .clip(CircleShape)
                    .background(Color.White),
            )
        }
    }
}
