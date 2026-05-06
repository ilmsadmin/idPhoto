package com.idphoto.app.processing

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs

/**
 * ML Kit Quality Check — Pipeline step 4.
 *
 * CameraX → FaceMesh → Face Alignment → **Quality Check** → MODNet → Export
 *
 * Kiểm tra chất lượng ảnh thẻ:
 * - Có phát hiện khuôn mặt không
 * - Khuôn mặt có ở giữa ảnh không
 * - Mắt có mở không (blink detection)
 * - Khuôn mặt có nhìn thẳng không (euler angles)
 * - Khuôn mặt có mỉm cười quá không (optional cho ảnh thẻ)
 * - Ánh sáng có đủ không (brightness check)
 * - Ảnh có bị mờ không (blur detection)
 */
class QualityChecker {

    private var accurateDetector: com.google.mlkit.vision.face.FaceDetector? = null
    private var fastDetector: com.google.mlkit.vision.face.FaceDetector? = null

    private fun getAccurateDetector(): com.google.mlkit.vision.face.FaceDetector {
        val cached = accurateDetector
        if (cached != null) return cached
        return FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setMinFaceSize(0.15f)
                .build()
        ).also { accurateDetector = it }
    }

    private fun getFastDetector(): com.google.mlkit.vision.face.FaceDetector {
        val cached = fastDetector
        if (cached != null) return cached
        return FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setMinFaceSize(0.2f)
                .build()
        ).also { fastDetector = it }
    }

    data class QualityResult(
        val isAcceptable: Boolean,
        val score: Float,             // 0..1 overall quality
        val issues: List<QualityIssue>,
        val faceDetected: Boolean,
        val faceCentered: Boolean,
        val eyesOpen: Boolean,
        val faceStraight: Boolean,
        val goodLighting: Boolean,
        val notBlurry: Boolean,
    )

    data class QualityIssue(
        val type: IssueType,
        val severity: Severity,
        val message: String,
    )

    enum class IssueType {
        NO_FACE,
        MULTIPLE_FACES,
        FACE_NOT_CENTERED,
        FACE_TOO_SMALL,
        FACE_TOO_LARGE,
        EYES_CLOSED,
        FACE_ROTATED,
        FACE_TILTED,
        LOW_LIGHT,
        BLURRY,
        SMILING_TOO_MUCH,
    }

    enum class Severity {
        ERROR,      // Phải sửa
        WARNING,    // Nên sửa
        INFO,       // Thông tin
    }

    /**
     * Chạy kiểm tra chất lượng đầy đủ.
     */
    suspend fun checkQuality(bitmap: Bitmap): QualityResult {
        return suspendCancellableCoroutine { cont ->
            val inputImage = InputImage.fromBitmap(bitmap, 0)

            getAccurateDetector().process(inputImage)
                .addOnSuccessListener { faces ->
                    val issues = mutableListOf<QualityIssue>()

                    // ── Check 1: Face detected ──
                    if (faces.isEmpty()) {
                        issues.add(QualityIssue(
                            IssueType.NO_FACE, Severity.ERROR,
                            "No face detected"
                        ))
                        if (cont.isActive) {
                            cont.resume(QualityResult(
                                isAcceptable = false, score = 0f, issues = issues,
                                faceDetected = false, faceCentered = false,
                                eyesOpen = false, faceStraight = false,
                                goodLighting = false, notBlurry = true,
                            ))
                        }
                        return@addOnSuccessListener
                    }

                    if (faces.size > 1) {
                        issues.add(QualityIssue(
                            IssueType.MULTIPLE_FACES, Severity.WARNING,
                            "Multiple faces detected"
                        ))
                    }

                    val face = faces[0]
                    val bounds = face.boundingBox

                    // ── Check 2: Face centered ──
                    val imgCenterX = bitmap.width / 2f
                    val imgCenterY = bitmap.height / 2f
                    val faceCenterX = bounds.centerX().toFloat()
                    val faceCenterY = bounds.centerY().toFloat()
                    val offsetX = abs(faceCenterX - imgCenterX) / bitmap.width
                    val offsetY = abs(faceCenterY - imgCenterY) / bitmap.height
                    val faceCentered = offsetX < 0.15f && offsetY < 0.2f

                    if (!faceCentered) {
                        issues.add(QualityIssue(
                            IssueType.FACE_NOT_CENTERED, Severity.WARNING,
                            "Face is not centered"
                        ))
                    }

                    // ── Check 3: Face size ──
                    val faceRatio = bounds.height().toFloat() / bitmap.height
                    if (faceRatio < 0.25f) {
                        issues.add(QualityIssue(
                            IssueType.FACE_TOO_SMALL, Severity.WARNING,
                            "Face too small, move closer"
                        ))
                    } else if (faceRatio > 0.85f) {
                        issues.add(QualityIssue(
                            IssueType.FACE_TOO_LARGE, Severity.WARNING,
                            "Face too large, move back"
                        ))
                    }

                    // ── Check 4: Eyes open ──
                    val leftEyeOpen = face.leftEyeOpenProbability ?: 0.5f
                    val rightEyeOpen = face.rightEyeOpenProbability ?: 0.5f
                    val eyesOpen = leftEyeOpen > 0.4f && rightEyeOpen > 0.4f

                    if (!eyesOpen) {
                        issues.add(QualityIssue(
                            IssueType.EYES_CLOSED, Severity.ERROR,
                            "Eyes are closed, please open your eyes"
                        ))
                    }

                    // ── Check 5: Face straight (euler angles) ──
                    val yaw = face.headEulerAngleY    // Left-right rotation
                    val pitch = face.headEulerAngleX   // Up-down tilt
                    val roll = face.headEulerAngleZ     // Head tilt

                    val faceStraight = abs(yaw) < 15f && abs(pitch) < 15f && abs(roll) < 10f

                    if (abs(yaw) >= 15f) {
                        issues.add(QualityIssue(
                            IssueType.FACE_ROTATED, Severity.WARNING,
                            "Please look straight at the camera"
                        ))
                    }
                    if (abs(roll) >= 10f) {
                        issues.add(QualityIssue(
                            IssueType.FACE_TILTED, Severity.WARNING,
                            "Please keep your head straight"
                        ))
                    }

                    // ── Check 6: Smiling ──
                    val smilingProb = face.smilingProbability ?: 0f
                    if (smilingProb > 0.7f) {
                        issues.add(QualityIssue(
                            IssueType.SMILING_TOO_MUCH, Severity.INFO,
                            "ID photos should have a neutral expression"
                        ))
                    }

                    // ── Check 7: Brightness ──
                    val goodLighting = checkBrightness(bitmap)
                    if (!goodLighting) {
                        issues.add(QualityIssue(
                            IssueType.LOW_LIGHT, Severity.WARNING,
                            "Low light, please move to a brighter area"
                        ))
                    }

                    // ── Check 8: Blur ──
                    val notBlurry = checkSharpness(bitmap)
                    if (!notBlurry) {
                        issues.add(QualityIssue(
                            IssueType.BLURRY, Severity.WARNING,
                            "Photo is blurry, hold the phone steady"
                        ))
                    }

                    // ── Calculate overall score ──
                    val errorCount = issues.count { it.severity == Severity.ERROR }
                    val warnCount = issues.count { it.severity == Severity.WARNING }
                    val score = (1f - errorCount * 0.3f - warnCount * 0.1f).coerceIn(0f, 1f)
                    val acceptable = errorCount == 0

                    if (cont.isActive) {
                        cont.resume(QualityResult(
                            isAcceptable = acceptable,
                            score = score,
                            issues = issues,
                            faceDetected = true,
                            faceCentered = faceCentered,
                            eyesOpen = eyesOpen,
                            faceStraight = faceStraight,
                            goodLighting = goodLighting,
                            notBlurry = notBlurry,
                        ))
                    }
                }
                .addOnFailureListener { e ->
                    if (cont.isActive) cont.resumeWithException(e)
                }
        }
    }

    /**
     * Quick quality check — dùng cho real-time camera preview.
     * Kiểm tra: face detection, angles, lighting, background uniformity.
     */
    fun quickCheck(bitmap: Bitmap, callback: (QuickCheckResult) -> Unit) {
        // Check lighting & background in parallel (không cần face detection)
        val lightingOk = quickCheckBrightness(bitmap)
        val backgroundOk = quickCheckBackground(bitmap)

        val inputImage = InputImage.fromBitmap(bitmap, 0)
        getFastDetector().process(inputImage)
            .addOnSuccessListener { faces ->
                if (faces.isEmpty()) {
                    callback(QuickCheckResult(
                        faceDetected = false,
                        faceStraight = false,
                        eyesOpen = false,
                        goodLighting = lightingOk,
                        goodBackground = backgroundOk,
                        hint = "No face detected",
                    ))
                } else {
                    val face = faces[0]
                    val straight = abs(face.headEulerAngleY) < 20f && abs(face.headEulerAngleZ) < 15f
                    val eyesOk = (face.leftEyeOpenProbability ?: 1f) > 0.3f &&
                                 (face.rightEyeOpenProbability ?: 1f) > 0.3f
                    val hint = when {
                        !lightingOk -> "Need more light"
                        !straight -> "Look straight"
                        !eyesOk -> "Open your eyes"
                        !backgroundOk -> "Background not uniform"
                        else -> "Good! Tap to capture"
                    }
                    callback(QuickCheckResult(
                        faceDetected = true,
                        faceStraight = straight,
                        eyesOpen = eyesOk,
                        goodLighting = lightingOk,
                        goodBackground = backgroundOk,
                        hint = hint,
                    ))
                }
            }
            .addOnFailureListener {
                callback(QuickCheckResult(
                    faceDetected = false,
                    faceStraight = false,
                    eyesOpen = false,
                    goodLighting = lightingOk,
                    goodBackground = backgroundOk,
                    hint = "Detection error",
                ))
            }
    }

    fun close() {
        accurateDetector?.close()
        fastDetector?.close()
        accurateDetector = null
        fastDetector = null
    }

    data class QuickCheckResult(
        val faceDetected: Boolean,
        val faceStraight: Boolean,
        val eyesOpen: Boolean,
        val goodLighting: Boolean,
        val goodBackground: Boolean,
        val hint: String,
    )

    /**
     * Quick brightness check — sample nhẹ, dùng cho real-time.
     * Kiểm tra ảnh quá tối hoặc quá sáng (overexposed).
     */
    private fun quickCheckBrightness(bitmap: Bitmap): Boolean {
        val sampleStep = (bitmap.width / 20).coerceAtLeast(1)
        var totalBrightness = 0f
        var count = 0

        for (y in 0 until bitmap.height step sampleStep) {
            for (x in 0 until bitmap.width step sampleStep) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                totalBrightness += (0.299f * r + 0.587f * g + 0.114f * b)
                count++
            }
        }

        val avg = totalBrightness / count
        // Quá tối (< 50) hoặc quá sáng (> 230) đều không tốt
        return avg in 50f..230f
    }

    /**
     * Quick background uniformity check — kiểm tra 4 góc ảnh có đồng nhất không.
     * Nền ảnh thẻ nên sạch, đồng màu ở 4 góc.
     */
    private fun quickCheckBackground(bitmap: Bitmap): Boolean {
        val w = bitmap.width
        val h = bitmap.height
        // Sample vùng 4 góc (10% mỗi bên)
        val sX = (w * 0.1f).toInt().coerceAtLeast(1)
        val sY = (h * 0.1f).toInt().coerceAtLeast(1)

        val corners = listOf(
            sampleRegionAvg(bitmap, 0, 0, sX, sY),                       // Top-left
            sampleRegionAvg(bitmap, w - sX, 0, sX, sY),                  // Top-right
            sampleRegionAvg(bitmap, 0, h - sY, sX, sY),                  // Bottom-left
            sampleRegionAvg(bitmap, w - sX, h - sY, sX, sY),             // Bottom-right
        )

        // Kiểm tra variance giữa 4 góc — nếu quá khác nhau thì nền không đồng nhất
        val avgR = corners.map { it.first }.average().toFloat()
        val avgG = corners.map { it.second }.average().toFloat()
        val avgB = corners.map { it.third }.average().toFloat()

        var maxDiff = 0f
        for ((r, g, b) in corners) {
            val diff = abs(r - avgR) + abs(g - avgG) + abs(b - avgB)
            if (diff > maxDiff) maxDiff = diff
        }

        // Threshold: nếu sai biệt > 80 thì nền không ổn
        return maxDiff < 80f
    }

    /**
     * Tính màu trung bình của vùng (x, y, w, h).
     */
    private fun sampleRegionAvg(bitmap: Bitmap, x: Int, y: Int, w: Int, h: Int): Triple<Float, Float, Float> {
        val step = 3 // Sample mỗi 3 pixel cho nhanh
        var totalR = 0f
        var totalG = 0f
        var totalB = 0f
        var count = 0
        val safeX = x.coerceIn(0, bitmap.width - 1)
        val safeY = y.coerceIn(0, bitmap.height - 1)
        val safeMaxX = (x + w).coerceIn(0, bitmap.width)
        val safeMaxY = (y + h).coerceIn(0, bitmap.height)

        for (py in safeY until safeMaxY step step) {
            for (px in safeX until safeMaxX step step) {
                val pixel = bitmap.getPixel(px, py)
                totalR += (pixel shr 16) and 0xFF
                totalG += (pixel shr 8) and 0xFF
                totalB += pixel and 0xFF
                count++
            }
        }

        if (count == 0) return Triple(0f, 0f, 0f)
        return Triple(totalR / count, totalG / count, totalB / count)
    }

    /**
     * Kiểm tra độ sáng trung bình của ảnh.
     */
    private fun checkBrightness(bitmap: Bitmap): Boolean {
        val sampleSize = 50
        val stepX = (bitmap.width / sampleSize).coerceAtLeast(1)
        val stepY = (bitmap.height / sampleSize).coerceAtLeast(1)
        var totalBrightness = 0f
        var count = 0

        for (y in 0 until bitmap.height step stepY) {
            for (x in 0 until bitmap.width step stepX) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                totalBrightness += (0.299f * r + 0.587f * g + 0.114f * b)
                count++
            }
        }

        val avgBrightness = totalBrightness / count
        return avgBrightness > 60f  // Threshold for adequate lighting
    }

    /**
     * Kiểm tra ảnh có bị mờ không — dùng Laplacian variance.
     */
    private fun checkSharpness(bitmap: Bitmap): Boolean {
        // Downsample for speed
        val small = Bitmap.createScaledBitmap(bitmap, 200, 200, true)
        val w = small.width
        val h = small.height
        val pixels = IntArray(w * h)
        small.getPixels(pixels, 0, w, 0, 0, w, h)

        // Convert to grayscale
        val gray = FloatArray(w * h)
        for (i in pixels.indices) {
            val p = pixels[i]
            gray[i] = 0.299f * ((p shr 16) and 0xFF) +
                      0.587f * ((p shr 8) and 0xFF) +
                      0.114f * (p and 0xFF)
        }

        // Laplacian variance
        var sumVariance = 0.0
        var count = 0
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val idx = y * w + x
                val laplacian = gray[idx - 1] + gray[idx + 1] +
                    gray[idx - w] + gray[idx + w] - 4 * gray[idx]
                sumVariance += laplacian * laplacian
                count++
            }
        }

        val variance = sumVariance / count
        small.recycle()

        return variance > 100.0  // Threshold for sharpness
    }
}
