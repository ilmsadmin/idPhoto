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

        // ID Photo standards: face should be 70-80% of photo height (ICAO)
        private const val FACE_RATIO_IN_PHOTO = 0.72f
        // Gentle auto-zoom ratio: face chiếm ~45% ảnh thẻ — crop nhẹ, giữ nhiều thân
        // hơn so với chuẩn ICAO. User có thể pinch-zoom tiếp trong EditScreen.
        private const val GENTLE_FACE_RATIO = 0.45f
        // Khoảng cách từ tâm mắt lên đỉnh đầu (bao gồm cả tóc) — tỉ lệ theo eyeDistance.
        // Thực tế đo: với người lớn ~1.6, trẻ em tóc dày có thể 1.8–2.0.
        // Chọn 1.9 để an toàn, tránh cắt tóc.
        private const val EYES_TO_TOP_OF_HEAD_RATIO = 1.9f
        // Khoảng trống tối thiểu trên đỉnh đầu so với viền trên khung (tỉ lệ theo photoHeight)
        private const val HEAD_TOP_MARGIN = 0.05f
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

        // Background executor để ML Kit callback không dispatch lên Main
        // (callback này làm nhiều math + xoay ảnh → không được block Main)
        val bgExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()

        detector.process(inputImage)
            .addOnSuccessListener(bgExecutor) { faces ->
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
            .addOnFailureListener(bgExecutor) {
                latch.countDown()
            }

        latch.await(3, java.util.concurrent.TimeUnit.SECONDS)
        detector.close()
        bgExecutor.shutdown()

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
     * Scan bitmap từ dưới lên để tìm dòng pixel cuối cùng có subject (alpha > threshold).
     *
     * Sau khi remove BG, vùng trống dưới thân là pixel trong suốt (alpha=0).
     * Nếu ta crop theo `h` (đáy bitmap gốc), thân sẽ bị cách đáy khung.
     * → Crop theo đáy thực của subject để thân sát bottom border.
     *
     * Nếu bitmap không có alpha hoặc alpha đầy (đã composite lên nền) → trả về height.
     */
    private fun findSubjectBottom(bitmap: Bitmap): Int {
        val w = bitmap.width
        val h = bitmap.height
        if (!bitmap.hasAlpha()) return h

        val alphaThreshold = 16  // pixel có alpha > 16/255 mới tính là subject
        // Sample mỗi 2 pixel theo chiều ngang để nhanh hơn
        val step = kotlin.math.max(1, w / 200)
        val rowBuffer = IntArray(w)

        // Quét từ dòng cuối lên
        for (y in h - 1 downTo 0) {
            try {
                bitmap.getPixels(rowBuffer, 0, w, 0, y, w, 1)
            } catch (e: Exception) {
                return h
            }
            var count = 0
            var x = 0
            while (x < w) {
                val alpha = (rowBuffer[x] ushr 24) and 0xFF
                if (alpha > alphaThreshold) {
                    count++
                    // Cần ≥ 3 pixel liên tiếp trong mẫu để tránh noise
                    if (count >= 3) return (y + 2).coerceAtMost(h - 1)
                }
                x += step
            }
        }
        return h
    }

    /**
     * Tính crop rect chuẩn ảnh thẻ dựa trên vị trí khuôn mặt — KHÔNG scale.
     *
     * Chiến lược crop (gentle + bottom-anchored):
     * 1. Ước tính đỉnh đầu = faceCenter.y − eyeDistance × 1.9 (bao gồm tóc).
     * 2. Chiều cao mong muốn = eyeDistance × 2.8 / 0.45 (mặt ~45% khung, zoom nhẹ).
     * 3. **Đáy crop LUÔN = đáy bitmap** (thân sát viền dưới, cắt song song đáy, không xéo).
     * 4. Nếu chiều cao mong muốn khiến top < 0 hoặc cắt vào tóc → mở rộng targetHeight
     *    sao cho top = topOfHead − margin. Chiều rộng tính lại theo aspectRatio.
     * 5. Nếu vẫn không đủ không gian → clamp top = 0 (ưu tiên không cắt đầu hơn thân).
     */
    fun computeIdPhotoCropRect(
        bitmap: Bitmap,
        faceResult: FaceAlignmentResult,
        targetSize: PhotoSize,
    ): android.graphics.Rect {
        val w = bitmap.width.toFloat()
        val h = bitmap.height.toFloat()

        val eyeDist = faceResult.eyeDistance.coerceAtLeast(1f)
        val faceCX = faceResult.faceCenter.x
        val faceCY = faceResult.faceCenter.y

        // Đỉnh đầu ước tính (đã tính tóc)
        val topOfHead = (faceCY - eyeDist * EYES_TO_TOP_OF_HEAD_RATIO).coerceAtLeast(0f)

        // Chiều cao mong muốn (gentle zoom)
        val faceHeight = eyeDist * 2.8f
        val desiredHeight = faceHeight / GENTLE_FACE_RATIO

        // ── BƯỚC 1: Đáy crop = đáy THỰC của subject (người) ──
        // Scan alpha từ dưới lên để tìm dòng pixel cuối cùng có nội dung (không trong suốt).
        // Nếu ảnh không có alpha (đã replace BG), fallback = h.
        // Cắt NGANG, song song với đáy ảnh, và sát thân người.
        val cropBottom = findSubjectBottom(bitmap).toFloat()

        // ── BƯỚC 2: Xác định cropTop ──
        // Yêu cầu: top ≤ topOfHead − margin (để có khoảng trống phía trên đầu)
        //          top ≥ 0 (trong ảnh)
        // Ưu tiên: không cắt đầu > đủ chiều cao "zoom nhẹ"
        val marginAboveHead = desiredHeight * HEAD_TOP_MARGIN
        val requiredTop = (topOfHead - marginAboveHead).coerceAtLeast(0f)
        val gentleTop = (cropBottom - desiredHeight).coerceAtLeast(0f)

        // Dùng min(requiredTop, gentleTop): lấy top thấp hơn (= crop cao hơn)
        // → đảm bảo cả 2 điều kiện: đủ desiredHeight VÀ không cắt đầu
        var cropTop = kotlin.math.min(requiredTop, gentleTop)

        // ── BƯỚC 3: Chiều cao thực tế + chiều rộng theo aspectRatio ──
        var cropHeight = cropBottom - cropTop
        var cropWidth = cropHeight * targetSize.aspectRatio

        // ── BƯỚC 4: Nếu chiều rộng vượt bitmap → giảm chiều rộng, vẫn giữ đáy + aspect ──
        // Cách làm: giữ cropBottom = h, thu nhỏ cả height+width, raise cropTop lên.
        if (cropWidth > w) {
            cropWidth = w
            cropHeight = cropWidth / targetSize.aspectRatio
            cropTop = cropBottom - cropHeight
            // Đảm bảo top ≥ 0
            if (cropTop < 0f) {
                cropTop = 0f
                cropHeight = cropBottom - cropTop
                cropWidth = cropHeight * targetSize.aspectRatio
            }
        }

        // ── BƯỚC 5: Horizontal — center theo faceCX, clamp trong ảnh ──
        var cropLeft = faceCX - cropWidth / 2f
        if (cropLeft < 0f) cropLeft = 0f
        if (cropLeft + cropWidth > w) cropLeft = w - cropWidth

        val left = cropLeft.toInt().coerceAtLeast(0)
        val top = cropTop.toInt().coerceAtLeast(0)
        val right = (left + cropWidth.toInt()).coerceAtMost(bitmap.width)
        val bottom = (top + cropHeight.toInt()).coerceAtMost(bitmap.height)

        return android.graphics.Rect(left, top, right, bottom)
    }

    /**
     * Crop ảnh theo chuẩn ảnh thẻ dựa vào vị trí khuôn mặt — GIỮ NGUYÊN resolution.
     * Không scale về kích thước pixel chuẩn, để flow save ở DPI cao vẫn giữ chất lượng.
     */
    fun cropForIdPhotoPreserveResolution(
        bitmap: Bitmap,
        faceResult: FaceAlignmentResult,
        targetSize: PhotoSize,
    ): Bitmap {
        val rect = computeIdPhotoCropRect(bitmap, faceResult, targetSize)
        if (rect.width() <= 0 || rect.height() <= 0) {
            return bitmap
        }
        return try {
            Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height())
        } catch (e: Exception) {
            bitmap
        }
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
        val topOffset = faceHeight * 0.5f  // từ mắt lên đỉnh đầu (reduced to include more body)
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
