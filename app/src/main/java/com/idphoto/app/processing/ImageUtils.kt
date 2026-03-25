package com.idphoto.app.processing

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream

/**
 * Tiện ích ghép nền và lưu ảnh.
 */
object ImageUtils {

    /**
     * Các màu nền chuẩn cho ảnh thẻ
     */
    data class BackgroundOption(
        val name: String,
        val color: Int,             // Android Color — dùng cho solid
        val displayColor: Long,     // Compose Color ULong — hiển thị UI
        val category: BgCategory = BgCategory.SOLID,
        val gradientColors: List<Int>? = null,   // Android Colors for gradient
        val gradientType: GradientType = GradientType.NONE,
    )

    enum class BgCategory { SOLID, GRADIENT, STUDIO }
    enum class GradientType { NONE, LINEAR_TOP_BOTTOM, LINEAR_LEFT_RIGHT, LINEAR_DIAGONAL, RADIAL }

    // ── SOLID colors ──
    private val solidOptions = listOf(
        BackgroundOption("Trắng", Color.WHITE, 0xFFFFFFFF),
        BackgroundOption("Xanh dương", Color.parseColor("#0066CC"), 0xFF0066CC),
        BackgroundOption("Đỏ", Color.parseColor("#CC0000"), 0xFFCC0000),
        BackgroundOption("Xanh lá", Color.parseColor("#007A33"), 0xFF007A33),
        BackgroundOption("Xám nhạt", Color.parseColor("#E8E8E8"), 0xFFE8E8E8),
        BackgroundOption("Xanh nhạt", Color.parseColor("#D6EAF8"), 0xFFD6EAF8),
        BackgroundOption("Xanh navy", Color.parseColor("#1B2838"), 0xFF1B2838),
        BackgroundOption("Hồng nhạt", Color.parseColor("#FADBD8"), 0xFFFADBD8),
        BackgroundOption("Vàng nhạt", Color.parseColor("#FEF9E7"), 0xFFFEF9E7),
        BackgroundOption("Xám đậm", Color.parseColor("#5D6D7E"), 0xFF5D6D7E),
        BackgroundOption("Trong suốt", Color.TRANSPARENT, 0x00000000),
    )

    // ── GRADIENT colors ──
    private val gradientOptions = listOf(
        BackgroundOption(
            "Xanh trời", Color.TRANSPARENT, 0xFF87CEEB,
            BgCategory.GRADIENT,
            listOf(Color.parseColor("#87CEEB"), Color.parseColor("#1565C0")),
            GradientType.LINEAR_TOP_BOTTOM,
        ),
        BackgroundOption(
            "Hoàng hôn", Color.TRANSPARENT, 0xFFFF7043,
            BgCategory.GRADIENT,
            listOf(Color.parseColor("#FF7043"), Color.parseColor("#FF8A65"), Color.parseColor("#FFB74D")),
            GradientType.LINEAR_TOP_BOTTOM,
        ),
        BackgroundOption(
            "Tím hồng", Color.TRANSPARENT, 0xFFCE93D8,
            BgCategory.GRADIENT,
            listOf(Color.parseColor("#CE93D8"), Color.parseColor("#F48FB1")),
            GradientType.LINEAR_DIAGONAL,
        ),
        BackgroundOption(
            "Xanh mint", Color.TRANSPARENT, 0xFF80CBC4,
            BgCategory.GRADIENT,
            listOf(Color.parseColor("#80CBC4"), Color.parseColor("#4DB6AC"), Color.parseColor("#00897B")),
            GradientType.LINEAR_TOP_BOTTOM,
        ),
        BackgroundOption(
            "Xanh đậm", Color.TRANSPARENT, 0xFF1A237E,
            BgCategory.GRADIENT,
            listOf(Color.parseColor("#1A237E"), Color.parseColor("#283593"), Color.parseColor("#3949AB")),
            GradientType.LINEAR_TOP_BOTTOM,
        ),
        BackgroundOption(
            "Xám sang", Color.TRANSPARENT, 0xFFBDBDBD,
            BgCategory.GRADIENT,
            listOf(Color.parseColor("#ECEFF1"), Color.parseColor("#B0BEC5"), Color.parseColor("#78909C")),
            GradientType.LINEAR_TOP_BOTTOM,
        ),
        BackgroundOption(
            "Hồng pastel", Color.TRANSPARENT, 0xFFF8BBD0,
            BgCategory.GRADIENT,
            listOf(Color.parseColor("#F8BBD0"), Color.parseColor("#F48FB1")),
            GradientType.LINEAR_LEFT_RIGHT,
        ),
        BackgroundOption(
            "Vàng ấm", Color.TRANSPARENT, 0xFFFFD54F,
            BgCategory.GRADIENT,
            listOf(Color.parseColor("#FFF9C4"), Color.parseColor("#FFD54F"), Color.parseColor("#FFB300")),
            GradientType.LINEAR_TOP_BOTTOM,
        ),
    )

    // ── STUDIO styles ──
    private val studioOptions = listOf(
        BackgroundOption(
            "Studio xám", Color.TRANSPARENT, 0xFF9E9E9E,
            BgCategory.STUDIO,
            listOf(Color.parseColor("#E0E0E0"), Color.parseColor("#757575")),
            GradientType.RADIAL,
        ),
        BackgroundOption(
            "Studio xanh", Color.TRANSPARENT, 0xFF1565C0,
            BgCategory.STUDIO,
            listOf(Color.parseColor("#42A5F5"), Color.parseColor("#0D47A1")),
            GradientType.RADIAL,
        ),
        BackgroundOption(
            "Studio đen", Color.TRANSPARENT, 0xFF212121,
            BgCategory.STUDIO,
            listOf(Color.parseColor("#424242"), Color.parseColor("#111111")),
            GradientType.RADIAL,
        ),
        BackgroundOption(
            "Studio trắng", Color.TRANSPARENT, 0xFFF5F5F5,
            BgCategory.STUDIO,
            listOf(Color.parseColor("#FFFFFF"), Color.parseColor("#BDBDBD")),
            GradientType.RADIAL,
        ),
        BackgroundOption(
            "Studio tím", Color.TRANSPARENT, 0xFF7B1FA2,
            BgCategory.STUDIO,
            listOf(Color.parseColor("#BA68C8"), Color.parseColor("#4A148C")),
            GradientType.RADIAL,
        ),
        BackgroundOption(
            "Studio hồng", Color.TRANSPARENT, 0xFFE91E63,
            BgCategory.STUDIO,
            listOf(Color.parseColor("#F48FB1"), Color.parseColor("#880E4F")),
            GradientType.RADIAL,
        ),
        BackgroundOption(
            "Studio cam", Color.TRANSPARENT, 0xFFFF6D00,
            BgCategory.STUDIO,
            listOf(Color.parseColor("#FFB74D"), Color.parseColor("#E65100")),
            GradientType.RADIAL,
        ),
        BackgroundOption(
            "Studio xanh lá", Color.TRANSPARENT, 0xFF2E7D32,
            BgCategory.STUDIO,
            listOf(Color.parseColor("#81C784"), Color.parseColor("#1B5E20")),
            GradientType.RADIAL,
        ),
    )

    val backgroundOptions: List<BackgroundOption> = solidOptions + gradientOptions + studioOptions

    /**
     * Render ảnh cuối cùng theo đúng vị trí user đã kéo/zoom trong khung.
     * Mô phỏng lại ContentScale.Fit + graphicsLayer(scale, translationX, translationY).
     *
     * @param foreground  Ảnh đã xóa nền (có alpha)
     * @param bgColor     Màu nền Android (Color.rgb)
     * @param scale       Tỉ lệ zoom user đã chọn
     * @param offsetX     Pixel offset X trên UI frame
     * @param offsetY     Pixel offset Y trên UI frame
     * @param frameWidth  Chiều rộng khung hiển thị (pixels on screen)
     * @param frameHeight Chiều cao khung hiển thị (pixels on screen)
     * @param outputWidth Chiều rộng ảnh xuất (pixels). 0 = dùng tỉ lệ khung * DPI.
     * @param outputHeight Chiều cao ảnh xuất. 0 = auto.
     */
    fun renderFramedBitmap(
        foreground: Bitmap,
        bgColor: Int,
        scale: Float,
        offsetX: Float,
        offsetY: Float,
        frameWidth: Int,
        frameHeight: Int,
        outputWidth: Int = 0,
        outputHeight: Int = 0,
    ): Bitmap {
        // Target output size — match frame aspect ratio at foreground resolution
        val frameAspect = frameWidth.toFloat() / frameHeight.toFloat()
        val outW: Int
        val outH: Int
        if (outputWidth > 0 && outputHeight > 0) {
            outW = outputWidth
            outH = outputHeight
        } else {
            val fgMax = maxOf(foreground.width, foreground.height)
            if (frameAspect >= 1f) {
                outW = fgMax
                outH = (fgMax / frameAspect).toInt()
            } else {
                outH = fgMax
                outW = (fgMax * frameAspect).toInt()
            }
        }

        // Create output bitmap with background color
        val result = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        if (bgColor != Color.TRANSPARENT) {
            canvas.drawColor(bgColor)
        }


        // Mapping ratio: frame-pixel → output-pixel
        val ratioX = outW.toFloat() / frameWidth.toFloat()
        val ratioY = outH.toFloat() / frameHeight.toFloat()

        // Step 1: ContentScale.Fit — fit foreground into frame, centered
        val fitScale = minOf(
            frameWidth.toFloat() / foreground.width.toFloat(),
            frameHeight.toFloat() / foreground.height.toFloat()
        )
        val fittedW = foreground.width * fitScale
        val fittedH = foreground.height * fitScale
        // Top-left of fitted image in frame-space (centered)
        val fitX = (frameWidth - fittedW) / 2f
        val fitY = (frameHeight - fittedH) / 2f

        // Step 2: graphicsLayer scale around center of Image (= center of frame)
        // After scale, the fitted image is scaled around frame center
        val frameCx = frameWidth / 2f
        val frameCy = frameHeight / 2f
        // New top-left after scaling around center:
        val scaledFitX = frameCx + (fitX - frameCx) * scale
        val scaledFitY = frameCy + (fitY - frameCy) * scale

        // Step 3: graphicsLayer translation
        val finalFrameX = scaledFitX + offsetX
        val finalFrameY = scaledFitY + offsetY

        // Map to output-pixel space
        val outDrawX = finalFrameX * ratioX
        val outDrawY = finalFrameY * ratioY
        val outScaleX = fitScale * scale * ratioX
        val outScaleY = fitScale * scale * ratioY

        val matrix = Matrix().apply {
            postScale(outScaleX, outScaleY)
            postTranslate(outDrawX, outDrawY)
        }

        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
        }

        canvas.drawBitmap(foreground, matrix, paint)
        return result
    }

    /**
     * Render framed bitmap with gradient/studio background support.
     */
    fun renderFramedBitmapWithOption(
        foreground: Bitmap,
        bgOption: BackgroundOption,
        scale: Float,
        offsetX: Float,
        offsetY: Float,
        frameWidth: Int,
        frameHeight: Int,
        outputWidth: Int = 0,
        outputHeight: Int = 0,
    ): Bitmap {
        if (bgOption.category == BgCategory.SOLID || bgOption.gradientColors == null) {
            return renderFramedBitmap(foreground, bgOption.color, scale, offsetX, offsetY, frameWidth, frameHeight, outputWidth, outputHeight)
        }
        // Calculate output size
        val frameAspect = frameWidth.toFloat() / frameHeight.toFloat()
        val outW: Int
        val outH: Int
        if (outputWidth > 0 && outputHeight > 0) { outW = outputWidth; outH = outputHeight }
        else {
            val fgMax = maxOf(foreground.width, foreground.height)
            if (frameAspect >= 1f) { outW = fgMax; outH = (fgMax / frameAspect).toInt() }
            else { outH = fgMax; outW = (fgMax * frameAspect).toInt() }
        }
        val result = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Draw gradient background
        val bgPaint = Paint().apply { isAntiAlias = true }
        val colorsArray = bgOption.gradientColors.toIntArray()
        bgPaint.shader = when (bgOption.gradientType) {
            GradientType.LINEAR_TOP_BOTTOM -> LinearGradient(0f, 0f, 0f, outH.toFloat(), colorsArray, null, Shader.TileMode.CLAMP)
            GradientType.LINEAR_LEFT_RIGHT -> LinearGradient(0f, 0f, outW.toFloat(), 0f, colorsArray, null, Shader.TileMode.CLAMP)
            GradientType.LINEAR_DIAGONAL -> LinearGradient(0f, 0f, outW.toFloat(), outH.toFloat(), colorsArray, null, Shader.TileMode.CLAMP)
            GradientType.RADIAL -> RadialGradient(outW / 2f, outH * 0.4f, maxOf(outW, outH) * 0.8f, colorsArray, null, Shader.TileMode.CLAMP)
            GradientType.NONE -> null
        }
        if (bgPaint.shader != null) canvas.drawRect(0f, 0f, outW.toFloat(), outH.toFloat(), bgPaint)

        // Draw foreground with transform
        val ratioX = outW.toFloat() / frameWidth.toFloat()
        val ratioY = outH.toFloat() / frameHeight.toFloat()
        val fitScale = minOf(frameWidth.toFloat() / foreground.width.toFloat(), frameHeight.toFloat() / foreground.height.toFloat())
        val fittedW = foreground.width * fitScale
        val fittedH = foreground.height * fitScale
        val fitX = (frameWidth - fittedW) / 2f
        val fitY = (frameHeight - fittedH) / 2f
        val frameCx = frameWidth / 2f
        val frameCy = frameHeight / 2f
        val scaledFitX = frameCx + (fitX - frameCx) * scale
        val scaledFitY = frameCy + (fitY - frameCy) * scale
        val finalFrameX = scaledFitX + offsetX
        val finalFrameY = scaledFitY + offsetY
        val outDrawX = finalFrameX * ratioX
        val outDrawY = finalFrameY * ratioY
        val outScaleX = fitScale * scale * ratioX
        val outScaleY = fitScale * scale * ratioY
        val matrix = Matrix().apply { postScale(outScaleX, outScaleY); postTranslate(outDrawX, outDrawY) }
        val fgPaint = Paint().apply { isAntiAlias = true; isFilterBitmap = true }
        canvas.drawBitmap(foreground, matrix, fgPaint)
        return result
    }

    /**
     * Ghép ảnh đã xóa nền lên nền gradient/studio.
     */
    fun compositeOnGradientBackground(
        foreground: Bitmap,
        option: BackgroundOption,
    ): Bitmap {
        val width = foreground.width
        val height = foreground.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        val gradientColors = option.gradientColors ?: return compositeOnBackground(foreground, option.color)

        // Draw gradient background
        val paint = Paint().apply { isAntiAlias = true }
        val colorsArray = gradientColors.toIntArray()

        paint.shader = when (option.gradientType) {
            GradientType.LINEAR_TOP_BOTTOM -> LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                colorsArray, null, Shader.TileMode.CLAMP
            )
            GradientType.LINEAR_LEFT_RIGHT -> LinearGradient(
                0f, 0f, width.toFloat(), 0f,
                colorsArray, null, Shader.TileMode.CLAMP
            )
            GradientType.LINEAR_DIAGONAL -> LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                colorsArray, null, Shader.TileMode.CLAMP
            )
            GradientType.RADIAL -> RadialGradient(
                width / 2f, height * 0.4f, maxOf(width, height) * 0.8f,
                colorsArray, null, Shader.TileMode.CLAMP
            )
            GradientType.NONE -> null
        }

        if (paint.shader != null) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        }

        // Alpha composite foreground on top
        val fgPixels = IntArray(width * height)
        foreground.getPixels(fgPixels, 0, width, 0, 0, width, height)
        val bgPixels = IntArray(width * height)
        result.getPixels(bgPixels, 0, width, 0, 0, width, height)

        val resultPixels = IntArray(width * height)
        for (i in fgPixels.indices) {
            val fgPx = fgPixels[i]
            val alpha = Color.alpha(fgPx) / 255f
            if (alpha >= 0.999f) {
                resultPixels[i] = fgPx or (0xFF shl 24)
            } else if (alpha <= 0.001f) {
                resultPixels[i] = bgPixels[i]
            } else {
                val bgPx = bgPixels[i]
                val r = (Color.red(fgPx) * alpha + Color.red(bgPx) * (1f - alpha)).toInt().coerceIn(0, 255)
                val g = (Color.green(fgPx) * alpha + Color.green(bgPx) * (1f - alpha)).toInt().coerceIn(0, 255)
                val b = (Color.blue(fgPx) * alpha + Color.blue(bgPx) * (1f - alpha)).toInt().coerceIn(0, 255)
                resultPixels[i] = Color.rgb(r, g, b)
            }
        }

        result.setPixels(resultPixels, 0, width, 0, 0, width, height)
        return result
    }

    /**
     * Ghép ảnh đã xóa nền lên nền mới — dùng proper alpha compositing.
     * Xử lý premultiplied alpha để tránh viền trắng/đen ở edge.
     */
    fun compositeOnBackground(foreground: Bitmap, backgroundColor: Int): Bitmap {
        val width = foreground.width
        val height = foreground.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        if (backgroundColor == Color.TRANSPARENT) {
            // Giữ nguyên foreground với alpha channel
            val canvas = Canvas(result)
            canvas.drawBitmap(foreground, 0f, 0f, null)
            return result
        }

        val fgPixels = IntArray(width * height)
        foreground.getPixels(fgPixels, 0, width, 0, 0, width, height)

        val bgR = Color.red(backgroundColor).toFloat()
        val bgG = Color.green(backgroundColor).toFloat()
        val bgB = Color.blue(backgroundColor).toFloat()

        val resultPixels = IntArray(width * height)

        for (i in fgPixels.indices) {
            val pixel = fgPixels[i]
            val alpha = Color.alpha(pixel) / 255f

            if (alpha <= 0.001f) {
                // Fully transparent → background color
                resultPixels[i] = Color.rgb(bgR.toInt(), bgG.toInt(), bgB.toInt())
            } else if (alpha >= 0.999f) {
                // Fully opaque → foreground pixel
                resultPixels[i] = pixel or (0xFF shl 24) // Force alpha = 255
            } else {
                // Alpha blend: fg * alpha + bg * (1 - alpha)
                val fgR = Color.red(pixel).toFloat()
                val fgG = Color.green(pixel).toFloat()
                val fgB = Color.blue(pixel).toFloat()

                val r = (fgR * alpha + bgR * (1f - alpha)).toInt().coerceIn(0, 255)
                val g = (fgG * alpha + bgG * (1f - alpha)).toInt().coerceIn(0, 255)
                val b = (fgB * alpha + bgB * (1f - alpha)).toInt().coerceIn(0, 255)

                resultPixels[i] = Color.rgb(r, g, b)
            }
        }

        result.setPixels(resultPixels, 0, width, 0, 0, width, height)
        return result
    }

    /**
     * Lưu ảnh vào Gallery với tuỳ chọn format.
     */
    fun saveToGallery(
        context: Context,
        bitmap: Bitmap,
        fileName: String = "IDPhoto_${System.currentTimeMillis()}",
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
        quality: Int = 100,
        mimeType: String = "image/png",
        fileExtension: String = "png",
    ): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveWithMediaStore(context, bitmap, fileName, format, quality, mimeType, fileExtension)
        } else {
            saveToExternalStorage(context, bitmap, fileName, format, quality, fileExtension)
        }
    }

    private fun saveWithMediaStore(
        context: Context,
        bitmap: Bitmap,
        fileName: String,
        format: Bitmap.CompressFormat,
        quality: Int,
        mimeType: String,
        fileExtension: String,
    ): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$fileName.$fileExtension")
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/IDPhoto")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                bitmap.compress(format, quality, outputStream)
            }
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(it, contentValues, null, null)
        }

        return uri
    }

    @Suppress("DEPRECATION")
    private fun saveToExternalStorage(
        context: Context,
        bitmap: Bitmap,
        fileName: String,
        format: Bitmap.CompressFormat,
        quality: Int,
        fileExtension: String,
    ): Uri? {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "IDPhoto"
        )
        if (!dir.exists()) dir.mkdirs()

        val file = File(dir, "$fileName.$fileExtension")
        FileOutputStream(file).use { outputStream ->
            bitmap.compress(format, quality, outputStream)
        }

        return Uri.fromFile(file)
    }
}
