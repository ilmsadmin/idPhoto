package com.idphoto.app.processing

import android.content.Context
import android.graphics.Bitmap
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.FloatBuffer
import kotlin.math.exp

/**
 * MODNet ONNX — tối ưu tốc độ, chất lượng cao.
 * Alpha matte mịn, xử lý tốt viền tóc và chi tiết nhỏ.
 * Dùng cho final export.
 *
 * Model: modnet_photographic_portrait_matting.onnx
 * Input: [1, 3, H, W] — dynamic size, phải chia hết cho 32
 * Output: [1, 1, H, W] — alpha matte [0, 1]
 * Normalization: (pixel - 127.5) / 127.5 → range [-1, 1]
 *
 * v3: Single-scale inference + lightweight refinement
 *   - 1 lần inference duy nhất ở scale 512 (MODNet đã cho alpha matte tốt)
 *   - Lightweight sigmoid refinement + edge smoothing (nhanh, ít allocation)
 *   - ~2x nhanh hơn v2 multi-scale
 */
class MODNetProcessor(private val context: Context) {

    companion object {
        private const val MODEL_FILE = "modnet.onnx"
        private const val REF_SIZE = 512          // Scale duy nhất — balance giữa chất lượng và tốc độ
        // Giới hạn resolution xử lý post-processing để tránh OOM
        private const val MAX_PROCESS_SIDE = 1024

        // Pre-computed normalization constants
        private const val NORM_SCALE = 1f / 127.5f
        private const val NORM_OFFSET = -1.0f
    }

    private val ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment()
    private var session: OrtSession? = null
    // Cache input name — không cần lookup mỗi lần inference
    private var cachedInputName: String? = null

    private fun getSession(): OrtSession {
        if (session == null) {
            val modelBytes = context.assets.open(MODEL_FILE).readBytes()
            val options = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(4)
                // Tối ưu graph cho inference
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            }
            session = ortEnv.createSession(modelBytes, options)
            cachedInputName = session!!.inputNames.first()
        }
        return session!!
    }

    /**
     * Tính kích thước input phù hợp — giữ tỷ lệ, chia hết cho 32.
     * Logic giống inference_onnx.py của MODNet chính thức.
     */
    private fun getInputSize(origW: Int, origH: Int, refSize: Int = REF_SIZE): Pair<Int, Int> {
        var imRw: Int
        var imRh: Int

        if (maxOf(origH, origW) < refSize || minOf(origH, origW) > refSize) {
            if (origW >= origH) {
                imRh = refSize
                imRw = (origW.toFloat() / origH * refSize).toInt()
            } else {
                imRw = refSize
                imRh = (origH.toFloat() / origW * refSize).toInt()
            }
        } else {
            imRw = origW
            imRh = origH
        }

        // Làm tròn xuống bội số 32
        imRw = imRw - imRw % 32
        imRh = imRh - imRh % 32

        // Đảm bảo tối thiểu 32
        imRw = imRw.coerceAtLeast(32)
        imRh = imRh.coerceAtLeast(32)

        return Pair(imRw, imRh)
    }

    /**
     * Xử lý ảnh bằng MODNet — single-scale inference + lightweight refinement.
     *
     * Tối ưu tốc độ:
     * - 1 lần inference duy nhất (thay vì 2 lần multi-scale)
     * - Upscale trực tiếp về target size (không qua bước trung gian)
     * - Lightweight sigmoid refinement thay vì guided filter nặng
     *
     * @return Bitmap ARGB_8888 với alpha matte mịn
     */
    fun removeBackground(bitmap: Bitmap): Bitmap {
        val sess = getSession()

        val origW = bitmap.width
        val origH = bitmap.height

        // ── Single inference ở REF_SIZE = 512 ──
        val (inputW, inputH) = getInputSize(origW, origH, REF_SIZE)
        val matte = runInference(sess, bitmap, inputW, inputH)

        // ── Tính target size cho refinement (≤ MAX_PROCESS_SIDE) ──
        val targetW: Int
        val targetH: Int
        val needUpscaleFinal: Boolean

        val maxOrig = maxOf(origW, origH)
        if (maxOrig <= MAX_PROCESS_SIDE) {
            targetW = origW
            targetH = origH
            needUpscaleFinal = false
        } else {
            val scale = MAX_PROCESS_SIDE.toFloat() / maxOrig
            targetW = (origW * scale).toInt().coerceAtLeast(1)
            targetH = (origH * scale).toInt().coerceAtLeast(1)
            needUpscaleFinal = true
        }

        // ── Upscale matte trực tiếp về target size ──
        val upscaled = if (inputW == targetW && inputH == targetH) matte
                       else MaskProcessor.bilinearUpscale(matte, inputW, inputH, targetW, targetH)

        // ── Lightweight refinement: sigmoid + edge smooth ──
        val refined = lightweightRefine(upscaled, targetW, targetH)

        // ── Upscale về original nếu cần ──
        val finalMask = if (!needUpscaleFinal) refined
                        else MaskProcessor.bilinearUpscale(refined, targetW, targetH, origW, origH)

        // ── Apply mask lên ảnh gốc ──
        return MaskProcessor.applyMaskSimple(bitmap, finalMask, origW, origH)
    }

    /**
     * Lightweight refinement — nhanh, ít allocation.
     * Sigmoid tăng contrast + edge smooth nhẹ, in-place khi có thể.
     */
    private fun lightweightRefine(
        mask: FloatArray,
        width: Int,
        height: Int
    ): FloatArray {
        val n = mask.size

        // Sigmoid refine in-place: tăng contrast, cắt sạch vùng mờ
        // Dùng 2 mức: steep cho vùng chắc chắn, nhẹ hơn cho transition
        for (i in 0 until n) {
            val v = mask[i]
            mask[i] = when {
                v > 0.95f -> 1f      // fast path: chắc chắn foreground
                v < 0.05f -> 0f      // fast path: chắc chắn background
                else -> {
                    // Adaptive steepness: steep hơn khi gần 0/1
                    val steepness = if (v > 0.8f || v < 0.2f) 12f else 8f
                    (1f / (1f + exp(-steepness * (v - 0.5f))))
                }
            }
        }

        // Edge smooth 1-pass: chỉ smooth pixel ở viền, không tạo array mới nếu không cần
        return smoothEdgesFast(mask, width, height)
    }

    /**
     * Fast edge smoothing — 1 pass, chỉ xử lý pixel ở viền.
     * Sử dụng 4-neighbor check để detect edge, box average 3×3 cho edge pixels.
     */
    private fun smoothEdgesFast(mask: FloatArray, width: Int, height: Int): FloatArray {
        val result = mask.copyOf()

        for (y in 1 until height - 1) {
            val rowOffset = y * width
            for (x in 1 until width - 1) {
                val idx = rowOffset + x
                val v = mask[idx]

                // Fast edge detection: check 4 neighbors
                val isFg = v > 0.5f
                val up = mask[idx - width] > 0.5f
                val down = mask[idx + width] > 0.5f
                val left = mask[idx - 1] > 0.5f
                val right = mask[idx + 1] > 0.5f

                if (isFg == up && isFg == down && isFg == left && isFg == right) continue

                // Edge pixel → 3×3 box average
                result[idx] = (
                    mask[idx - width - 1] + mask[idx - width] + mask[idx - width + 1] +
                    mask[idx - 1] + v + mask[idx + 1] +
                    mask[idx + width - 1] + mask[idx + width] + mask[idx + width + 1]
                ) / 9f
            }
        }
        return result
    }

    /**
     * Chạy inference MODNet ở 1 scale cụ thể.
     * Tối ưu: single-pass CHW conversion, tránh Color.red/green/blue overhead.
     * @return alpha matte FloatArray [inputH * inputW]
     */
    private fun runInference(
        sess: OrtSession,
        bitmap: Bitmap,
        inputW: Int,
        inputH: Int
    ): FloatArray {
        val pixelCount = inputW * inputH

        // Resize input
        val resized = Bitmap.createScaledBitmap(bitmap, inputW, inputH, true)

        // Đọc pixels 1 lần
        val pixels = IntArray(pixelCount)
        resized.getPixels(pixels, 0, inputW, 0, 0, inputW, inputH)
        if (resized != bitmap) resized.recycle()

        // Chuẩn bị input tensor [1, 3, H, W] — CHW format
        // Single-pass: extract R, G, B planes đồng thời, tránh 3 lần duyệt
        val inputBuffer = FloatBuffer.allocate(3 * pixelCount)
        val rOffset = 0
        val gOffset = pixelCount
        val bOffset = pixelCount * 2

        // Direct array access cho FloatBuffer — nhanh hơn .put() từng phần tử
        val bufArray = FloatArray(3 * pixelCount)
        for (i in 0 until pixelCount) {
            val pixel = pixels[i]
            // Bit-shift trực tiếp thay vì Color.red/green/blue (tránh method call overhead)
            bufArray[rOffset + i] = ((pixel shr 16) and 0xFF) * NORM_SCALE + NORM_OFFSET
            bufArray[gOffset + i] = ((pixel shr 8) and 0xFF) * NORM_SCALE + NORM_OFFSET
            bufArray[bOffset + i] = (pixel and 0xFF) * NORM_SCALE + NORM_OFFSET
        }
        inputBuffer.put(bufArray)
        inputBuffer.rewind()

        // Chạy inference
        val inputShape = longArrayOf(1, 3, inputH.toLong(), inputW.toLong())
        val inputTensor = OnnxTensor.createTensor(ortEnv, inputBuffer, inputShape)
        val inputName = cachedInputName ?: sess.inputNames.first()
        val results = sess.run(mapOf(inputName to inputTensor))

        // Đọc output alpha matte [1, 1, H, W]
        val outputTensor = results[0] as OnnxTensor
        val outputData = outputTensor.floatBuffer
        outputData.rewind()

        val matteValues = FloatArray(pixelCount)
        outputData.get(matteValues) // Bulk get — nhanh hơn get() từng phần tử

        // Clamp in-place — chỉ clamp nếu cần
        for (i in matteValues.indices) {
            val v = matteValues[i]
            if (v < 0f) matteValues[i] = 0f
            else if (v > 1f) matteValues[i] = 1f
        }

        // Cleanup
        inputTensor.close()
        results.close()

        return matteValues
    }

    /**
     * Kiểm tra xem model MODNet có sẵn trong assets không
     */
    fun isModelAvailable(): Boolean {
        return try {
            context.assets.open(MODEL_FILE).close()
            true
        } catch (_: Exception) {
            false
        }
    }

    fun close() {
        session?.close()
        session = null
        cachedInputName = null
    }
}
