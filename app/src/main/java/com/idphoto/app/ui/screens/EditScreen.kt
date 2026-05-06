package com.idphoto.app.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idphoto.app.processing.ImageUtils
import com.idphoto.app.processing.PhotoSize
import com.idphoto.app.processing.PhotoSizeManager
import com.idphoto.app.ui.LocalStrings
import com.idphoto.app.ui.localizedBgName
import com.idphoto.app.ui.localizedSizeDescription
import com.idphoto.app.ui.components.GradientButton
import com.idphoto.app.ui.components.SurfaceIconButton
import com.idphoto.app.ui.theme.LocalAppColors

/**
 * Save options data class passed back to ViewModel.
 */
data class SaveOptions(
    val printCopies: Int,
    val includePrintLayout: Boolean,
    val outputFormat: String, // "PNG" or "JPEG"
    val photoDpi: Int = 300,  // DPI for output quality
)

/**
 * Edit Screen — matching mockup.
 *
 * Photo preview with background color picker,
 * horizontal scrollable tool buttons row.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    photo: Bitmap?,
    selectedBgColor: Color,
    selectedBgIndex: Int = 0,
    activeEditTool: EditTool? = null,
    brightnessLevel: Float = 0f,
    pipelineRunId: Int = 0,
    selectedSize: PhotoSize = PhotoSizeManager.standardSizes[1],
    photoDpi: Int = 300,
    isSaving: Boolean = false,
    onBack: () -> Unit,
    onSave: (scale: Float, offsetX: Float, offsetY: Float, frameWidth: Int, frameHeight: Int, saveOptions: SaveOptions) -> Unit,
    onBgColorSelected: (Color) -> Unit,
    onBgOptionSelected: (Int) -> Unit = {},
    onToolClick: (EditTool) -> Unit,
    onBrightnessLevelChanged: (Float) -> Unit = {},
    onCloseToolPanel: () -> Unit = {},
    onNavigateToPrint: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = LocalAppColors.current
    val haptic = LocalHapticFeedback.current

    // ── State for drag & zoom — declared before UI so Save button can access ──
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var frameWidthPx by remember { mutableIntStateOf(0) }
    var frameHeightPx by remember { mutableIntStateOf(0) }

    // ── Save options dialog state ──
    var showSaveDialog by remember { mutableStateOf(false) }

    // ── Back confirmation dialog state ──
    var showBackConfirmDialog by remember { mutableStateOf(false) }

    // Reset when a new photo is loaded (new pipeline run), NOT when bg/brightness changes
    LaunchedEffect(pipelineRunId) {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    // ── Save Options Dialog ──
    if (showSaveDialog) {
        SaveOptionsDialog(
            currentSize = selectedSize,
            currentDpi = photoDpi,
            onDismiss = { showSaveDialog = false },
            onConfirm = { saveOptions ->
                showSaveDialog = false
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onSave(scale, offsetX, offsetY, frameWidthPx, frameHeightPx, saveOptions)
            },
        )
    }

    // ── Back Confirmation Dialog ──
    if (showBackConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showBackConfirmDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(32.dp),
                )
            },
            title = { Text(strings.back, fontWeight = FontWeight.Bold) },
            text = { Text(strings.backConfirmMessage) },
            confirmButton = {
                Button(
                    onClick = {
                        showBackConfirmDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.error),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(strings.backConfirmYes)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackConfirmDialog = false }) {
                    Text(strings.cancel)
                }
            },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        // ── Top Bar ──
        Surface(
            modifier = Modifier
                .fillMaxWidth(),
            color = colors.surface,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 14.dp, start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SurfaceIconButton(
                    icon = Icons.Default.ArrowBackIosNew,
                    contentDescription = strings.back,
                    onClick = { showBackConfirmDialog = true },
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    strings.editTitle,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.onSurface,
                    letterSpacing = (-0.3).sp,
                    modifier = Modifier.weight(1f),
                )
                GradientButton(
                    text = strings.save,
                    onClick = { showSaveDialog = true },
                    icon = Icons.Default.Save,
                )
            }
        }

        // ── Photo Preview with drag & zoom to position subject ──

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            // Tính kích thước khung preview theo tỷ lệ selectedSize
            // Giữ max 200dp chiều rộng hoặc 280dp chiều cao, tuỳ tỷ lệ nào chặt hơn
            val sizeAspect = selectedSize.widthMm / selectedSize.heightMm
            val maxFrameWidth = 200.dp
            val maxFrameHeight = 280.dp
            val frameWidth: androidx.compose.ui.unit.Dp
            val frameHeight: androidx.compose.ui.unit.Dp
            if (sizeAspect >= 1f) {
                // Landscape or square
                frameWidth = maxFrameWidth
                frameHeight = (maxFrameWidth.value / sizeAspect).dp
                    .coerceAtMost(maxFrameHeight)
            } else {
                // Portrait
                frameHeight = maxFrameHeight
                frameWidth = (maxFrameHeight.value * sizeAspect).dp
                    .coerceAtMost(maxFrameWidth)
            }

            // Photo frame — clipped container (tỷ lệ khớp với selectedSize)
            // Add bottom padding to reserve space for the background/brightness panel
            // anchored at BottomCenter, so the frame never gets covered.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 44.dp, bottom = 200.dp),
            ) {
            Box(
                modifier = Modifier
                    .width(frameWidth)
                    .height(frameHeight)
                    .shadow(12.dp, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .then(
                        run {
                            val bgOpt = ImageUtils.backgroundOptions.getOrNull(selectedBgIndex)
                            if (bgOpt?.gradientColors != null) {
                                val composeColors = bgOpt.gradientColors.map { Color(it.toLong() or 0xFF000000L) }
                                val brush = when (bgOpt.gradientType) {
                                    ImageUtils.GradientType.RADIAL -> Brush.radialGradient(composeColors)
                                    ImageUtils.GradientType.LINEAR_LEFT_RIGHT -> Brush.horizontalGradient(composeColors)
                                    ImageUtils.GradientType.LINEAR_DIAGONAL -> Brush.linearGradient(composeColors)
                                    else -> Brush.verticalGradient(composeColors)
                                }
                                Modifier.background(brush)
                            } else {
                                Modifier.background(selectedBgColor)
                            }
                        }
                    )
                    .onSizeChanged { size ->
                        frameWidthPx = size.width
                        frameHeightPx = size.height
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.5f, 3f)
                            // Cho phép kéo tự do trong vùng rộng
                            val maxX = size.width * 0.8f
                            val maxY = size.height * 0.8f
                            offsetX = (offsetX + pan.x).coerceIn(-maxX, maxX)
                            offsetY = (offsetY + pan.y).coerceIn(-maxY, maxY)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (photo != null) {
                    Image(
                        bitmap = photo.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offsetX
                                translationY = offsetY
                                transformOrigin = TransformOrigin(0.5f, 1f)
                            },
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.BottomCenter,
                    )
                } else {
                    // Placeholder
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(width = 60.dp, height = 74.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.4f)),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .size(width = 96.dp, height = 48.dp)
                                .clip(RoundedCornerShape(topStart = 48.dp, topEnd = 48.dp))
                                .background(Color.White.copy(alpha = 0.4f)),
                        )
                    }
                }
            }

            }  // end Column photo frame

            // ── Top overlay row: [size badge] [gesture hint] [reset] ──
            // Arranged in a single Row so they never overlap each other.
            val isAdjusted = scale != 1f || offsetX != 0f || offsetY != 0f
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Size badge (left)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.55f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            Icons.Default.CropPortrait,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            "${selectedSize.name} (${selectedSize.displaySize})",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                        )
                    }
                }

                // Gesture hint (center, flexible — shrinks before overlapping)
                if (photo != null) {
                    val hintAlpha = if (isAdjusted) 0.45f else 0.9f
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .alpha(hintAlpha),
                        shape = RoundedCornerShape(10.dp),
                        color = colors.onSurface.copy(alpha = 0.06f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                Icons.Default.ZoomOutMap,
                                contentDescription = null,
                                tint = colors.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.size(11.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                strings.pinchToZoomHint,
                                fontSize = 9.sp,
                                color = colors.onSurface.copy(alpha = 0.6f),
                                maxLines = 1,
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Reset button (right) — only when adjusted
                if (isAdjusted) {
                    Surface(
                        onClick = {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Black.copy(alpha = 0.5f),
                    ) {
                        Icon(
                            Icons.Default.RestartAlt,
                            contentDescription = strings.contentReset,
                            tint = Color.White,
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                .size(14.dp),
                        )
                    }
                }
            }

            // Background color picker — show when CHANGE_BG tool is active or no tool active
            androidx.compose.animation.AnimatedVisibility(
                visible = activeEditTool == null || activeEditTool == EditTool.CHANGE_BG,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
            ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.surface,
                shadowElevation = 4.dp,
            ) {
                var bgTab by remember { mutableIntStateOf(0) }
                val bgTabs = listOf(strings.bgTabSolid, strings.bgTabGradient, strings.bgTabStudio)
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    // Category tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        bgTabs.forEachIndexed { idx, tab ->
                            Surface(
                                onClick = { bgTab = idx },
                                shape = RoundedCornerShape(12.dp),
                                color = if (bgTab == idx) colors.primary else colors.background,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    tab,
                                    fontSize = 11.sp,
                                    fontWeight = if (bgTab == idx) FontWeight.Bold else FontWeight.Normal,
                                    color = if (bgTab == idx) Color.White else colors.textSecondary,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // Color swatches for current category
                    val currentCategory = when (bgTab) {
                        0 -> ImageUtils.BgCategory.SOLID
                        1 -> ImageUtils.BgCategory.GRADIENT
                        2 -> ImageUtils.BgCategory.STUDIO
                        else -> ImageUtils.BgCategory.SOLID
                    }
                    val filteredOptions = ImageUtils.backgroundOptions.withIndex()
                        .filter { it.value.category == currentCategory }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                    ) {
                        itemsIndexed(filteredOptions.toList()) { _, (globalIndex, option) ->
                            val isSelected = globalIndex == selectedBgIndex
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable {
                                    onBgOptionSelected(globalIndex)
                                    // Also call color for backward compatibility
                                    if (option.category == ImageUtils.BgCategory.SOLID) {
                                        onBgColorSelected(Color(option.displayColor))
                                    }
                                },
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .then(
                                            if (option.gradientColors != null) {
                                                val brush = when (option.gradientType) {
                                                    ImageUtils.GradientType.RADIAL -> Brush.radialGradient(
                                                        option.gradientColors.map { Color(it.toLong() or 0xFF000000L) }
                                                    )
                                                    else -> Brush.linearGradient(
                                                        option.gradientColors.map { Color(it.toLong() or 0xFF000000L) }
                                                    )
                                                }
                                                Modifier.background(brush)
                                            } else if (option.color == android.graphics.Color.TRANSPARENT) {
                                                Modifier.background(Color.White)
                                            } else {
                                                Modifier.background(Color(option.displayColor))
                                            }
                                        )
                                        .then(
                                            if (isSelected) Modifier.border(3.dp, colors.primary, CircleShape)
                                            else Modifier.border(1.dp, Color.Gray.copy(alpha = 0.2f), CircleShape)
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (option.color == android.graphics.Color.TRANSPARENT && option.gradientColors == null) {
                                        Icon(Icons.Default.GridOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                    }
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check, contentDescription = null,
                                            tint = if (option.displayColor == 0xFFFFFFFF.toLong() || option.displayColor == 0xFFF5F5F5.toLong() || option.displayColor == 0xFFE8E8E8.toLong())
                                                colors.primary else Color.White,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    strings.localizedBgName(option.name),
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) colors.primary else colors.textTertiary,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
            }  // end AnimatedVisibility for bg picker

            // ── Brightness Tool Panel ──
            androidx.compose.animation.AnimatedVisibility(
                visible = activeEditTool == EditTool.BRIGHTNESS,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
            ) {
                BrightnessToolPanel(
                    brightnessLevel = brightnessLevel,
                    onBrightnessChanged = onBrightnessLevelChanged,
                    onClose = onCloseToolPanel,
                )
            }
        }

        // ── Edit Tools ──
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = colors.surfaceContainerLow,
            shadowElevation = 8.dp,
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                EditToolButton(Icons.Default.Palette, strings.tColor, activeEditTool == EditTool.CHANGE_BG) { onToolClick(EditTool.CHANGE_BG) }
                EditToolButton(Icons.Default.WbSunny, strings.tBright, activeEditTool == EditTool.BRIGHTNESS) { onToolClick(EditTool.BRIGHTNESS) }
                EditToolButton(Icons.AutoMirrored.Filled.RotateRight, strings.tRotate, false) { onToolClick(EditTool.ROTATE) }
                EditToolButton(Icons.Default.Print, strings.tPrint, false) { onNavigateToPrint() }
            }
        }
    } // end Column

    // ── Saving overlay ──
    if (isSaving) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(enabled = false) { /* block touches */ },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    strings.saving,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
            }
        }
    }
    } // end Box
}

// ─── Tool Panels ───────────────────────────────────

@Composable
private fun BrightnessToolPanel(
    brightnessLevel: Float,
    onBrightnessChanged: (Float) -> Unit,
    onClose: () -> Unit,
) {
    val colors = LocalAppColors.current
    val strings = LocalStrings.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surface,
        shadowElevation = 4.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    strings.brightnessTitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = strings.contentClose, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.BrightnessLow, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(22.dp))
                Slider(
                    value = brightnessLevel,
                    onValueChange = onBrightnessChanged,
                    valueRange = -1f..1f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary),
                )
                Icon(Icons.Default.BrightnessHigh, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(22.dp))
            }
            Text(
                when {
                    brightnessLevel > 0.01f -> "+${(brightnessLevel * 100).toInt()}%"
                    brightnessLevel < -0.01f -> "${(brightnessLevel * 100).toInt()}%"
                    else -> strings.brightnessOriginal
                },
                fontSize = 12.sp,
                color = colors.textTertiary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun EditToolButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp)
            .width(64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isActive) colors.primaryContainer else colors.surfaceContainer)
                .then(
                    if (isActive) Modifier.border(1.dp, colors.primary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    else Modifier
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier.size(22.dp),
                tint = if (isActive) colors.primary else colors.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            label,
            fontSize = 10.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive) colors.primary else colors.onSurfaceVariant,
        )
    }
}

enum class EditTool {
    CHANGE_BG, BRIGHTNESS, ROTATE
}

// ─── Save Options Dialog ───────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SaveOptionsDialog(
    currentSize: PhotoSize,
    currentDpi: Int = 300,
    onDismiss: () -> Unit,
    onConfirm: (SaveOptions) -> Unit,
) {
    val strings = LocalStrings.current
    val colors = LocalAppColors.current

    var printCopies by remember { mutableIntStateOf(9) }
    var includePrintLayout by remember { mutableStateOf(true) }
    var outputFormat by remember { mutableStateOf("PNG") }
    var selectedDpi by remember { mutableIntStateOf(currentDpi) }

    val photosPerSheet = PhotoSizeManager.getPhotosPerSheet(currentSize)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.SaveAlt,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    strings.saveOptionsTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // ── Current Photo Size (read-only info) ──
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.background,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.PhotoSizeSelectLarge,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                strings.savePhotoSize,
                                fontSize = 11.sp,
                                color = colors.textTertiary,
                            )
                            Text(
                                "${currentSize.name} — ${strings.localizedSizeDescription(currentSize.description)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = colors.textPrimary,
                            )
                            Text(
                                currentSize.pixelSizeAtDpi(selectedDpi) + " @ ${selectedDpi} DPI",
                                fontSize = 11.sp,
                                color = colors.textTertiary,
                            )
                        }
                    }
                }

                // ── DPI Quality Selector ──
                Text(
                    strings.sQuality,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = colors.textPrimary,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    data class DpiOption(val dpi: Int, val label: String, val desc: String)
                    val dpiOptions = listOf(
                        DpiOption(150, "150", strings.dpiDraft),
                        DpiOption(200, "200", strings.dpiOk),
                        DpiOption(300, "300", strings.dpiHd),
                        DpiOption(450, "450", strings.dpiPro),
                        DpiOption(600, "600", strings.dpiUltra),
                    )
                    dpiOptions.forEach { option ->
                        val isSelected = selectedDpi == option.dpi
                        Surface(
                            onClick = { selectedDpi = option.dpi },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) colors.primary.copy(alpha = 0.15f) else colors.background,
                            border = if (isSelected)
                                ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                    brush = Brush.linearGradient(listOf(colors.primary, colors.primary))
                                ) else null,
                            modifier = Modifier.weight(1f),
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    "${option.dpi} ${strings.dpi}",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = if (isSelected) colors.primary else colors.textSecondary,
                                )
                                Text(
                                    strings.dpiLabel,
                                    fontSize = 9.sp,
                                    color = colors.textTertiary,
                                )
                                Text(
                                    option.desc,
                                    fontSize = 9.sp,
                                    color = if (isSelected) colors.primary else colors.textTertiary,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }

                // ── Include Print Layout Toggle ──
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.background,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.GridView,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                strings.saveWithPrintLayout,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary,
                            )
                            Text(
                                strings.savePhotosPerSheet.replace("%d", photosPerSheet.toString()),
                                fontSize = 11.sp,
                                color = colors.textTertiary,
                            )
                        }
                        Switch(
                            checked = includePrintLayout,
                            onCheckedChange = { includePrintLayout = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = colors.primary),
                        )
                    }
                }

                // ── Print Copies (only visible when print layout enabled) ──
                if (includePrintLayout) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.background,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                strings.savePrintCopies,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary,
                                modifier = Modifier.weight(1f),
                            )
                            // Stepper
                            Surface(
                                onClick = { if (printCopies > 1) printCopies-- },
                                shape = RoundedCornerShape(8.dp),
                                border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                                modifier = Modifier.size(32.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("−", fontSize = 16.sp, color = colors.textPrimary)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "$printCopies",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.primary,
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Surface(
                                onClick = { if (printCopies < 50) printCopies++ },
                                shape = RoundedCornerShape(8.dp),
                                border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                                modifier = Modifier.size(32.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("+", fontSize = 16.sp, color = colors.textPrimary)
                                }
                            }
                        }
                    }
                }

                // ── Output Format ──
                Text(
                    strings.saveFormat,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = colors.textPrimary,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    listOf(strings.formatPng, strings.formatJpeg).forEach { format ->
                        val isSelected = outputFormat == format
                        Surface(
                            onClick = { outputFormat = format },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) colors.primary.copy(alpha = 0.15f) else colors.background,
                            border = if (isSelected)
                                ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                    brush = Brush.linearGradient(listOf(colors.primary, colors.primary))
                                ) else null,
                            modifier = Modifier.weight(1f),
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Icon(
                                    if (format == strings.formatPng) Icons.Default.Image else Icons.Default.Photo,
                                    contentDescription = null,
                                    tint = if (isSelected) colors.primary else colors.textSecondary,
                                    modifier = Modifier.size(24.dp),
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    format,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = if (isSelected) colors.primary else colors.textSecondary,
                                )
                                Text(
                                    if (format == strings.formatPng) strings.formatHighQuality else strings.formatLightweight,
                                    fontSize = 10.sp,
                                    color = colors.textTertiary,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        SaveOptions(
                            printCopies = printCopies,
                            includePrintLayout = includePrintLayout,
                            outputFormat = outputFormat,
                            photoDpi = selectedDpi,
                        )
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.save, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        },
    )
}
