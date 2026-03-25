package com.idphoto.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idphoto.app.ui.LocalStrings

/**
 * Processing screen — shows pipeline steps with status indicators.
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
) {
    val strings = LocalStrings.current

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
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Title
            Text(
                strings.processing,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Pipeline steps
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.12f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    steps.forEach { step ->
                        PipelineStepRow(
                            label = step.label,
                            status = step.status,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Current status message
            Text(
                currentMessage,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )

            // Error + retry
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    errorMessage,
                    color = Color(0xFFFFCDD2),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onCancel,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White,
                        ),
                    ) {
                        Text(strings.cancel)
                    }
                    Button(
                        onClick = onRetry,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF1565C0),
                        ),
                    ) {
                        Text(strings.retry)
                    }
                }
            }
        }
    }
}

@Composable
private fun PipelineStepRow(
    label: String,
    status: StepStatus,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Status icon
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    when (status) {
                        StepStatus.DONE -> Color(0xFF4CAF50).copy(alpha = 0.3f)
                        StepStatus.RUNNING -> Color.White.copy(alpha = 0.2f)
                        StepStatus.ERROR -> Color(0xFFF44336).copy(alpha = 0.3f)
                        StepStatus.PENDING -> Color.White.copy(alpha = 0.08f)
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            when (status) {
                StepStatus.DONE -> Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color(0xFF4CAF50),
                )

                StepStatus.RUNNING -> CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color.White,
                    strokeWidth = 2.dp,
                )

                StepStatus.ERROR -> Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color(0xFFF44336),
                )

                StepStatus.PENDING -> Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f)),
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            label,
            color = when (status) {
                StepStatus.DONE -> Color.White
                StepStatus.RUNNING -> Color.White
                StepStatus.ERROR -> Color(0xFFFFCDD2)
                StepStatus.PENDING -> Color.White.copy(alpha = 0.4f)
            },
            fontSize = 14.sp,
            fontWeight = if (status == StepStatus.RUNNING) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}
