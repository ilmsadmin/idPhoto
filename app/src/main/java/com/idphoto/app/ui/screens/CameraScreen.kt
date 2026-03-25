package com.idphoto.app.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.idphoto.app.processing.QualityChecker
import com.idphoto.app.ui.LocalStrings
import kotlinx.coroutines.delay
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * New Camera Screen — matching mockup design.
 *
 * Dark background, CameraX viewfinder with:
 * - Top bar: Close, size tag, flash
 * - Face/body guide with dashed outline + corner markers + AI detect badge
 * - Tips row (lighting, face, background)
 * - Bottom: gallery, shutter, flip
 */
/**
 * Real-time face quality check state for camera tips.
 */
private data class LiveQualityState(
    val faceDetected: Boolean = false,
    val faceStraight: Boolean = false,
    val eyesOpen: Boolean = false,
    val goodLighting: Boolean = false,
    val goodBackground: Boolean = false,
    val hint: String = "",
)

@Composable
fun CameraScreen(
    selectedSizeName: String,
    onPhotoCaptured: (Bitmap) -> Unit,
    onDismiss: () -> Unit,
    onChangeSizeClick: () -> Unit,
    onGalleryClick: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val strings = LocalStrings.current

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_FRONT) }
    val imageCapture = remember { mutableStateOf<ImageCapture?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var flashEnabled by remember { mutableStateOf(false) }

    // Real-time quality analysis state
    var liveQuality by remember { mutableStateOf(LiveQualityState()) }
    val qualityChecker = remember { QualityChecker() }
    val isAnalyzing = remember { AtomicBoolean(false) }

    // Countdown timer state
    var timerSeconds by remember { mutableIntStateOf(0) } // 0 = off, 3 or 5
    var countdownActive by remember { mutableStateOf(false) }
    var countdownValue by remember { mutableIntStateOf(0) }

    // Capture flash animation
    var showFlash by remember { mutableStateOf(false) }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    // Countdown logic
    LaunchedEffect(countdownActive, countdownValue) {
        if (countdownActive && countdownValue > 0) {
            delay(1000)
            countdownValue -= 1
        } else if (countdownActive && countdownValue == 0) {
            countdownActive = false
            // Trigger capture
            showFlash = true
            isCapturing = true
            capturePhoto(
                context = context,
                imageCapture = imageCapture.value,
                isFrontCamera = lensFacing == CameraSelector.LENS_FACING_FRONT,
                flashEnabled = flashEnabled,
                onCaptured = { bitmap ->
                    isCapturing = false
                    showFlash = false
                    onPhotoCaptured(bitmap)
                },
                onError = {
                    isCapturing = false
                    showFlash = false
                },
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .statusBarsPadding(),
        ) {
        // ── Top Bar — nằm trên nền đen, dưới status bar ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Close button
            Surface(
                onClick = onDismiss,
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            // Size tag
            Surface(
                onClick = onChangeSizeClick,
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.15f),
            ) {
                Text(
                    "$selectedSizeName ▾",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            // Flash button
            Surface(
                onClick = { flashEnabled = !flashEnabled },
                shape = CircleShape,
                color = if (flashEnabled) Color(0xFFFFB300).copy(alpha = 0.3f)
                        else Color.White.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Flash",
                        tint = if (flashEnabled) Color(0xFFFFB300) else Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }

        // ── Viewfinder Area — camera preview có clip bo góc ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 4.dp)
                .clip(RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            // Camera Preview with ImageAnalysis
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PreviewView(ctx).also { previewView ->
                        previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
                        bindCameraWithAnalysis(
                            context = ctx,
                            previewView = previewView,
                            lifecycleOwner = lifecycleOwner as androidx.lifecycle.LifecycleOwner,
                            cameraProviderFuture = cameraProviderFuture,
                            lensFacing = lensFacing,
                            flashEnabled = flashEnabled,
                            imageCapture = imageCapture,
                            qualityChecker = qualityChecker,
                            isAnalyzing = isAnalyzing,
                            onQualityUpdate = { result ->
                                liveQuality = result
                            },
                        )
                    }
                },
                update = { previewView ->
                    bindCameraWithAnalysis(
                        context = context,
                        previewView = previewView,
                        lifecycleOwner = lifecycleOwner as androidx.lifecycle.LifecycleOwner,
                        cameraProviderFuture = cameraProviderFuture,
                        lensFacing = lensFacing,
                        flashEnabled = flashEnabled,
                        imageCapture = imageCapture,
                        qualityChecker = qualityChecker,
                        isAnalyzing = isAnalyzing,
                        onQualityUpdate = { result ->
                            liveQuality = result
                        },
                    )
                },
            )

            // Face/body guide overlay with dynamic color based on detection
            FaceGuideOverlay(
                guideTxt = if (liveQuality.faceDetected) {
                    if (liveQuality.faceStraight && liveQuality.eyesOpen) strings.guideTxt
                    else liveQuality.hint.ifEmpty { strings.guideTxt }
                } else {
                    strings.guideTxt
                },
                faceDetected = liveQuality.faceDetected,
                faceOk = liveQuality.faceDetected && liveQuality.faceStraight && liveQuality.eyesOpen,
            )

            // AI Detect badge — shows live status
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                shape = RoundedCornerShape(16.dp),
                color = if (liveQuality.faceDetected)
                    Color(0xFF1565C0).copy(alpha = 0.85f)
                else
                    Color(0xFF424242).copy(alpha = 0.85f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (liveQuality.faceDetected) Icons.Default.Person else Icons.Default.PersonSearch,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (liveQuality.faceDetected) "AI Detect" else "Scanning...",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            // Tips row — overlaid at bottom of viewfinder
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            ) {
                TipChip(
                    text = strings.tipLight,
                    isOk = liveQuality.goodLighting,
                    modifier = Modifier.weight(1f),
                )
                TipChip(
                    text = strings.tipFace,
                    isOk = liveQuality.faceDetected && liveQuality.faceStraight,
                    modifier = Modifier.weight(1f),
                )
                TipChip(
                    text = strings.tipBg,
                    isOk = liveQuality.goodBackground,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // ── Bottom Controls — nền đen cố định dưới camera ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 8.dp)
                .navigationBarsPadding(),
        ) {
            // Shutter / gallery / flip row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                // Gallery button
                Surface(
                    onClick = onGalleryClick,
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier.size(48.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = "Gallery",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                // Shutter button with animation
                val shutterScale by animateFloatAsState(
                    targetValue = if (isCapturing) 0.85f else 1f,
                    animationSpec = tween(100),
                    label = "shutter_scale",
                )
                Button(
                    onClick = {
                        if (!isCapturing && !countdownActive) {
                            if (timerSeconds > 0) {
                                countdownValue = timerSeconds
                                countdownActive = true
                            } else {
                                showFlash = true
                                isCapturing = true
                                capturePhoto(
                                    context = context,
                                    imageCapture = imageCapture.value,
                                    isFrontCamera = lensFacing == CameraSelector.LENS_FACING_FRONT,
                                    flashEnabled = flashEnabled,
                                    onCaptured = { bitmap ->
                                        isCapturing = false
                                        showFlash = false
                                        onPhotoCaptured(bitmap)
                                    },
                                    onError = {
                                        isCapturing = false
                                        showFlash = false
                                    },
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .size(72.dp)
                        .scale(shutterScale),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    contentPadding = PaddingValues(0.dp),
                    enabled = !isCapturing && !countdownActive,
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(if (isCapturing) Color.LightGray else Color.White),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isCapturing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 3.dp,
                                color = Color.Gray,
                            )
                        }
                    }
                }

                // Flip camera button
                Surface(
                    onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT)
                            CameraSelector.LENS_FACING_BACK
                        else CameraSelector.LENS_FACING_FRONT
                    },
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier.size(48.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.FlipCameraAndroid,
                            contentDescription = "Flip",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }

            // Timer selector row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TimerOption("OFF", timerSeconds == 0) { timerSeconds = 0 }
                Spacer(modifier = Modifier.width(16.dp))
                TimerOption("3s", timerSeconds == 3) { timerSeconds = 3 }
                Spacer(modifier = Modifier.width(16.dp))
                TimerOption("5s", timerSeconds == 5) { timerSeconds = 5 }
            }
        }
    }

        // Capture flash overlay
        AnimatedVisibility(
            visible = showFlash,
            enter = fadeIn(animationSpec = tween(50)),
            exit = fadeOut(animationSpec = tween(200)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White),
            )
        }

        // Countdown overlay
        if (countdownActive && countdownValue > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "$countdownValue",
                    color = Color.White,
                    fontSize = 96.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    } // Box
}

/**
 * Timer option chip for countdown selection.
 */
@Composable
private fun TimerOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Color.White.copy(alpha = 0.25f) else Color.Transparent,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (label != "OFF") {
                Icon(
                    Icons.Default.Timer,
                    contentDescription = null,
                    tint = if (selected) Color.White else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                label,
                color = if (selected) Color.White else Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}

/**
 * Face and body guide overlay with dashed outline and corner markers.
 * Colors adapt based on real-time face detection.
 */
@Composable
private fun FaceGuideOverlay(
    guideTxt: String,
    faceDetected: Boolean = false,
    faceOk: Boolean = false,
) {
    val cornerColor = when {
        faceOk -> Color(0xFF81C784)       // Green when face detected & OK
        faceDetected -> Color(0xFFFFB74D)  // Orange when face detected but not straight
        else -> Color(0xFF4FC3F7)          // Blue default (no face)
    }
    val guideAlpha = if (faceDetected) 0.5f else 0.4f

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cw = size.width
            val ch = size.height

            // Guide dimensions
            val guideW = cw * 0.52f
            val guideH = guideW * 1.3f
            val gx = (cw - guideW) / 2f
            val gy = (ch - guideH) / 2f

            // Dashed face/body outline
            val dash = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
            drawRoundRect(
                color = Color.White.copy(alpha = guideAlpha),
                topLeft = Offset(gx, gy),
                size = Size(guideW, guideH),
                cornerRadius = CornerRadius(guideW * 0.5f, guideW * 0.38f),
                style = Stroke(width = 3f, pathEffect = dash),
            )

            // Face oval
            val faceW = guideW * 0.4f
            val faceH = faceW * 1.2f
            val fx = (cw - faceW) / 2f
            val fy = gy + guideH * 0.12f
            drawOval(
                color = if (faceDetected) cornerColor.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.25f),
                topLeft = Offset(fx, fy),
                size = Size(faceW, faceH),
                style = Stroke(width = 2.5f),
            )

            // Body arc
            val bodyW = guideW * 0.6f
            val bodyH = bodyW * 0.47f
            val bx = (cw - bodyW) / 2f
            val by = fy + faceH + guideH * 0.04f
            drawOval(
                color = Color.White.copy(alpha = 0.25f),
                topLeft = Offset(bx, by),
                size = Size(bodyW, bodyH),
                style = Stroke(width = 2.5f),
            )

            // Corner markers (4 corners)
            val cornerLen = 24.dp.toPx()
            val cornerW = 3.dp.toPx()
            val margin = 8.dp.toPx()
            val cx1 = gx - margin
            val cy1 = gy - margin
            val cx2 = gx + guideW + margin
            val cy2 = gy + guideH + margin

            // Top-left
            drawLine(cornerColor, Offset(cx1, cy1), Offset(cx1 + cornerLen, cy1), cornerW, StrokeCap.Round)
            drawLine(cornerColor, Offset(cx1, cy1), Offset(cx1, cy1 + cornerLen), cornerW, StrokeCap.Round)
            // Top-right
            drawLine(cornerColor, Offset(cx2, cy1), Offset(cx2 - cornerLen, cy1), cornerW, StrokeCap.Round)
            drawLine(cornerColor, Offset(cx2, cy1), Offset(cx2, cy1 + cornerLen), cornerW, StrokeCap.Round)
            // Bottom-left
            drawLine(cornerColor, Offset(cx1, cy2), Offset(cx1 + cornerLen, cy2), cornerW, StrokeCap.Round)
            drawLine(cornerColor, Offset(cx1, cy2), Offset(cx1, cy2 - cornerLen), cornerW, StrokeCap.Round)
            // Bottom-right
            drawLine(cornerColor, Offset(cx2, cy2), Offset(cx2 - cornerLen, cy2), cornerW, StrokeCap.Round)
            drawLine(cornerColor, Offset(cx2, cy2), Offset(cx2, cy2 - cornerLen), cornerW, StrokeCap.Round)
        }

        // Guide text below
        Text(
            guideTxt,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 120.dp),
        )
    }
}

@Composable
private fun TipChip(text: String, isOk: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.1f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                if (isOk) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (isOk) Color(0xFF81C784) else Color(0xFFFFB74D),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp,
                maxLines = 1,
            )
        }
    }
}

// ── Camera helpers ──

/**
 * Bind camera with Preview + ImageCapture + ImageAnalysis for real-time face quality.
 */
private fun bindCameraWithAnalysis(
    context: Context,
    previewView: PreviewView,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    cameraProviderFuture: com.google.common.util.concurrent.ListenableFuture<ProcessCameraProvider>,
    lensFacing: Int,
    flashEnabled: Boolean,
    imageCapture: MutableState<ImageCapture?>,
    qualityChecker: QualityChecker,
    isAnalyzing: AtomicBoolean,
    onQualityUpdate: (LiveQualityState) -> Unit,
) {
    try {
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }
        val capture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setFlashMode(
                if (flashEnabled) ImageCapture.FLASH_MODE_ON
                else ImageCapture.FLASH_MODE_OFF
            )
            .build()
        imageCapture.value = capture

        // Image Analysis for real-time face quality check
        val analysisExecutor = Executors.newSingleThreadExecutor()
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()

        imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
            if (isAnalyzing.compareAndSet(false, true)) {
                try {
                    val bitmap = imageProxyToBitmap(imageProxy)
                    if (bitmap != null) {
                        qualityChecker.quickCheck(bitmap) { result ->
                            ContextCompat.getMainExecutor(context).execute {
                                onQualityUpdate(
                                    LiveQualityState(
                                        faceDetected = result.faceDetected,
                                        faceStraight = result.faceStraight,
                                        eyesOpen = result.eyesOpen,
                                        goodLighting = result.goodLighting,
                                        goodBackground = result.goodBackground,
                                        hint = result.hint,
                                    )
                                )
                            }
                            bitmap.recycle()
                            isAnalyzing.set(false)
                        }
                    } else {
                        isAnalyzing.set(false)
                    }
                } catch (e: Exception) {
                    isAnalyzing.set(false)
                }
            }
            imageProxy.close()
        }

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner, cameraSelector, preview, capture, imageAnalysis
        )
    } catch (e: Exception) {
        Log.e("CameraScreen", "Failed to bind camera", e)
    }
}

/**
 * Convert ImageProxy (RGBA_8888) to Bitmap for analysis.
 */
private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
    return try {
        val planes = imageProxy.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * imageProxy.width

        val bitmap = Bitmap.createBitmap(
            imageProxy.width + rowPadding / pixelStride,
            imageProxy.height,
            Bitmap.Config.ARGB_8888,
        )
        buffer.rewind()
        bitmap.copyPixelsFromBuffer(buffer)

        // Crop to actual size if there's padding
        if (rowPadding > 0) {
            val cropped = Bitmap.createBitmap(bitmap, 0, 0, imageProxy.width, imageProxy.height)
            bitmap.recycle()
            cropped
        } else {
            bitmap
        }
    } catch (e: Exception) {
        Log.e("CameraScreen", "Failed to convert ImageProxy to Bitmap", e)
        null
    }
}

private fun capturePhoto(
    context: Context,
    imageCapture: ImageCapture?,
    isFrontCamera: Boolean,
    flashEnabled: Boolean = false,
    onCaptured: (Bitmap) -> Unit,
    onError: () -> Unit,
) {
    val capture = imageCapture ?: run {
        onError()
        return
    }

    // Set flash mode
    capture.flashMode = if (flashEnabled) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF

    val photoFile = File(context.cacheDir, "captured_${System.currentTimeMillis()}.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
    val executor = Executors.newSingleThreadExecutor()

    capture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                if (bitmap != null) {
                    val finalBitmap = if (isFrontCamera) {
                        val matrix = Matrix().apply { preScale(-1f, 1f) }
                        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    } else {
                        bitmap
                    }
                    ContextCompat.getMainExecutor(context).execute {
                        onCaptured(finalBitmap)
                    }
                } else {
                    ContextCompat.getMainExecutor(context).execute { onError() }
                }
                photoFile.delete()
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("CameraScreen", "Photo capture failed", exception)
                ContextCompat.getMainExecutor(context).execute { onError() }
                photoFile.delete()
            }
        },
    )
}
