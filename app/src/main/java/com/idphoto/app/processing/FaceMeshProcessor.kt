package com.idphoto.app.processing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PointF
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * MediaPipe FaceMesh — 468 landmark detector.
 *
 * Pipeline step 2: CameraX → **FaceMesh** → Face Alignment → Quality Check → MODNet → Export
 *
 * Dùng FaceMesh landmarks để:
 * 1. Xác định vị trí mắt, mũi, miệng
 * 2. Tính góc nghiêng khuôn mặt
 * 3. Xoay ảnh cho mặt thẳng (deskew)
 * 4. Crop vùng mặt + vai theo tỷ lệ ảnh thẻ chuẩn
 */
class FaceMeshProcessor(private val context: Context) {

    companion object {
        // FaceMesh landmark indices
        const val LEFT_EYE_CENTER = 468     // Iris center (nếu dùng 478-point model)
        const val RIGHT_EYE_CENTER = 473

        // Fallback: dùng eye contour midpoint
        const val LEFT_EYE_INNER = 133
        const val LEFT_EYE_OUTER = 33
        const val RIGHT_EYE_INNER = 362
        const val RIGHT_EYE_OUTER = 263

        const val NOSE_TIP = 1
        const val CHIN = 152
        const val FOREHEAD = 10

        const val LEFT_MOUTH = 61
        const val RIGHT_MOUTH = 291
        const val TOP_LIP = 0
        const val BOTTOM_LIP = 17

        // ID Photo standards: face should be 70-80% of photo height
        private const val FACE_RATIO_IN_PHOTO = 0.72f
        // Head top margin: ~10% from top
        private const val HEAD_TOP_MARGIN = 0.08f
    }

    data class FaceAlignmentResult(
        val alignedBitmap: Bitmap,
        val faceCenter: PointF,
        val eyeDistance: Float,
        val rotationAngle: Float,
        val landmarks: List<PointF>? = null,
        val confidence: Float = 1.0f,
    )

    data class FaceLandmarks(
        val leftEye: PointF,
        val rightEye: PointF,
        val noseTip: PointF,
        val chin: PointF,
        val forehead: PointF,
        val leftMouth: PointF,
        val rightMouth: PointF,
    )

    /**
     * Align face in the photo — xoay cho mặt thẳng, dựa trên mắt.
     * Sử dụng ML Kit Face Detection landmarks (vì MediaPipe Tasks có thể
     * chưa stable trên tất cả devices). Fallback-safe.
     */
    fun alignFace(bitmap: Bitmap): FaceAlignmentResult {
        // Sử dụng ML Kit Face Detection để lấy landmarks
        // (đáng tin cậy hơn MediaPipe Tasks trên nhiều devices)
        return alignWithMLKitFace(bitmap)
    }

    /**
     * Align bằng ML Kit Face Detection — reliable, có sẵn trên mọi device.
     */
    private fun alignWithMLKitFace(bitmap: Bitmap): FaceAlignmentResult {
        // Sync detection using ML Kit
        val detector = com.google.mlkit.vision.face.FaceDetection.getClient(
            com.google.mlkit.vision.face.FaceDetectorOptions.Builder()
                .setPerformanceMode(com.google.mlkit.vision.face.FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(com.google.mlkit.vision.face.FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(com.google.mlkit.vision.face.FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build()
        )

        val inputImage = com.google.mlkit.vision.common.InputImage.fromBitmap(bitmap, 0)

        var result: FaceAlignmentResult? = null
        val latch = java.util.concurrent.CountDownLatch(1)

        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    val face = faces[0]
                    val rotZ = face.headEulerAngleZ  // Roll angle

                    // Get eye positions
                    val leftEye = face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.LEFT_EYE)
                    val rightEye = face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.RIGHT_EYE)

                    if (leftEye != null && rightEye != null) {
                        val lp = leftEye.position
                        val rp = rightEye.position

                        val eyeDist = sqrt(
                            (rp.x - lp.x) * (rp.x - lp.x) + (rp.y - lp.y) * (rp.y - lp.y)
                        )

                        val eyeCenter = PointF(
                            (lp.x + rp.x) / 2f,
                            (lp.y + rp.y) / 2f
                        )

                        // Deskew: xoay ảnh cho 2 mắt nằm ngang
                        val angle = atan2(
                            (rp.y - lp.y).toDouble(),
                            (rp.x - lp.x).toDouble()
                        ).toFloat() * (180f / Math.PI.toFloat())

                        val aligned = if (kotlin.math.abs(angle) > 0.5f) {
                            rotateImage(bitmap, -angle, eyeCenter)
                        } else {
                            bitmap
                        }

                        // Tính lại faceCenter trên ảnh đã xoay (canvas mở rộng)
                        val newCenterX: Float
                        val newCenterY: Float
                        if (kotlin.math.abs(angle) > 0.5f) {
                            // Ảnh đã xoay quanh tâm bitmap gốc → tâm mới offset
                            val rad = Math.toRadians((-angle).toDouble())
                            val cos = kotlin.math.cos(rad).toFloat()
                            val sin = kotlin.math.sin(rad).toFloat()
                            val cx = bitmap.width / 2f
                            val cy = bitmap.height / 2f
                            // Xoay điểm faceCenter quanh tâm gốc
                            val dx = eyeCenter.x - cx
                            val dy = eyeCenter.y - cy
                            val rx = dx * cos - dy * sin
                            val ry = dx * sin + dy * cos
                            newCenterX = rx + aligned.width / 2f
                            newCenterY = ry + aligned.height / 2f
                        } else {
                            newCenterX = eyeCenter.x
                            newCenterY = eyeCenter.y
                        }

                        result = FaceAlignmentResult(
                            alignedBitmap = aligned,
                            faceCenter = PointF(newCenterX, newCenterY),
                            eyeDistance = eyeDist,
                            rotationAngle = angle,
                            confidence = face.trackingId?.toFloat()?.let { 1.0f } ?: 0.9f,
                        )
                    }
                }
                latch.countDown()
            }
            .addOnFailureListener {
                latch.countDown()
            }

        latch.await(3, java.util.concurrent.TimeUnit.SECONDS)
        detector.close()

        return result ?: FaceAlignmentResult(
            alignedBitmap = bitmap,
            faceCenter = PointF(bitmap.width / 2f, bitmap.height / 2f),
            eyeDistance = bitmap.width * 0.25f,
            rotationAngle = 0f,
            confidence = 0f,
        )
    }

    /**
     * Xoay ảnh quanh tâm ảnh — mở rộng canvas để không bị cắt góc.
     */
    private fun rotateImage(bitmap: Bitmap, degrees: Float, center: PointF): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val rad = Math.toRadians(degrees.toDouble())
        val cos = kotlin.math.abs(kotlin.math.cos(rad))
        val sin = kotlin.math.abs(kotlin.math.sin(rad))

        // Kích thước canvas mới để chứa toàn bộ ảnh sau xoay
        val newW = (w * cos + h * sin).toInt()
        val newH = (w * sin + h * cos).toInt()

        val result = Bitmap.createBitmap(newW, newH, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(result)

        // Dịch tâm canvas rồi xoay
        canvas.translate(newW / 2f, newH / 2f)
        canvas.rotate(degrees)
        canvas.translate(-w / 2f, -h / 2f)
        canvas.drawBitmap(bitmap, 0f, 0f, null)

        return result
    }

    /**
     * Crop ảnh theo chuẩn ảnh thẻ — dựa vào vị trí khuôn mặt.
     * Đảm bảo khuôn mặt chiếm đúng tỷ lệ theo ICAO / ID photo standards.
     */
    fun cropForIdPhoto(
        bitmap: Bitmap,
        faceResult: FaceAlignmentResult,
        targetSize: PhotoSize,
    ): Bitmap {
        val w = bitmap.width.toFloat()
        val h = bitmap.height.toFloat()

        // Ước tính chiều cao mặt dựa trên khoảng cách 2 mắt
        // Face height ≈ eye distance * 2.8 (tỷ lệ trung bình)
        val faceHeight = faceResult.eyeDistance * 2.8f

        // Chiều cao ảnh thẻ cần: mặt chiếm FACE_RATIO_IN_PHOTO
        val targetPhotoHeight = faceHeight / FACE_RATIO_IN_PHOTO
        val targetPhotoWidth = targetPhotoHeight * targetSize.aspectRatio

        // Tính crop rect — center on face, offset lên trên 1 chút
        val faceCY = faceResult.faceCenter.y
        val faceCX = faceResult.faceCenter.x

        // Đỉnh đầu cách top ảnh ~ HEAD_TOP_MARGIN
        val topOffset = faceHeight * 0.6f  // từ mắt lên đỉnh đầu
        val cropTop = (faceCY - topOffset - targetPhotoHeight * HEAD_TOP_MARGIN)
            .coerceIn(0f, h - targetPhotoHeight)
        val cropLeft = (faceCX - targetPhotoWidth / 2f)
            .coerceIn(0f, w - targetPhotoWidth)

        val cropW = targetPhotoWidth.toInt().coerceAtMost(bitmap.width)
        val cropH = targetPhotoHeight.toInt().coerceAtMost(bitmap.height)

        if (cropW <= 0 || cropH <= 0) {
            return PhotoSizeManager.cropToSize(bitmap, targetSize)
        }

        val safeLeft = cropLeft.toInt().coerceIn(0, bitmap.width - cropW)
        val safeTop = cropTop.toInt().coerceIn(0, bitmap.height - cropH)

        return try {
            val cropped = Bitmap.createBitmap(bitmap, safeLeft, safeTop, cropW, cropH)
            Bitmap.createScaledBitmap(cropped, targetSize.widthPx, targetSize.heightPx, true)
        } catch (e: Exception) {
            PhotoSizeManager.cropToSize(bitmap, targetSize)
        }
    }
}
