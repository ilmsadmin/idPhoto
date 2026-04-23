package com.idphoto.app.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idphoto.app.ui.LocalStrings
import com.idphoto.app.ui.components.GradientButton
import com.idphoto.app.ui.components.OutlinedPillButton
import com.idphoto.app.ui.theme.LocalAppColors

/**
 * Processing screen — Material 3 Expressive (Redesign 2.0).
 *
 *  ┌────────────────────────┐
 *  │ ◯ conic ring + ✦ core  │   ← header
 *  │ Title + subtitle       │
 *  │ ─ pipeline steps ──    │
 *  │ [✓] Done    0.4s       │
 *  │ [⟳] Running ...        │
 *  │ [○] Pending —          │
 *  │ ─ cancel button ──     │
 *  └────────────────────────┘
 */
data class PipelineStepUi(
    val label: String,
    val status: StepStatus,
)

enum class StepStatus { PENDING, RUNNING, DONE, ERROR }

@Composable
fun ProcessingScreen(
    steps: List<PipelineStepUi>,
    currentMessage: String,
    errorMessage: String?,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    progress: Float = -1f, // -1 = indeterminate
) {
    val strings = LocalStrings.current
    val colors = LocalAppColors.current

    // Subtle violet glow background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        if (colors.isDark) Color(0x408B6DFF) else Color(0x308B6DFF),
                        Color.Transparent,
                    ),
                    radius = 900f,
                )
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header
            ProcessingIconStack()
            Spacer(Modifier.height(18.dp))
            Text(
                strings.processing,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.onSurface,
                letterSpacing = (-0.3).sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                strings.aiProcessingPrivacy,
                fontSize = 13.sp,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
            )

            // Progress bar (determinate khi có progress, indeterminate lúc init)
            Spacer(Modifier.height(16.dp))
            if (progress >= 0f) {
                val animated by animateFloatAsState(
                    targetValue = progress.coerceIn(0f, 1f),
                    animationSpec = tween(durationMillis = 400, easing = LinearEasing),
                    label = "progress",
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    LinearProgressIndicator(
                        progress = { animated },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = colors.primary,
                        trackColor = colors.surfaceContainer,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${(animated * 100).toInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.primary,
                    )
                }
            } else {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = colors.primary,
                    trackColor = colors.surfaceContainer,
                )
            }

            // Steps
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                steps.forEachIndexed { idx, step ->
                    ProcStepRow(
                        index = idx,
                        label = step.label,
                        status = step.status,
                    )
                }
            }

            if (currentMessage.isNotBlank()) {
                Text(
                    currentMessage,
                    color = colors.onSurfaceVariant,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                Spacer(Modifier.height(8.dp))
            }

            if (errorMessage != null) {
                Text(
                    errorMessage,
                    color = colors.error,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                ) {
                    OutlinedPillButton(
                        text = strings.cancel,
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                    )
                    GradientButton(
                        text = strings.retry,
                        onClick = onRetry,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
                    OutlinedPillButton(text = strings.cancel, onClick = onCancel)
                }
            }
        }
    }
}

// ───────────────────────── Icon stack ─────────────────────────

@Composable
private fun ProcessingIconStack() {
    val colors = LocalAppColors.current
    val transition = rememberInfiniteTransition(label = "ring")
    val angle by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(3000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "rot",
    )
    Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
        // Conic-like ring: rotating sweep of multi-color via radial-fallback (Brush.sweepGradient)
        Box(
            modifier = Modifier
                .matchParentSize()
                .rotate(angle)
                .clip(CircleShape)
                .background(
                    Brush.sweepGradient(
                        listOf(
                            Color(0xFF3D5AFE),
                            Color(0xFF8B6DFF),
                            Color(0xFFFF7A59),
                            Color(0xFF3D5AFE),
                        )
                    )
                )
                .padding(4.dp)
                .clip(CircleShape)
                .background(colors.surface),
        )
        // Core
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            colors.primaryContainer,
                            if (colors.isDark) Color(0xFF3D5AFE).copy(alpha = 0.25f) else Color(0xFFF1E9FF),
                        )
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.AutoAwesome,
                null,
                tint = colors.primary,
                modifier = Modifier.size(44.dp),
            )
        }
    }
}

// ───────────────────────── Step row ─────────────────────────

@Composable
private fun ProcStepRow(index: Int, label: String, status: StepStatus) {
    val colors = LocalAppColors.current
    val strings = LocalStrings.current

    val (bg, fg, icon) = when (status) {
        StepStatus.DONE -> Triple(
            if (colors.isDark) Color(0x4400C9A7) else Color(0xFFD7F5E8),
            colors.success,
            Icons.Filled.Check,
        )
        StepStatus.RUNNING -> Triple(colors.primaryContainer, colors.primary, Icons.Filled.Sync)
        StepStatus.ERROR -> Triple(colors.error.copy(alpha = 0.15f), colors.error, Icons.Filled.Close)
        StepStatus.PENDING -> Triple(colors.surfaceContainer, colors.onSurfaceVariant, Icons.Filled.Download)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surfaceContainerLowest)
            .border(1.dp, colors.outlineVariant, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bg),
            contentAlignment = Alignment.Center,
        ) {
            if (status == StepStatus.RUNNING) {
                CircularProgressIndicator(
                    color = fg,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp))
            }
        }
        Text(
            label,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (status == StepStatus.RUNNING) colors.primary else colors.onSurface,
        )
        Text(
            when (status) {
                StepStatus.DONE -> strings.stepDone
                StepStatus.RUNNING -> strings.stepRunning
                StepStatus.ERROR -> strings.stepError
                StepStatus.PENDING -> strings.stepPending
            },
            fontSize = 11.sp,
            color = colors.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
    }
    @Suppress("UNUSED_EXPRESSION") index
    @Suppress("UNUSED_EXPRESSION") SolidColor(Color.Transparent)
}
