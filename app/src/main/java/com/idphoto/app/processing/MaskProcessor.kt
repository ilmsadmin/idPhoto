package com.idphoto.app.processing

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

/**
 * Bộ xử lý mask v3 — đơn giản, hiệu quả, sạch.
 *
 * Triết lý: ML Kit mask chất lượng trung bình → KHÔNG cố fix bằng blur/decontamination
 * mà dùng hard threshold + erode để cắt sạch, chấp nhận mất 1-2px chi tiết
 * nhưng đổi lại viền hoàn toàn không dính nền cũ.
 */
object MaskProcessor {

    // ──────────────────────────────────────────────
    // BILINEAR INTERPOLATION UPSCALE
    // ──────────────────────────────────────────────

    fun bilinearUpscale(
        mask: FloatArray,
        srcW: Int, srcH: Int,
        dstW: Int, dstH: Int
    ): FloatArray {
        val result = FloatArray(dstW * dstH)

        for (y in 0 until dstH) {
            for (x in 0 until dstW) {
                val srcX = x.toFloat() * (srcW - 1) / (dstW - 1).coerceAtLeast(1)
                val srcY = y.toFloat() * (srcH - 1) / (dstH - 1).coerceAtLeast(1)

                val x0 = srcX.toInt().coerceIn(0, srcW - 1)
                val y0 = srcY.toInt().coerceIn(0, srcH - 1)
                val x1 = (x0 + 1).coerceIn(0, srcW - 1)
                val y1 = (y0 + 1).coerceIn(0, srcH - 1)

                val fx = srcX - x0
                val fy = srcY - y0

                val v00 = mask[y0 * srcW + x0]
                val v10 = mask[y0 * srcW + x1]
                val v01 = mask[y1 * srcW + x0]
                val v11 = mask[y1 * srcW + x1]

                val value = v00 * (1 - fx) * (1 - fy) +
                        v10 * fx * (1 - fy) +
                        v01 * (1 - fx) * fy +
                        v11 * fx * fy

                result[y * dstW + x] = value.coerceIn(0f, 1f)
            }
        }
        return result
    }

    // ──────────────────────────────────────────────
    // HARD THRESHOLD
    // ──────────────────────────────────────────────

    /**
     * Chuyển soft mask thành hard mask (0 hoặc 1).
     * Loại bỏ hoàn toàn vùng semi-transparent chứa nền cũ.
     */
    fun hardThreshold(mask: FloatArray, threshold: Float = 0.5f): FloatArray {
        return FloatArray(mask.size) { i ->
            if (mask[i] >= threshold) 1f else 0f
        }
    }

    // ──────────────────────────────────────────────
    // ERODE — thu nhỏ mask để cắt bỏ nền sót ở rìa
    // ──────────────────────────────────────────────

    /**
     * Morphological erosion — thu nhỏ vùng foreground vào trong [radius] pixel.
     * Nếu bất kỳ pixel nào trong vùng lân cận [radius] là 0 → pixel này = 0.
     * Hiệu quả: cắt bỏ 1-2px nền sót ở viền.
     */
    fun erode(mask: FloatArray, width: Int, height: Int, radius: Int): FloatArray {
        if (radius <= 0) return mask.copyOf()

        val result = FloatArray(width * height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = y * width + x

                if (mask[idx] <= 0f) {
                    result[idx] = 0f
                    continue
                }

                // Kiểm tra tất cả pixel trong vùng lân cận
                var minVal = 1f
                for (dy in -radius..radius) {
                    for (dx in -radius..radius) {
                        // Chỉ kiểm tra trong hình tròn (circular erosion)
                        if (dx * dx + dy * dy > radius * radius) continue

                        val nx = (x + dx).coerceIn(0, width - 1)
                        val ny = (y + dy).coerceIn(0, height - 1)
                        val nVal = mask[ny * width + nx]
                        if (nVal < minVal) minVal = nVal
                    }
                }
                result[idx] = minVal
            }
        }
        return result
    }

    // ──────────────────────────────────────────────
    // SMOOTH EDGE ONLY — anti-alias 1px ở viền
    // ──────────────────────────────────────────────

    /**
     * Chỉ smooth ở viền (pixel có neighbor khác giá trị) — tạo anti-alias nhẹ.
     * Không ảnh hưởng vùng foreground/background bên trong.
     */
    fun smoothEdgeOnly(mask: FloatArray, width: Int, height: Int, smoothRadius: Int = 1): FloatArray {
        val result = mask.copyOf()

        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = y * width + x
                val val0 = mask[idx]

                // Chỉ xử lý pixel ở viền (có neighbor khác giá trị)
                if (!isEdgePixel(mask, width, height, x, y)) continue

                // Box average trong vùng smoothRadius
                var sum = 0f
                var count = 0
                for (dy in -smoothRadius..smoothRadius) {
                    for (dx in -smoothRadius..smoothRadius) {
                        val nx = (x + dx).coerceIn(0, width - 1)
                        val ny = (y + dy).coerceIn(0, height - 1)
                        sum += mask[ny * width + nx]
                        count++
                    }
                }
                result[idx] = (sum / count).coerceIn(0f, 1f)
            }
        }
        return result
    }

    /**
     * Kiểm tra pixel có phải edge không — có ít nhất 1 neighbor có giá trị khác.
     */
    private fun isEdgePixel(mask: FloatArray, width: Int, height: Int, x: Int, y: Int): Boolean {
        val val0 = mask[y * width + x]
        // 4-connected neighbors
        val neighbors = listOf(
            Pair(x - 1, y), Pair(x + 1, y),
            Pair(x, y - 1), Pair(x, y + 1)
        )
        for ((nx, ny) in neighbors) {
            if (nx < 0 || nx >= width || ny < 0 || ny >= height) continue
            val nVal = mask[ny * width + nx]
            // Nếu neighbor khác loại (0 vs 1) → đây là edge
            if ((val0 > 0.5f) != (nVal > 0.5f)) return true
        }
        return false
    }

    // ──────────────────────────────────────────────
    // APPLY MASK — đơn giản, giữ nguyên pixel gốc
    // ──────────────────────────────────────────────

    /**
     * Áp dụng mask lên ảnh — chỉ set alpha channel.
     * Pixel gốc giữ nguyên 100%, không blend, không decontamination.
     */
    fun applyMaskSimple(
        original: Bitmap,
        mask: FloatArray,
        width: Int, height: Int
    ): Bitmap {
        val pixels = IntArray(width * height)
        original.getPixels(pixels, 0, width, 0, 0, width, height)

        val resultPixels = IntArray(width * height)

        for (i in pixels.indices) {
            val a = (mask[i] * 255).toInt().coerceIn(0, 255)
            resultPixels[i] = (a shl 24) or (pixels[i] and 0x00FFFFFF)
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(resultPixels, 0, width, 0, 0, width, height)
        return result
    }

    // ──────────────────────────────────────────────
    // SIGMOID REFINEMENT (giữ lại cho MODNet)
    // ──────────────────────────────────────────────

    fun sigmoidRefine(
        mask: FloatArray,
        midpoint: Float = 0.5f,
        steepness: Float = 10f
    ): FloatArray {
        return FloatArray(mask.size) { i ->
            val x = mask[i]
            (1f / (1f + exp(-steepness * (x - midpoint)))).coerceIn(0f, 1f)
        }
    }

    // ──────────────────────────────────────────────
    // MODNet PIPELINE
    // ──────────────────────────────────────────────

    /**
     * Pipeline cho MODNet alpha matte.
     * MODNet đã cho ra soft alpha matte chất lượng cao → xử lý nhẹ,
     * giữ nguyên soft edges (tóc, viền mịn).
     */
    fun processModNetMask(
        original: Bitmap,
        rawMask: FloatArray,
        maskW: Int, maskH: Int
    ): Bitmap {
        val width = original.width
        val height = original.height

        // Upscale mask về kích thước ảnh gốc
        val upscaled = bilinearUpscale(rawMask, maskW, maskH, width, height)

        // MODNet alpha matte đã rất tốt → chỉ cần nhẹ sigmoid để tăng contrast
        val refined = sigmoidRefine(upscaled, midpoint = 0.5f, steepness = 8f)

        return applyMaskSimple(original, refined, width, height)
    }

    // ──────────────────────────────────────────────
    // HAIR-AWARE PIPELINE (v2)
    // ──────────────────────────────────────────────

    /**
     * Pipeline nâng cao cho MODNet — xử lý đặc biệt vùng tóc.
     * Trả về FloatArray alpha mask (KHÔNG phải Bitmap) để caller có thể
     * upscale về original resolution trước khi apply.
     *
     * QUAN TRỌNG: Gọi ở processing resolution (≤1024px) để tránh OOM.
     *
     * Các bước:
     * 1. Phát hiện vùng chuyển tiếp (transition zone) — nơi có tóc
     * 2. Guided filter dùng ảnh gốc làm guide — giữ edge tóc sắc nét
     * 3. Soft sigmoid cho vùng body, giữ nguyên soft alpha cho vùng tóc
     * 4. Edge-aware color decontamination nhẹ cho vùng tóc
     */
    fun processModNetMaskHairAware(
        original: Bitmap,
        mergedMask: FloatArray,
        width: Int,
        height: Int
    ): FloatArray {
        // 1. Detect transition zone (tóc, viền mềm)
        val transitionMap = detectTransitionZone(mergedMask, width, height)

        // 2. Guided filter — dùng luminance của ảnh gốc để refine alpha ở vùng tóc
        val guided = guidedFilterAlpha(original, mergedMask, width, height, radius = 4, eps = 0.01f)

        // 3. Adaptive sigmoid: mạnh cho body (sắc nét), nhẹ cho tóc (giữ detail)
        val refined = adaptiveSigmoidRefine(mergedMask, guided, transitionMap, width, height)

        // 4. Nhẹ nhàng smooth edge ở vùng KHÔNG phải tóc (body edge)
        return selectiveSmoothEdge(refined, transitionMap, width, height)
    }

    /**
     * Phát hiện vùng chuyển tiếp (transition zone).
     * Vùng có alpha ở khoảng [0.05, 0.95] = vùng tóc / viền mềm.
     * Trả về map: 0 = solid (body/background), 1 = transition (tóc).
     */
    private fun detectTransitionZone(
        mask: FloatArray,
        width: Int,
        height: Int
    ): FloatArray {
        val transition = FloatArray(width * height)

        for (i in mask.indices) {
            // Soft transition detection
            val v = mask[i]
            transition[i] = when {
                v < 0.05f || v > 0.95f -> 0f       // chắc chắn FG/BG
                v < 0.15f -> (v - 0.05f) / 0.10f   // ramp up từ 0.05→0.15
                v > 0.85f -> (0.95f - v) / 0.10f   // ramp down từ 0.85→0.95
                else -> 1f                          // vùng chuyển tiếp rõ ràng
            }
        }

        // Dilate transition zone — mở rộng ra 3px để bao phủ cả viền tóc
        return dilateMask(transition, width, height, radius = 3)
    }

    /**
     * Dilate (mở rộng) mask — ngược với erode.
     * Tối ưu: separable 2-pass (horizontal + vertical) max filter → O(N×r) thay vì O(N×r²).
     */
    private fun dilateMask(
        mask: FloatArray,
        width: Int,
        height: Int,
        radius: Int
    ): FloatArray {
        if (radius <= 0) return mask.copyOf()

        // Pass 1: horizontal max
        val temp = FloatArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                var maxVal = 0f
                val x0 = (x - radius).coerceAtLeast(0)
                val x1 = (x + radius).coerceAtMost(width - 1)
                for (nx in x0..x1) {
                    val v = mask[y * width + nx]
                    if (v > maxVal) maxVal = v
                }
                temp[y * width + x] = maxVal
            }
        }

        // Pass 2: vertical max
        val result = FloatArray(width * height)
        for (x in 0 until width) {
            for (y in 0 until height) {
                var maxVal = 0f
                val y0 = (y - radius).coerceAtLeast(0)
                val y1 = (y + radius).coerceAtMost(height - 1)
                for (ny in y0..y1) {
                    val v = temp[ny * width + x]
                    if (v > maxVal) maxVal = v
                }
                result[y * width + x] = maxVal
            }
        }
        return result
    }

    /**
     * Guided filter đơn giản — dùng luminance ảnh gốc làm guide.
     *
     * Ý tưởng: alpha nên thay đổi theo cùng edge structure với ảnh gốc.
     * Tóc tối trên nền sáng → edge rõ → giữ alpha sharp theo edge đó.
     *
     * Simplified box guided filter: O(N) complexity.
     * Tối ưu bộ nhớ: reuse buffer để giảm số FloatArray allocations.
     */
    private fun guidedFilterAlpha(
        image: Bitmap,
        mask: FloatArray,
        width: Int,
        height: Int,
        radius: Int,
        eps: Float
    ): FloatArray {
        val n = width * height

        // Lấy luminance từ ảnh gốc
        val pixels = IntArray(n)
        image.getPixels(pixels, 0, width, 0, 0, width, height)
        val guide = FloatArray(n) { i ->
            val p = pixels[i]
            (0.299f * Color.red(p) + 0.587f * Color.green(p) + 0.114f * Color.blue(p)) / 255f
        }

        // Reuse buffers để tiết kiệm RAM
        val buf1 = FloatArray(n) // dùng cho nhiều mục đích
        val buf2 = FloatArray(n)

        // meanI = boxFilter(guide)
        val meanI = boxFilter(guide, width, height, radius)
        // meanP = boxFilter(mask)
        val meanP = boxFilter(mask, width, height, radius)

        // buf1 = I * P → meanIP = boxFilter(buf1)
        for (i in 0 until n) buf1[i] = guide[i] * mask[i]
        val meanIP = boxFilter(buf1, width, height, radius)

        // buf1 = I * I → meanII = boxFilter(buf1), reuse buf1
        for (i in 0 until n) buf1[i] = guide[i] * guide[i]
        val meanII = boxFilter(buf1, width, height, radius)

        // a = cov / (var + eps) → store in buf1
        // b = meanP - a * meanI → store in buf2
        for (i in 0 until n) {
            val covIP = meanIP[i] - meanI[i] * meanP[i]
            val varI = meanII[i] - meanI[i] * meanI[i]
            buf1[i] = covIP / (varI + eps)       // a
            buf2[i] = meanP[i] - buf1[i] * meanI[i]  // b
        }

        // meanA, meanB
        val meanA = boxFilter(buf1, width, height, radius)
        val meanB = boxFilter(buf2, width, height, radius)

        // Output: q = meanA * I + meanB
        val result = FloatArray(n)
        for (i in 0 until n) {
            result[i] = (meanA[i] * guide[i] + meanB[i]).coerceIn(0f, 1f)
        }
        return result
    }

    /**
     * Box filter (mean filter) — O(N) implementation dùng prefix sum.
     */
    private fun boxFilter(
        input: FloatArray,
        width: Int,
        height: Int,
        radius: Int
    ): FloatArray {
        val result = FloatArray(width * height)
        val temp = FloatArray(width * height)

        // Horizontal pass
        for (y in 0 until height) {
            var sum = 0f
            var count = 0
            // Initialize window
            for (x in 0..min(radius, width - 1)) {
                sum += input[y * width + x]
                count++
            }
            temp[y * width] = sum / count

            for (x in 1 until width) {
                // Add right
                val addX = x + radius
                if (addX < width) {
                    sum += input[y * width + addX]
                    count++
                }
                // Remove left
                val remX = x - radius - 1
                if (remX >= 0) {
                    sum -= input[y * width + remX]
                    count--
                }
                temp[y * width + x] = sum / count
            }
        }

        // Vertical pass
        for (x in 0 until width) {
            var sum = 0f
            var count = 0
            for (y in 0..min(radius, height - 1)) {
                sum += temp[y * width + x]
                count++
            }
            result[x] = sum / count

            for (y in 1 until height) {
                val addY = y + radius
                if (addY < height) {
                    sum += temp[addY * width + x]
                    count++
                }
                val remY = y - radius - 1
                if (remY >= 0) {
                    sum -= temp[remY * width + x]
                    count--
                }
                result[y * width + x] = sum / count
            }
        }

        return result
    }

    /**
     * Adaptive sigmoid: mạnh cho body, nhẹ cho tóc.
     * - Vùng body (transitionMap ≈ 0): sigmoid steep → cắt sạch
     * - Vùng tóc (transitionMap ≈ 1): dùng guided alpha → giữ detail
     */
    private fun adaptiveSigmoidRefine(
        originalMask: FloatArray,
        guidedMask: FloatArray,
        transitionMap: FloatArray,
        width: Int,
        height: Int
    ): FloatArray {
        val result = FloatArray(width * height)

        for (i in result.indices) {
            val t = transitionMap[i]  // 0=body, 1=hair/transition

            if (t < 0.01f) {
                // Body: sigmoid mạnh → sắc nét
                val x = originalMask[i]
                result[i] = (1f / (1f + exp(-12f * (x - 0.5f)))).coerceIn(0f, 1f)
            } else {
                // Hair/transition: blend giữa guided và sigmoid nhẹ
                val guided = guidedMask[i]
                val softSigmoid = (1f / (1f + exp(-6f * (originalMask[i] - 0.45f)))).coerceIn(0f, 1f)

                // Bias về guided (giữ detail tóc), nhưng sigmoid nhẹ giúp bớt noise
                result[i] = (guided * 0.7f + softSigmoid * 0.3f).coerceIn(0f, 1f)
            }
        }

        return result
    }

    /**
     * Smooth edge chỉ ở vùng body (không phải tóc).
     * Vùng tóc giữ nguyên sharp edge từ guided filter.
     */
    private fun selectiveSmoothEdge(
        mask: FloatArray,
        transitionMap: FloatArray,
        width: Int,
        height: Int
    ): FloatArray {
        val smoothed = smoothEdgeOnly(mask, width, height, smoothRadius = 1)
        val result = FloatArray(width * height)

        for (i in result.indices) {
            val t = transitionMap[i]
            // Vùng tóc: giữ nguyên, vùng body: dùng smoothed
            result[i] = mask[i] * t + smoothed[i] * (1f - t)
        }
        return result
    }

}
