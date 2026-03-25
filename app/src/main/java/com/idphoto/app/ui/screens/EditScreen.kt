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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idphoto.app.processing.ImageUtils
import com.idphoto.app.processing.PhotoSize
import com.idphoto.app.processing.PhotoSizeManager
import com.idphoto.app.ui.LocalStrings
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

    // ── State for drag & zoom — declared before UI so Save button can access ──
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var frameWidthPx by remember { mutableIntStateOf(0) }
    var frameHeightPx by remember { mutableIntStateOf(0) }

    // ── Save options dialog state ──
    var showSaveDialog by remember { mutableStateOf(false) }

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
                onSave(scale, offsetX, offsetY, frameWidthPx, frameHeightPx, saveOptions)
            },
        )
    }

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
                    strings.editTitle,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = { showSaveDialog = true },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                    ),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                ) {
                    Icon(
                        Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.save, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
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
                            },
                        contentScale = ContentScale.Fit,
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

            // Reset button — only show when adjusted
            if (scale != 1f || offsetX != 0f || offsetY != 0f) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp),
                    onClick = {
                        scale = 1f
                        offsetX = 0f
                        offsetY = 0f
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.5f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            Icons.Default.RestartAlt,
                            contentDescription = "Reset",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }

            // Size info badge — hiển thị cỡ ảnh đang edit
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 8.dp, start = 8.dp),
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
                val bgTabs = listOf("Đơn sắc", "Gradient", "Studio")
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
                                    option.name,
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
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            color = colors.surface,
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                EditToolButton(Icons.Default.Palette, strings.tColor, activeEditTool == EditTool.CHANGE_BG) { onToolClick(EditTool.CHANGE_BG) }
                EditToolButton(Icons.Default.WbSunny, strings.tBright, activeEditTool == EditTool.BRIGHTNESS) { onToolClick(EditTool.BRIGHTNESS) }
                EditToolButton(Icons.AutoMirrored.Filled.RotateRight, strings.tRotate, false) { onToolClick(EditTool.ROTATE) }
                EditToolButton(Icons.Default.Print, strings.tPrint, false) { onNavigateToPrint() }
            }
        }
    }
}

// ─── Tool Panels ───────────────────────────────────

@Composable
private fun BrightnessToolPanel(
    brightnessLevel: Float,
    onBrightnessChanged: (Float) -> Unit,
    onClose: () -> Unit,
) {
    val colors = LocalAppColors.current
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
                    "Độ sáng",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
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
                    else -> "Gốc"
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
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 10.dp)
            .width(56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isActive) colors.primary.copy(alpha = 0.15f) else colors.background),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier.size(22.dp),
                tint = if (isActive) colors.primary else colors.textSecondary,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            label,
            fontSize = 10.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive) colors.primary else colors.textSecondary,
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
                                "${currentSize.name} — ${currentSize.description}",
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
                        DpiOption(150, "150", "Draft"),
                        DpiOption(200, "200", "OK"),
                        DpiOption(300, "300", "HD ★"),
                        DpiOption(450, "450", "Pro"),
                        DpiOption(600, "600", "Ultra"),
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
                                    "${option.dpi}",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = if (isSelected) colors.primary else colors.textSecondary,
                                )
                                Text(
                                    "DPI",
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
                    listOf("PNG", "JPEG").forEach { format ->
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
                                    if (format == "PNG") Icons.Default.Image else Icons.Default.Photo,
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
                                    if (format == "PNG") "Chất lượng cao" else "Nhẹ hơn",
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
