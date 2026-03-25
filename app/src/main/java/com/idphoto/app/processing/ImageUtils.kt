package com.idphoto.app.processing

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
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

        // ── Mapping ratio: frame-pixel → output-pixel ──
        // QUAN TRỌNG: Dùng uniform ratio (cùng 1 giá trị cho cả X và Y)
        // để tránh biến dạng (méo ảnh). Nếu frame trên screen có aspect ratio
        // hơi khác output (do dp→px rounding), ta dùng min ratio để ảnh vừa
        // khít trong output, rồi center phần dư.
        val rawRatioX = outW.toFloat() / frameWidth.toFloat()
        val rawRatioY = outH.toFloat() / frameHeight.toFloat()
        val uniformRatio = minOf(rawRatioX, rawRatioY)

        // Offset để center ảnh nếu output aspect khác frame aspect
        val centerOffsetX = (outW - frameWidth * uniformRatio) / 2f
        val centerOffsetY = (outH - frameHeight * uniformRatio) / 2f

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
        val frameCx = frameWidth / 2f
        val frameCy = frameHeight / 2f
        val scaledFitX = frameCx + (fitX - frameCx) * scale
        val scaledFitY = frameCy + (fitY - frameCy) * scale

        // Step 3: graphicsLayer translation
        val finalFrameX = scaledFitX + offsetX
        val finalFrameY = scaledFitY + offsetY

        // Map to output-pixel space — dùng uniform ratio + center offset
        val outDrawX = finalFrameX * uniformRatio + centerOffsetX
        val outDrawY = finalFrameY * uniformRatio + centerOffsetY
        // Uniform scale cho cả X và Y — KHÔNG dùng riêng ratioX/ratioY
        val totalScale = fitScale * scale * uniformRatio

        // ── HIGH-QUALITY RENDERING ──
        // Thay vì dùng Matrix scale trên Canvas (bilinear kém chất lượng),
        // pre-scale bitmap bằng multi-step halving (Lanczos-like quality)
        // rồi chỉ dùng Canvas translate (không scale).
        // Dùng uniform totalScale cho cả width và height → giữ đúng tỷ lệ ảnh.
        val destW = (foreground.width * totalScale).toInt().coerceAtLeast(1)
        val destH = (foreground.height * totalScale).toInt().coerceAtLeast(1)

        val scaledFg = highQualityScale(foreground, destW, destH)

        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            isDither = false
        }

        // Vẽ ảnh đã scale sẵn tại vị trí đúng — Canvas KHÔNG scale thêm
        canvas.drawBitmap(scaledFg, outDrawX, outDrawY, paint)

        if (scaledFg != foreground) {
            scaledFg.recycle()
        }

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

        // Draw foreground with transform — uniform ratio to prevent distortion
        val rawRatioX = outW.toFloat() / frameWidth.toFloat()
        val rawRatioY = outH.toFloat() / frameHeight.toFloat()
        val uniformRatio = minOf(rawRatioX, rawRatioY)
        val centerOffsetX = (outW - frameWidth * uniformRatio) / 2f
        val centerOffsetY = (outH - frameHeight * uniformRatio) / 2f

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
        val outDrawX = finalFrameX * uniformRatio + centerOffsetX
        val outDrawY = finalFrameY * uniformRatio + centerOffsetY
        val totalScale = fitScale * scale * uniformRatio

        // ── HIGH-QUALITY RENDERING — pre-scale thay vì Matrix scale ──
        val destW = (foreground.width * totalScale).toInt().coerceAtLeast(1)
        val destH = (foreground.height * totalScale).toInt().coerceAtLeast(1)
        val scaledFg = highQualityScale(foreground, destW, destH)

        val fgPaint = Paint().apply { isAntiAlias = true; isFilterBitmap = true; isDither = false }
        canvas.drawBitmap(scaledFg, outDrawX, outDrawY, fgPaint)
        if (scaledFg != foreground) scaledFg.recycle()

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
     * High-quality bitmap scaling — single-step direct downscale.
     *
     * Thay vì multi-step halving (gây tích lũy blur qua nhiều bước),
     * dùng Android createScaledBitmap (bilinear) trực tiếp 1 bước
     * rồi khôi phục sharpness bằng Unsharp Mask mạnh hơn.
     *
     * Lý do: multi-step halving giữ edge tóc tốt nhưng blur khuôn mặt
     * sau 3-4 bước (ảnh 3000px → 354px = 4 bước halving). Single-step
     * + USM cho kết quả sắc nét hơn trên cả khuôn mặt lẫn tóc.
     *
     * Pipeline:
     * 1. Nếu scale ratio > 4x → 1 bước halving trung gian (giữ detail tóc)
     * 2. Direct scale về target size
     * 3. 2-pass Unsharp Mask: radius lớn (khôi phục structure) + radius nhỏ (detail)
     */
    fun highQualityScale(src: Bitmap, targetW: Int, targetH: Int): Bitmap {
        if (src.width == targetW && src.height == targetH) return src

        // Upscale — bilinear đủ tốt
        if (targetW >= src.width && targetH >= src.height) {
            return Bitmap.createScaledBitmap(src, targetW, targetH, true)
        }

        val scaleRatio = src.width.toFloat() / targetW.toFloat()

        // ── Downscale strategy ──
        // Nếu ratio > 4x: 1 bước trung gian (scale về 2x target) rồi scale tiếp
        // Nếu ratio ≤ 4x: direct scale 1 bước
        // Tối đa 1 bước trung gian → ít blur tích lũy hơn multi-step halving
        var current = src
        var didIntermediate = false

        if (scaleRatio > 4f) {
            // Scale về kích thước trung gian = 2x target
            val midW = targetW * 2
            val midH = targetH * 2
            current = Bitmap.createScaledBitmap(src, midW, midH, true)
            didIntermediate = true
        }

        // Scale về target size
        var result = if (current.width == targetW && current.height == targetH) {
            current
        } else {
            val scaled = Bitmap.createScaledBitmap(current, targetW, targetH, true)
            if (didIntermediate) current.recycle()
            scaled
        }

        // ── 2-pass Unsharp Mask — khôi phục sharpness ──
        // Pass 1: radius=2 — khôi phục structure lớn (đường nét khuôn mặt, mắt)
        // Pass 2: radius=1 — khôi phục fine detail (lông mày, tóc, texture da)
        // Amount adaptive theo scale ratio: downscale càng nhiều → sharpen càng mạnh

        val baseAmount = when {
            scaleRatio > 6f -> 0.7f    // 3000→354px: cần sharpen mạnh
            scaleRatio > 4f -> 0.55f   // 3000→600px: sharpen vừa
            scaleRatio > 2f -> 0.4f    // Scale trung bình
            else -> 0.25f              // Scale nhẹ
        }

        // Pass 1: Structure recovery (radius lớn, threshold cao hơn)
        val pass1 = unsharpMask(result, radius = 2, amount = baseAmount * 0.7f, threshold = 3)
        if (result != src) result.recycle()

        // Pass 2: Detail recovery (radius nhỏ, threshold thấp hơn để bắt fine detail)
        val pass2 = unsharpMask(pass1, radius = 1, amount = baseAmount, threshold = 2)
        pass1.recycle()

        return pass2
    }

    /**
     * Unsharp Mask (USM) — kỹ thuật sharpen chuẩn công nghiệp.
     *
     * Nguyên lý: sharp = original + amount * (original - blurred)
     * Tức là tăng cường sự khác biệt giữa ảnh gốc và ảnh blur,
     * làm nổi bật các edge/detail mà không tạo noise mới.
     *
     * @param radius    Bán kính blur (1 = fine detail, 2 = structure)
     * @param amount    Cường độ sharpen (0.3-0.7)
     * @param threshold Ngưỡng — chỉ sharpen pixel có contrast > threshold,
     *                  tránh amplify noise ở vùng flat (nền đồng màu)
     */
    private fun unsharpMask(
        src: Bitmap,
        radius: Int = 1,
        amount: Float = 0.5f,
        threshold: Int = 2,
    ): Bitmap {
        val width = src.width
        val height = src.height
        val pixels = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)

        // Tạo bản blur bằng box blur (nhanh, đủ tốt cho radius nhỏ)
        val blurred = boxBlur(pixels, width, height, radius)

        // Áp dụng USM: sharp = original + amount * (original - blurred)
        val result = IntArray(width * height)
        for (i in pixels.indices) {
            val origPixel = pixels[i]
            val blurPixel = blurred[i]

            val origA = (origPixel ushr 24) and 0xFF
            val origR = (origPixel shr 16) and 0xFF
            val origG = (origPixel shr 8) and 0xFF
            val origB = origPixel and 0xFF

            val blurR = (blurPixel shr 16) and 0xFF
            val blurG = (blurPixel shr 8) and 0xFF
            val blurB = blurPixel and 0xFF

            val diffR = origR - blurR
            val diffG = origG - blurG
            val diffB = origB - blurB

            // Luminance-based diff check — chỉ bỏ qua vùng thực sự flat
            val lumaDiff = (0.299f * kotlin.math.abs(diffR) +
                           0.587f * kotlin.math.abs(diffG) +
                           0.114f * kotlin.math.abs(diffB)).toInt()

            if (lumaDiff < threshold) {
                // Vùng flat (nền đồng màu) — giữ nguyên
                result[i] = origPixel
            } else {
                // Có detail — sharpen
                val newR = (origR + (diffR * amount).toInt()).coerceIn(0, 255)
                val newG = (origG + (diffG * amount).toInt()).coerceIn(0, 255)
                val newB = (origB + (diffB * amount).toInt()).coerceIn(0, 255)
                result[i] = (origA shl 24) or (newR shl 16) or (newG shl 8) or newB
            }
        }

        val output = Bitmap.createBitmap(width, height, src.config ?: Bitmap.Config.ARGB_8888)
        output.setPixels(result, 0, width, 0, 0, width, height)
        return output
    }

    /**
     * Box blur nhanh — separable 2-pass (horizontal + vertical).
     * O(n) per pixel regardless of radius.
     */
    private fun boxBlur(pixels: IntArray, width: Int, height: Int, radius: Int): IntArray {
        val temp = IntArray(width * height)
        val result = IntArray(width * height)
        val size = radius * 2 + 1

        // Horizontal pass
        for (y in 0 until height) {
            var sumR = 0; var sumG = 0; var sumB = 0

            for (kx in -radius..radius) {
                val px = kx.coerceIn(0, width - 1)
                val pixel = pixels[y * width + px]
                sumR += (pixel shr 16) and 0xFF
                sumG += (pixel shr 8) and 0xFF
                sumB += pixel and 0xFF
            }
            temp[y * width] = (pixels[y * width] and 0xFF000000.toInt()) or
                    ((sumR / size) shl 16) or ((sumG / size) shl 8) or (sumB / size)

            for (x in 1 until width) {
                val addX = (x + radius).coerceAtMost(width - 1)
                val removeX = (x - radius - 1).coerceAtLeast(0)
                val addPixel = pixels[y * width + addX]
                val removePixel = pixels[y * width + removeX]

                sumR += ((addPixel shr 16) and 0xFF) - ((removePixel shr 16) and 0xFF)
                sumG += ((addPixel shr 8) and 0xFF) - ((removePixel shr 8) and 0xFF)
                sumB += (addPixel and 0xFF) - (removePixel and 0xFF)

                temp[y * width + x] = (pixels[y * width + x] and 0xFF000000.toInt()) or
                        ((sumR / size).coerceIn(0, 255) shl 16) or
                        ((sumG / size).coerceIn(0, 255) shl 8) or
                        (sumB / size).coerceIn(0, 255)
            }
        }

        // Vertical pass
        for (x in 0 until width) {
            var sumR = 0; var sumG = 0; var sumB = 0

            for (ky in -radius..radius) {
                val py = ky.coerceIn(0, height - 1)
                val pixel = temp[py * width + x]
                sumR += (pixel shr 16) and 0xFF
                sumG += (pixel shr 8) and 0xFF
                sumB += pixel and 0xFF
            }
            result[x] = (temp[x] and 0xFF000000.toInt()) or
                    ((sumR / size) shl 16) or ((sumG / size) shl 8) or (sumB / size)

            for (y in 1 until height) {
                val addY = (y + radius).coerceAtMost(height - 1)
                val removeY = (y - radius - 1).coerceAtLeast(0)
                val addPixel = temp[addY * width + x]
                val removePixel = temp[removeY * width + x]

                sumR += ((addPixel shr 16) and 0xFF) - ((removePixel shr 16) and 0xFF)
                sumG += ((addPixel shr 8) and 0xFF) - ((removePixel shr 8) and 0xFF)
                sumB += (addPixel and 0xFF) - (removePixel and 0xFF)

                result[y * width + x] = (temp[y * width + x] and 0xFF000000.toInt()) or
                        ((sumR / size).coerceIn(0, 255) shl 16) or
                        ((sumG / size).coerceIn(0, 255) shl 8) or
                        (sumB / size).coerceIn(0, 255)
            }
        }

        return result
    }

    /**
     * Lưu ảnh vào Gallery với tuỳ chọn format và DPI metadata.
     */
    fun saveToGallery(
        context: Context,
        bitmap: Bitmap,
        fileName: String = "IDPhoto_${System.currentTimeMillis()}",
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
        quality: Int = 100,
        mimeType: String = "image/png",
        fileExtension: String = "png",
        dpi: Int = 300,
    ): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveWithMediaStore(context, bitmap, fileName, format, quality, mimeType, fileExtension, dpi)
        } else {
            saveToExternalStorage(context, bitmap, fileName, format, quality, fileExtension, dpi)
        }
    }

    /**
     * Ghi DPI metadata vào EXIF (hỗ trợ JPEG).
     * JPEG sử dụng EXIF XResolution/YResolution.
     */
    private fun writeDpiExif(context: Context, uri: Uri, dpi: Int) {
        try {
            context.contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                val exif = ExifInterface(pfd.fileDescriptor)
                // EXIF stores DPI as rational: "300/1"
                val dpiRational = "$dpi/1"
                exif.setAttribute(ExifInterface.TAG_X_RESOLUTION, dpiRational)
                exif.setAttribute(ExifInterface.TAG_Y_RESOLUTION, dpiRational)
                // ResolutionUnit = 2 means DPI (inches)
                exif.setAttribute(ExifInterface.TAG_RESOLUTION_UNIT, "2")
                exif.saveAttributes()
            }
        } catch (_: Exception) {
            // Silently fail — DPI metadata is optional
        }
    }

    private fun writeDpiExif(file: File, dpi: Int) {
        try {
            val exif = ExifInterface(file.absolutePath)
            val dpiRational = "$dpi/1"
            exif.setAttribute(ExifInterface.TAG_X_RESOLUTION, dpiRational)
            exif.setAttribute(ExifInterface.TAG_Y_RESOLUTION, dpiRational)
            exif.setAttribute(ExifInterface.TAG_RESOLUTION_UNIT, "2")
            exif.saveAttributes()
        } catch (_: Exception) {
            // Silently fail
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
        dpi: Int,
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
            // Ghi DPI metadata vào EXIF (JPEG)
            if (format == Bitmap.CompressFormat.JPEG) {
                writeDpiExif(context, it, dpi)
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
        dpi: Int,
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

        // Ghi DPI metadata vào EXIF (JPEG)
        if (format == Bitmap.CompressFormat.JPEG) {
            writeDpiExif(file, dpi)
        }

        return Uri.fromFile(file)
    }
}
