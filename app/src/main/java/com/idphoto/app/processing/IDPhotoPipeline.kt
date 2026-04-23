package com.idphoto.app.processing

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Thrown when the pipeline cannot find a human face in the input photo.
 */
// Note: This default message should be localized. Using English as default.
class NoFaceDetectedException(message: String = "No face detected") : Exception(message)

/**
 * ID Photo Processing Pipeline — orchestrator.
 *
 * Pipeline theo thứ tự:
 * 1. CameraX          → Capture ảnh thô
 * 2. MediaPipe FaceMesh → Phát hiện 468 landmarks khuôn mặt
 * 3. Face Alignment    → Deskew (xoay thẳng mặt) dựa trên eye landmarks
 * 4. ML Kit Quality    → Kiểm tra chất lượng (eyes open, face straight, lighting, blur)
 * 5. MODNet Segmentation → Tách nền chính xác (ONNX inference)
 * 6. Export ID Photo   → Ghép nền + crop theo size + beauty + print layout
 *
 * Mỗi step có thể skip nếu fail — luôn trả về kết quả best-effort.
 */
class IDPhotoPipeline(private val context: Context) {

    private val faceMeshProcessor = FaceMeshProcessor(context)
    private val qualityChecker = QualityChecker()
    private val mlKitSegmenter = MLKitSegmenter()
    private val modNetProcessor = MODNetProcessor(context)

    data class PipelineResult(
        val originalBitmap: Bitmap,
        val alignedBitmap: Bitmap,
        val qualityResult: QualityChecker.QualityResult?,
        val segmentedBitmap: Bitmap?,          // Foreground with alpha
        val faceAlignment: FaceMeshProcessor.FaceAlignmentResult?,
        val pipelineSteps: List<PipelineStep>,
    )

    data class PipelineStep(
        val name: String,
        val status: StepStatus,
        val durationMs: Long = 0,
        val message: String = "",
    )

    enum class StepStatus {
        PENDING,
        RUNNING,
        SUCCESS,
        SKIPPED,
        FAILED,
    }

    interface PipelineCallback {
        fun onStepStarted(stepName: String, stepIndex: Int, totalSteps: Int)
        fun onStepCompleted(stepName: String, stepIndex: Int, status: StepStatus)
        fun onPreviewReady(previewBitmap: Bitmap)  // ML Kit fast preview
        fun onComplete(result: PipelineResult)
        fun onError(error: Exception)
        /** Progress 0f..1f — gọi nhiều lần trong khi xử lý để cập nhật UI. */
        fun onProgress(progress: Float) {}
    }

    companion object {
        // Giới hạn chiều dài cạnh dài nhất của ảnh đầu vào.
        // Ảnh gallery thường 4000×6000 → downscale 2048 để nhanh hơn nhiều lần
        // mà vẫn đủ chất lượng cho ảnh thẻ (max ~600×800 ở 300 DPI).
        private const val MAX_INPUT_SIDE = 2048
    }

    /**
     * Downscale ảnh đầu vào nếu quá lớn — giữ tỷ lệ.
     */
    private fun downscaleIfNeeded(bitmap: Bitmap): Bitmap {
        val maxSide = maxOf(bitmap.width, bitmap.height)
        if (maxSide <= MAX_INPUT_SIDE) return bitmap
        val scale = MAX_INPUT_SIDE.toFloat() / maxSide
        val newW = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val newH = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }

    /**
     * Chạy full pipeline.
     *
     * @param bitmap Ảnh gốc từ CameraX hoặc Gallery
     * @param callback Callback cho UI updates
     */
    suspend fun process(bitmap: Bitmap, callback: PipelineCallback) {
        val steps = mutableListOf<PipelineStep>()
        val totalSteps = 4 // FaceMesh → Alignment → Quality → Segmentation

        // Toàn bộ pipeline chạy trên Dispatchers.Default để KHÔNG bao giờ block Main.
        // Main thread luôn rảnh → loading animation mượt, UI responsive.
        withContext(Dispatchers.Default) {
            try {
                // ━━━━ Step 0: Pre-process — downscale ảnh đầu vào nếu quá lớn ━━━━
                // Tối ưu tốc độ: 4000×6000 → 2048×3072 giúp ML Kit/MODNet nhanh ~3-4x
                val input = downscaleIfNeeded(bitmap)
                callback.onProgress(0.05f)

                // ━━━━ Step 1: Face Detection & Landmark (MediaPipe FaceMesh) ━━━━
            // Note: This step name should be localized. Using Vietnamese as default.
            callback.onStepStarted("Nhận diện khuôn mặt", 0, totalSteps)
            val t1 = System.currentTimeMillis()

            val faceResult = faceMeshProcessor.alignFace(input)

            val d1 = System.currentTimeMillis() - t1
            val faceStatus = if (faceResult.confidence > 0f) StepStatus.SUCCESS else StepStatus.FAILED
            // Note: This step name should be localized. Using English as default.
            steps.add(PipelineStep("FaceMesh Detection", faceStatus, d1))
            callback.onStepCompleted("", 0, faceStatus)
            callback.onProgress(0.20f)

            // ── Abort if no face detected ──
            if (faceResult.confidence <= 0f) {
                callback.onError(NoFaceDetectedException())
                return@withContext
            }

            // ━━━━ Step 2: Face Alignment (Deskew) ━━━━
            // Note: This step name should be localized. Using Vietnamese as default.
            callback.onStepStarted("Căn chỉnh khuôn mặt", 1, totalSteps)
            val t2 = System.currentTimeMillis()

            val alignedBitmap = faceResult.alignedBitmap
            val alignStatus = if (kotlin.math.abs(faceResult.rotationAngle) > 0.5f)
                StepStatus.SUCCESS else StepStatus.SKIPPED

            val d2 = System.currentTimeMillis() - t2
            // Note: These messages should be localized. Using Vietnamese as default.
            steps.add(PipelineStep("Face Alignment", alignStatus, d2,
                if (alignStatus == StepStatus.SUCCESS) "Xoay ${String.format("%.1f", faceResult.rotationAngle)}°" else "Không cần xoay"))
            callback.onStepCompleted("", 1, alignStatus)
            callback.onProgress(0.30f)

            // ━━━━ Step 3: Quality Check (ML Kit) ━━━━
            // Note: This step name should be localized. Using Vietnamese as default.
            callback.onStepStarted("Kiểm tra chất lượng", 2, totalSteps)
            val t3 = System.currentTimeMillis()

            val qualityResult = try {
                qualityChecker.checkQuality(alignedBitmap)
            } catch (e: Exception) {
                null
            }

            val d3 = System.currentTimeMillis() - t3
            val qualityStatus = when {
                qualityResult == null -> StepStatus.SKIPPED
                qualityResult.isAcceptable -> StepStatus.SUCCESS
                else -> StepStatus.SUCCESS // Still continue, just warn
            }
            // Note: These messages should be localized. Using English/Vietnamese as default.
            steps.add(PipelineStep("Quality Check", qualityStatus, d3,
                qualityResult?.let { "Score: ${(it.score * 100).toInt()}%" } ?: "Skipped"))
            callback.onStepCompleted("", 2, qualityStatus)
            callback.onProgress(0.40f)

            // ━━━━ Step 4: Background Segmentation (ML Kit fast → MODNet deep) ━━━━
            // Note: This step name should be localized. Using Vietnamese as default.
            callback.onStepStarted("Tách nền ảnh", 3, totalSteps)
            val t4 = System.currentTimeMillis()

            // Fast preview with ML Kit first
            var segmented: Bitmap? = null
            try {
                val mlKitResult = mlKitSegmenter.removeBackground(alignedBitmap)
                segmented = mlKitResult
                callback.onPreviewReady(mlKitResult)
                callback.onProgress(0.60f)
            } catch (e: Exception) {
                // ML Kit failed, continue to MODNet
            }

            // Deep segmentation with MODNet (higher quality)
            if (modNetProcessor.isModelAvailable()) {
                try {
                    val modNetResult = modNetProcessor.removeBackground(alignedBitmap)
                    segmented = modNetResult
                    callback.onProgress(0.95f)
                } catch (e: Exception) {
                    // MODNet failed, keep ML Kit result
                }
            }

            val d4 = System.currentTimeMillis() - t4
            val segStatus = if (segmented != null) StepStatus.SUCCESS else StepStatus.FAILED
            // Note: This step name should be localized. Using English as default.
            steps.add(PipelineStep("Segmentation", segStatus, d4))
            callback.onStepCompleted("", 3, segStatus)
            callback.onProgress(1.0f)

            // ━━━━ Complete ━━━━
            callback.onComplete(PipelineResult(
                originalBitmap = bitmap,
                alignedBitmap = alignedBitmap,
                qualityResult = qualityResult,
                segmentedBitmap = segmented,
                faceAlignment = faceResult,
                pipelineSteps = steps,
            ))

            } catch (e: Exception) {
                callback.onError(e)
            }
        }
    }

    fun close() {
        mlKitSegmenter.close()
        modNetProcessor.close()
    }
}
