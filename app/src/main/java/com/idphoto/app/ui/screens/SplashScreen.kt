package com.idphoto.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idphoto.app.R
import com.idphoto.app.ui.LocalStrings
import kotlinx.coroutines.delay

/**
 * Splash Screen — matching mockup.
 * Blue gradient background, camera icon in white rounded square,
 * "ID Photo Pro" title, subtitle, loading spinner.
 * Auto-navigate to Home after 1 second.
 */
@Composable
fun SplashScreen(
    onContentReady: () -> Unit = {},
    onNavigateToHome: () -> Unit,
) {
    val strings = LocalStrings.current

    // Signal that Compose content is drawn — dismiss system splash
    LaunchedEffect(Unit) {
        onContentReady()
    }

    // Spinner animation
    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "spinner"
    )

    // Auto navigate after 1s
    LaunchedEffect(Unit) {
        delay(1000)
        onNavigateToHome()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1565C0), Color(0xFF0D47A1))
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // App logo
            Image(
                painter = painterResource(R.drawable.ic_launcher),
                contentDescription = "ID Photo Pro",
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(28.dp)),
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                "ID Photo Pro",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                strings.splashSub,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 15.sp,
            )
        }

        // Loading spinner at bottom
        CircularProgressIndicator(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
                .size(32.dp)
                .rotate(rotation),
            color = Color.White,
            strokeWidth = 3.dp,
            trackColor = Color.White.copy(alpha = 0.2f),
        )
    }
}
