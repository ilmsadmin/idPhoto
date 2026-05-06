package com.idphoto.app.processing

import android.graphics.Bitmap
import android.graphics.Color
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import com.google.mlkit.vision.segmentation.Segmentation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * ML Kit Selfie Segmentation.
 *
 * Pipeline mới (v3):
 * 1. Bilinear upscale
 * 2. Hard threshold (cắt sạch, không soft alpha)
 * 3. Erode mask 2px (loại bỏ nền sót ở rìa)
 * 4. Gaussian blur 1px CHỈ ở viền (anti-alias nhẹ)
 * 5. Apply mask giữ nguyên pixel gốc
 */
class MLKitSegmenter {

    // Background executor cho ML Kit callbacks — tránh block Main thread
    // (ML Kit mặc định dispatch callback về Main, khiến UI bị đơ)
    private val callbackExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "MLKitSeg-Callback").apply { priority = Thread.NORM_PRIORITY - 1 }
    }

    private val segmenter by lazy {
        val options = SelfieSegmenterOptions.Builder()
            .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
            .enableRawSizeMask()
            .build()
        Segmentation.getClient(options)
    }

    suspend fun removeBackground(bitmap: Bitmap): Bitmap {
        return suspendCancellableCoroutine { cont ->
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            segmenter.process(inputImage)
                .addOnSuccessListener(callbackExecutor) { segmentationMask ->
                    try {
                        val mask = segmentationMask.buffer
                        val maskWidth = segmentationMask.width
                        val maskHeight = segmentationMask.height

                        val result = applyRefinedMask(bitmap, mask, maskWidth, maskHeight)
                        if (cont.isActive) cont.resume(result)
                    } catch (e: Exception) {
                        if (cont.isActive) cont.resumeWithException(e)
                    }
                }
                .addOnFailureListener(callbackExecutor) { e ->
                    if (cont.isActive) cont.resumeWithException(e)
                }
        }
    }

    private fun applyRefinedMask(
        original: Bitmap,
        maskBuffer: ByteBuffer,
        maskWidth: Int,
        maskHeight: Int
    ): Bitmap {
        val width = original.width
        val height = original.height

        // 1. Đọc raw mask
        maskBuffer.rewind()
        val rawMask = FloatArray(maskWidth * maskHeight)
        for (i in rawMask.indices) {
            rawMask[i] = maskBuffer.float
        }

        // 2. Bilinear upscale → kích thước ảnh gốc
        val upscaled = MaskProcessor.bilinearUpscale(rawMask, maskWidth, maskHeight, width, height)

        // 3. Hard threshold — cắt dứt khoát, loại bỏ vùng transition chứa nền
        //    Threshold 0.6 (hơi cao) → ưu tiên cắt sạch nền hơn giữ chi tiết
        val hardMask = MaskProcessor.hardThreshold(upscaled, threshold = 0.6f)

        // 4. Erode 2px — thu nhỏ mask, cắt bỏ pixel nền sót ở rìa
        val eroded = MaskProcessor.erode(hardMask, width, height, radius = 2)

        // 5. Smooth edge 1px — chỉ anti-alias ở viền, giữ nguyên bên trong
        val smoothed = MaskProcessor.smoothEdgeOnly(eroded, width, height, smoothRadius = 1)

        // 6. Apply mask — giữ nguyên pixel gốc, chỉ set alpha
        return MaskProcessor.applyMaskSimple(original, smoothed, width, height)
    }

    fun close() {
        callbackExecutor.shutdown()
        segmenter.close()
    }
}
