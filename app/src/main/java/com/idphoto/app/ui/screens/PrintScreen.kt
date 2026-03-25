package com.idphoto.app.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idphoto.app.ui.LocalStrings
import com.idphoto.app.ui.theme.LocalAppColors

/**
 * Print Layout Screen — matching mockup.
 *
 * 3×3 grid preview sheet, quantity stepper, paper size, cut lines toggle,
 * download + print buttons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintScreen(
    photo: Bitmap?,
    quantity: Int,
    paperSize: String,
    cutLinesEnabled: Boolean,
    onBack: () -> Unit,
    onQuantityChange: (Int) -> Unit,
    onPaperSizeClick: () -> Unit,
    onCutLinesToggle: (Boolean) -> Unit,
    onDownload: () -> Unit,
    onPrint: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = LocalAppColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        // ── Top Bar ──
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colors.surface,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 52.dp, bottom = 14.dp, start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    onClick = onBack,
                    shape = RoundedCornerShape(12.dp),
                    color = colors.background,
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.ArrowBackIosNew,
                            contentDescription = strings.back,
                            modifier = Modifier.size(22.dp),
                            tint = colors.textPrimary,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    strings.printTitle,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.textPrimary,
                )
            }
        }

        // ── Print Preview: 3×3 Grid ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .width(280.dp)
                    .height(380.dp),
                shape = RoundedCornerShape(8.dp),
                color = colors.surface,
                shadowElevation = 8.dp,
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val rows = 3
                    val cols = 3
                    for (r in 0 until rows) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            for (c in 0 until cols) {
                                val idx = r * cols + c
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFD0D7E2))
                                        .then(
                                            if (cutLinesEnabled)
                                                Modifier.border(0.5.dp, colors.textTertiary, RoundedCornerShape(4.dp))
                                            else Modifier
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (idx < quantity && photo != null) {
                                        Image(
                                            bitmap = photo.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop,
                                        )
                                    } else {
                                        // Placeholder
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 18.dp, height = 22.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White.copy(alpha = 0.5f)),
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 30.dp, height = 12.dp)
                                                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                                                    .background(Color.White.copy(alpha = 0.5f)),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Print Options ──
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            color = colors.surface,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Quantity
                PrintOptionRow(
                    icon = Icons.Default.GridView,
                    label = strings.pQty,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Surface(
                            onClick = { if (quantity > 1) onQuantityChange(quantity - 1) },
                            shape = RoundedCornerShape(8.dp),
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                            modifier = Modifier.size(30.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("−", fontSize = 16.sp, color = colors.textPrimary)
                            }
                        }
                        Text(
                            "$quantity",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Surface(
                            onClick = { if (quantity < 20) onQuantityChange(quantity + 1) },
                            shape = RoundedCornerShape(8.dp),
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                            modifier = Modifier.size(30.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("+", fontSize = 16.sp, color = colors.textPrimary)
                            }
                        }
                    }
                }

                HorizontalDivider(color = colors.divider)

                // Paper size
                PrintOptionRow(
                    icon = Icons.Default.Description,
                    label = strings.pPaper,
                ) {
                    Surface(
                        onClick = onPaperSizeClick,
                        shape = RoundedCornerShape(8.dp),
                        color = colors.primaryLight,
                    ) {
                        Text(
                            "$paperSize ▾",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.primary,
                        )
                    }
                }

                HorizontalDivider(color = colors.divider)

                // Cut lines
                PrintOptionRow(
                    icon = Icons.Default.ContentCut,
                    label = strings.pCut,
                ) {
                    Switch(
                        checked = cutLinesEnabled,
                        onCheckedChange = onCutLinesToggle,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = colors.primary,
                        ),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Download + Print buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Download
                    OutlinedButton(
                        onClick = onDownload,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.pDownload, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }

                    // Print
                    Button(
                        onClick = onPrint,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                        ),
                    ) {
                        Icon(
                            Icons.Default.Print,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.pPrint, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun PrintOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = LocalAppColors.current.textSecondary,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = LocalAppColors.current.textPrimary,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}
