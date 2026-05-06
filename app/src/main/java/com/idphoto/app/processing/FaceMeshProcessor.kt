package com.idphoto.app.processing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.coroutines.resume

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

    private val detector: FaceDetector by lazy {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build()
        )
    }

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
        // Passport framing ratio: face/head area chiếm vừa đủ để ra ảnh thẻ,
        // không giữ toàn thân sau khi remove background.
        private const val ID_PHOTO_FACE_RATIO = 0.48f
        private const val BODY_BELOW_EYES_RATIO = 4.6f
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
    suspend fun alignFace(bitmap: Bitmap): FaceAlignmentResult {
        // Sử dụng ML Kit Face Detection để lấy landmarks
        // (đáng tin cậy hơn MediaPipe Tasks trên nhiều devices)
        return alignWithMLKitFace(bitmap)
    }

    /**
     * Align bằng ML Kit Face Detection — reliable, có sẵn trên mọi device.
     */
    private suspend fun alignWithMLKitFace(bitmap: Bitmap): FaceAlignmentResult {
        val inputImage = InputImage.fromBitmap(bitmap, 0)

        return suspendCancellableCoroutine { cont ->
            detector.process(inputImage)
                .addOnSuccessListener { faces ->
                    val fallback = FaceAlignmentResult(
                        alignedBitmap = bitmap,
                        faceCenter = PointF(bitmap.width / 2f, bitmap.height / 2f),
                        eyeDistance = bitmap.width * 0.25f,
                        rotationAngle = 0f,
                        confidence = 0f,
                    )

                    if (faces.isEmpty()) {
                        if (cont.isActive) cont.resume(fallback)
                        return@addOnSuccessListener
                    }

                    val face = faces[0]
                    val leftEye = face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.LEFT_EYE)
                    val rightEye = face.getLandmark(com.google.mlkit.vision.face.FaceLandmark.RIGHT_EYE)
                    if (leftEye == null || rightEye == null) {
                        if (cont.isActive) cont.resume(fallback)
                        return@addOnSuccessListener
                    }

                    val lp = leftEye.position
                    val rp = rightEye.position
                    val eyeDist = sqrt((rp.x - lp.x) * (rp.x - lp.x) + (rp.y - lp.y) * (rp.y - lp.y))
                    val eyeCenter = PointF((lp.x + rp.x) / 2f, (lp.y + rp.y) / 2f)

                    val angle = atan2((rp.y - lp.y).toDouble(), (rp.x - lp.x).toDouble()).toFloat() *
                        (180f / Math.PI.toFloat())

                    val aligned = if (kotlin.math.abs(angle) > 0.5f) {
                        rotateImage(bitmap, -angle, eyeCenter)
                    } else {
                        bitmap
                    }

                    val newCenterX: Float
                    val newCenterY: Float
                    if (kotlin.math.abs(angle) > 0.5f) {
                        val rad = Math.toRadians((-angle).toDouble())
                        val cos = kotlin.math.cos(rad).toFloat()
                        val sin = kotlin.math.sin(rad).toFloat()
                        val cx = bitmap.width / 2f
                        val cy = bitmap.height / 2f
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

                    if (cont.isActive) {
                        cont.resume(
                            FaceAlignmentResult(
                                alignedBitmap = aligned,
                                faceCenter = PointF(newCenterX, newCenterY),
                                eyeDistance = eyeDist,
                                rotationAngle = angle,
                                confidence = face.trackingId?.toFloat()?.let { 1.0f } ?: 0.9f,
                            )
                        )
                    }
                }
                .addOnFailureListener {
                    if (cont.isActive) {
                        cont.resume(
                            FaceAlignmentResult(
                                alignedBitmap = bitmap,
                                faceCenter = PointF(bitmap.width / 2f, bitmap.height / 2f),
                                eyeDistance = bitmap.width * 0.25f,
                                rotationAngle = 0f,
                                confidence = 0f,
                            )
                        )
                    }
                }
        }
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
        return try {
            val canvas = android.graphics.Canvas(result)
            canvas.translate(newW / 2f, newH / 2f)
            canvas.rotate(degrees)
            canvas.translate(-w / 2f, -h / 2f)
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            result
        } catch (e: Exception) {
            result.recycle()
            throw e
        }
    }

    /**
     * Scan bitmap từ dưới lên để tìm dòng pixel cuối cùng có subject (alpha > threshold).
     *
     * Sau khi remove BG, vùng trống dưới thân là pixel trong suốt (alpha=0).
     * Nếu ta crop theo `h` (đáy bitmap gốc), thân sẽ bị cách đáy khung.
     * → Crop theo đáy thực của subject để thân sát bottom border.
     *
        * Trả về tọa độ bottom dạng exclusive, dùng trực tiếp được với Bitmap.createBitmap.
        * Nếu bitmap không có alpha hoặc alpha đầy (đã composite lên nền) → trả về height.
     */
    private fun findSubjectBottom(bitmap: Bitmap): Int {
        val w = bitmap.width
        val h = bitmap.height
        if (!bitmap.hasAlpha()) return h

            val alphaThreshold = 32
        val step = kotlin.math.max(1, w / 200)
            val sampledColumns = ((w + step - 1) / step).coerceAtLeast(1)
            val minPixelsInRow = kotlin.math.max(4, sampledColumns / 60)
        val pixels = IntArray(w * h)

        try {
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        } catch (_: Exception) {
            return h
        }

        for (y in h - 1 downTo 0) {
            var count = 0
            var x = 0
            while (x < w) {
                val alpha = (pixels[y * w + x] ushr 24) and 0xFF
                if (alpha > alphaThreshold) {
                    count++
                    if (count >= minPixelsInRow) return (y + 1).coerceIn(1, h)
                }
                x += step
            }
        }
        return h
    }

    fun close() {
        detector.close()
    }

    /**
     * Tính crop rect chuẩn ảnh thẻ dựa trên vị trí khuôn mặt — KHÔNG scale.
     *
     * Chiến lược crop (gentle + bottom-anchored):
     * 1. Ước tính đỉnh đầu = faceCenter.y − eyeDistance × 1.9 (bao gồm tóc).
        * 2. Chiều cao mong muốn = eyeDistance × 2.8 / ratio ảnh thẻ.
        * 3. Đáy crop nằm ở thân trên, không phải đáy toàn thân sau khi remove background.
        * 4. Nếu chiều cao mong muốn cắt vào tóc → mở rộng lên trên để giữ khoảng trống đầu.
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

        // Chiều cao mong muốn cho ảnh thẻ: lấy thân trên, không lấy toàn thân.
        val faceHeight = eyeDist * 2.8f
        val desiredHeight = faceHeight / ID_PHOTO_FACE_RATIO

        // ── BƯỚC 1: Đáy crop = thân trên theo chuẩn ảnh thẻ ──
        // Nếu dùng đáy subject thật, ảnh full-body sẽ bị thu nhỏ toàn thân trong khung.
        // Vì vậy crop ngang qua phần thân trên để vai/ngực chạm đáy như ảnh thẻ.
        val subjectBottom = findSubjectBottom(bitmap).toFloat()
        val passportBottom = faceCY + eyeDist * BODY_BELOW_EYES_RATIO
        val cropBottom = passportBottom.coerceIn(1f, minOf(subjectBottom, h))

        // ── BƯỚC 2: Xác định cropTop ──
        // Yêu cầu: top ≤ topOfHead − margin (để có khoảng trống phía trên đầu)
        //          top ≥ 0 (trong ảnh)
        // Ưu tiên: không cắt đầu > đúng zoom ảnh thẻ
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

        val bottom = cropBottom.roundToInt().coerceIn(1, bitmap.height)
        val height = cropHeight.roundToInt().coerceIn(1, bottom)
        val width = (height * targetSize.aspectRatio).roundToInt().coerceIn(1, bitmap.width)
        val top = (bottom - height).coerceAtLeast(0)
        val left = cropLeft.roundToInt().coerceIn(0, bitmap.width - width)
        val right = (left + width).coerceAtMost(bitmap.width)

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
