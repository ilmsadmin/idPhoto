package com.idphoto.app.processing

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Xử lý làm đẹp da và tăng độ tươi tắn cho khuôn mặt.
 *
 * Bao gồm:
 * - Làm mịn da (skin smoothing) — bilateral-like filter
 * - Tăng sáng & tươi tắn (brightness/vibrance boost)
 * - Tăng contrast nhẹ cho da sáng hơn
 */
object BeautyProcessor {

    /**
     * Áp dụng tất cả hiệu ứng làm đẹp.
     * @param smoothLevel 0f..1f — mức làm mịn da (0 = không, 1 = max)
     * @param brightnessLevel 0f..1f — mức tăng sáng/tươi tắn
     */
    fun applyBeauty(
        bitmap: Bitmap,
        smoothLevel: Float = 0.5f,
        brightnessLevel: Float = 0.5f
    ): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Detect skin region — tạo mask xác định vùng da
        val skinMask = detectSkinRegion(pixels, width, height)

        // Bước 1: Làm mịn da (bilateral-inspired smoothing) — chỉ trên vùng da
        if (smoothLevel > 0.01f) {
            applySkinSmoothing(pixels, skinMask, width, height, smoothLevel)
        }

        // Bước 2: Tăng sáng & tươi tắn
        if (brightnessLevel > 0.01f) {
            applyBrightnessAndFreshness(pixels, skinMask, width, height, brightnessLevel)
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }

    /**
     * Chỉ làm mịn da.
     */
    fun applySmoothOnly(bitmap: Bitmap, level: Float): Bitmap {
        if (level <= 0.01f) return bitmap.copy(Bitmap.Config.ARGB_8888, true)

        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val skinMask = detectSkinRegion(pixels, width, height)
        applySkinSmoothing(pixels, skinMask, width, height, level)

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }

    /**
     * Chỉ tăng tươi tắn.
     */
    fun applyFreshnessOnly(bitmap: Bitmap, level: Float): Bitmap {
        if (level <= 0.01f) return bitmap.copy(Bitmap.Config.ARGB_8888, true)

        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val skinMask = detectSkinRegion(pixels, width, height)
        applyBrightnessAndFreshness(pixels, skinMask, width, height, level)

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }

    // ─── Skin Detection ────────────────────────────────────

    /**
     * Phát hiện vùng da dựa trên màu sắc trong không gian YCbCr.
     * Trả về FloatArray 0..1 (0 = không phải da, 1 = da).
     *
     * Cải tiến:
     * - Loại bỏ vùng tối (lông mày, tóc, mắt) dựa trên luminance thấp
     * - Loại bỏ vùng có texture cao (lông mày, tóc) dựa trên local variance
     * - Thu hẹp vùng soft-edge để không lan vào tóc/lông mày
     */
    private fun detectSkinRegion(pixels: IntArray, width: Int, height: Int): FloatArray {
        val mask = FloatArray(width * height)

        // Bước 1: Tính luminance cho toàn ảnh
        val luminance = FloatArray(width * height)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            luminance[i] = 0.299f * r + 0.587f * g + 0.114f * b
        }

        // Bước 2: Tính local variance (texture measure) — dùng 3x3 neighborhood
        val variance = computeLocalVariance(luminance, width, height, 1)

        // Bước 3: Phát hiện da bằng YCbCr + loại bỏ vùng tối/texture cao
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF

            val y = luminance[i]
            val cb = (-0.169f * r - 0.331f * g + 0.500f * b + 128f)
            val cr = (0.500f * r - 0.419f * g - 0.081f * b + 128f)

            // ── Loại bỏ pixel tối (tóc, lông mày, mắt) ──
            // Pixel quá tối gần như chắc chắn không phải da
            if (y < 80) {
                mask[i] = 0f
                continue
            }

            // ── Loại bỏ vùng texture cao (tóc, lông mày, lông mi) ──
            // Da thật có texture thấp (mịn), tóc/lông mày có texture cao
            val localVar = variance[i]
            val textureThreshold = 600f  // Vùng da thường có variance < 400-500
            if (localVar > textureThreshold * 2) {
                // Texture rất cao → chắc chắn không phải da
                mask[i] = 0f
                continue
            }

            // Skin color range in YCbCr
            val isSkin = y > 80 && y < 240 &&
                    cb > 80 && cb < 125 &&
                    cr > 135 && cr < 170

            if (isSkin) {
                // Giảm weight nếu texture cao (vùng chuyển tiếp da↔tóc)
                val texturePenalty = if (localVar > textureThreshold) {
                    max(0f, 1f - (localVar - textureThreshold) / textureThreshold)
                } else {
                    1f
                }
                mask[i] = texturePenalty
            } else {
                // Soft edge nhỏ hơn — chỉ 8px falloff thay vì 15px
                val cbDist = if (cb < 80) (80 - cb) / 8f else if (cb > 125) (cb - 125) / 8f else 0f
                val crDist = if (cr < 135) (135 - cr) / 8f else if (cr > 170) (cr - 170) / 8f else 0f
                val dist = sqrt(cbDist * cbDist + crDist * crDist)
                val colorWeight = max(0f, 1f - dist)

                // Soft edge cũng bị penalty texture
                val texturePenalty = if (localVar > textureThreshold) {
                    max(0f, 1f - (localVar - textureThreshold) / textureThreshold)
                } else {
                    1f
                }
                mask[i] = colorWeight * texturePenalty
            }
        }

        // Smooth skin mask (radius=1 thay vì 2, tránh lan vào tóc/lông mày)
        return smoothMask(mask, width, height, 1)
    }

    /**
     * Tính local variance (độ biến đổi cục bộ) của luminance.
     * Vùng da mịn → variance thấp, vùng tóc/lông mày → variance cao.
     */
    private fun computeLocalVariance(
        luminance: FloatArray,
        width: Int,
        height: Int,
        radius: Int
    ): FloatArray {
        val result = FloatArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                var sum = 0f
                var sumSq = 0f
                var count = 0

                val y0 = max(0, y - radius)
                val y1 = min(height - 1, y + radius)
                val x0 = max(0, x - radius)
                val x1 = min(width - 1, x + radius)

                for (ny in y0..y1) {
                    for (nx in x0..x1) {
                        val v = luminance[ny * width + nx]
                        sum += v
                        sumSq += v * v
                        count++
                    }
                }

                val mean = sum / count
                result[y * width + x] = sumSq / count - mean * mean
            }
        }
        return result
    }

    private fun smoothMask(mask: FloatArray, width: Int, height: Int, radius: Int): FloatArray {
        val result = FloatArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                var sum = 0f
                var count = 0
                for (dy in -radius..radius) {
                    for (dx in -radius..radius) {
                        val nx = (x + dx).coerceIn(0, width - 1)
                        val ny = (y + dy).coerceIn(0, height - 1)
                        sum += mask[ny * width + nx]
                        count++
                    }
                }
                result[y * width + x] = sum / count
            }
        }
        return result
    }

    // ─── Skin Smoothing ────────────────────────────────────

    /**
     * Làm mịn da — surface blur style.
     * Chỉ smooth pixel có màu sắc tương tự, giữ edge chi tiết (mắt, mũi, miệng).
     *
     * Kết hợp box blur + bilateral threshold:
     * - Pixel gần giống → trung bình (smooth)
     * - Pixel khác biệt lớn → giữ nguyên (edge)
     *
     * Cải tiến:
     * - Chỉ lấy trung bình từ các pixel cũng thuộc vùng da (skinMask > 0.3)
     * - Giảm radius gần biên da↔tóc để tránh lấy mẫu từ vùng tóc/lông mày
     * - Tăng ngưỡng skinWeight tối thiểu để tránh smooth vùng chuyển tiếp
     */
    private fun applySkinSmoothing(
        pixels: IntArray,
        skinMask: FloatArray,
        width: Int,
        height: Int,
        level: Float
    ) {
        val baseRadius = (3 + (level * 4)).toInt() // radius 3..7 (giảm max từ 8→7)
        val colorThreshold = (20 + level * 30).toInt() // threshold 20..50

        val original = pixels.copyOf()

        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = y * width + x
                val skinWeight = skinMask[idx]
                if (skinWeight < 0.3f) continue // Tăng ngưỡng từ 0.1→0.3, bảo vệ vùng chuyển tiếp

                // Giảm radius khi skinWeight thấp (gần biên da)
                // Vùng biên chỉ dùng radius nhỏ để không lấy mẫu từ tóc/lông mày
                val effectiveRadius = if (skinWeight > 0.7f) baseRadius
                    else (baseRadius * skinWeight).toInt().coerceAtLeast(2)

                val centerPixel = original[idx]
                val cr = (centerPixel shr 16) and 0xFF
                val cg = (centerPixel shr 8) and 0xFF
                val cb = centerPixel and 0xFF

                var sumR = 0f
                var sumG = 0f
                var sumB = 0f
                var totalWeight = 0f

                val y0 = max(0, y - effectiveRadius)
                val y1 = min(height - 1, y + effectiveRadius)
                val x0 = max(0, x - effectiveRadius)
                val x1 = min(width - 1, x + effectiveRadius)

                for (ny in y0..y1) {
                    for (nx in x0..x1) {
                        val nIdx = ny * width + nx

                        // Chỉ lấy mẫu từ pixel cũng thuộc vùng da
                        // Tránh lấy mẫu từ tóc/lông mày rồi blend vào da
                        if (skinMask[nIdx] < 0.2f) continue

                        val nPixel = original[nIdx]
                        val nr = (nPixel shr 16) and 0xFF
                        val ng = (nPixel shr 8) and 0xFF
                        val nb = nPixel and 0xFF

                        // Color difference — bilateral component
                        val diff = Math.abs(nr - cr) + Math.abs(ng - cg) + Math.abs(nb - cb)
                        if (diff < colorThreshold) {
                            val w = (1f - diff.toFloat() / colorThreshold) * skinMask[nIdx]
                            sumR += nr * w
                            sumG += ng * w
                            sumB += nb * w
                            totalWeight += w
                        }
                    }
                }

                if (totalWeight > 0) {
                    val newR = (sumR / totalWeight).toInt().coerceIn(0, 255)
                    val newG = (sumG / totalWeight).toInt().coerceIn(0, 255)
                    val newB = (sumB / totalWeight).toInt().coerceIn(0, 255)

                    // Blend theo skinWeight và level
                    val blend = skinWeight * level
                    val finalR = (cr * (1f - blend) + newR * blend).toInt().coerceIn(0, 255)
                    val finalG = (cg * (1f - blend) + newG * blend).toInt().coerceIn(0, 255)
                    val finalB = (cb * (1f - blend) + newB * blend).toInt().coerceIn(0, 255)

                    pixels[idx] = (pixels[idx] and 0xFF000000.toInt()) or
                            (finalR shl 16) or (finalG shl 8) or finalB
                }
            }
        }
    }

    // ─── Brightness & Freshness ─────────────────────────────

    /**
     * Tăng sáng, tươi tắn, và hồng hào cho khuôn mặt.
     *
     * - Tăng luminance nhẹ (sáng da)
     * - Tăng saturation nhẹ (tươi hơn)
     * - Thêm chút warm tone (ấm, hồng hào)
     * - Tăng contrast nhẹ
     */
    private fun applyBrightnessAndFreshness(
        pixels: IntArray,
        skinMask: FloatArray,
        width: Int,
        height: Int,
        level: Float
    ) {
        val brightBoost = level * 15f      // +0..15 brightness
        val satBoost = 1f + level * 0.25f  // 1.0..1.25 saturation
        val warmShift = level * 5f         // warm tone shift
        val contrastBoost = 1f + level * 0.1f // 1.0..1.1 contrast

        for (i in pixels.indices) {
            val skinWeight = skinMask[i]
            if (skinWeight < 0.05f) continue

            val pixel = pixels[i]
            val a = (pixel shr 24) and 0xFF
            var r = ((pixel shr 16) and 0xFF).toFloat()
            var g = ((pixel shr 8) and 0xFF).toFloat()
            var b = (pixel and 0xFF).toFloat()

            val blend = skinWeight * level

            // 1. Tăng sáng
            r += brightBoost * blend
            g += brightBoost * blend
            b += brightBoost * blend

            // 2. Warm tone (hồng hào nhẹ)
            r += warmShift * blend
            b -= (warmShift * 0.3f) * blend

            // 3. Tăng saturation
            val lum = 0.299f * r + 0.587f * g + 0.114f * b
            val satFactor = 1f + (satBoost - 1f) * blend
            r = lum + (r - lum) * satFactor
            g = lum + (g - lum) * satFactor
            b = lum + (b - lum) * satFactor

            // 4. Tăng contrast nhẹ (quanh midpoint 128)
            val cf = 1f + (contrastBoost - 1f) * blend
            r = (r - 128f) * cf + 128f
            g = (g - 128f) * cf + 128f
            b = (b - 128f) * cf + 128f

            pixels[i] = (a shl 24) or
                    (r.toInt().coerceIn(0, 255) shl 16) or
                    (g.toInt().coerceIn(0, 255) shl 8) or
                    b.toInt().coerceIn(0, 255)
        }
    }
}
