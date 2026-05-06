package com.idphoto.app.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader

/**
 * Overlay áo vest/sơ mi lên ảnh thẻ.
 *
 * Vì ảnh thẻ đã tách nền, ta vẽ áo ở vùng dưới (vai + ngực)
 * bằng hình vector đơn giản, phù hợp với ảnh thẻ chuẩn.
 */
object OutfitOverlay {

    data class OutfitOption(
        val name: String,
        val primaryColor: Int,
        val secondaryColor: Int,  // Cổ áo / cà vạt
        val type: OutfitType,
    )

    enum class OutfitType {
        NONE,
        SUIT_TIE,        // Vest + cà vạt
        SUIT_NO_TIE,     // Vest không cà vạt
        SHIRT_WHITE,     // Sơ mi trắng
        SHIRT_BLUE,      // Sơ mi xanh
        BLOUSE,          // Áo nữ (cổ tròn)
        AO_DAI,          // Áo dài Việt Nam
    }

    // Note: These outfit names are keyed by default Vietnamese names from data class.
    // Actual display names should be retrieved from Strings.outfitNamesMap based on app language.
    val outfitOptions = listOf(
        OutfitOption("Không", Color.TRANSPARENT, Color.TRANSPARENT, OutfitType.NONE),
        OutfitOption("Vest đen", Color.parseColor("#2C2C2C"), Color.parseColor("#8B0000"), OutfitType.SUIT_TIE),
        OutfitOption("Vest xanh", Color.parseColor("#1B3A5C"), Color.parseColor("#C0392B"), OutfitType.SUIT_TIE),
        OutfitOption("Vest (ko cà vạt)", Color.parseColor("#2C2C2C"), Color.WHITE, OutfitType.SUIT_NO_TIE),
        OutfitOption("Sơ mi trắng", Color.WHITE, Color.parseColor("#E8E8E8"), OutfitType.SHIRT_WHITE),
        OutfitOption("Sơ mi xanh", Color.parseColor("#1565C0"), Color.parseColor("#0D47A1"), OutfitType.SHIRT_BLUE),
        OutfitOption("Áo nữ trắng", Color.WHITE, Color.parseColor("#F0F0F0"), OutfitType.BLOUSE),
        OutfitOption("Áo dài", Color.parseColor("#CC0000"), Color.parseColor("#FFD700"), OutfitType.AO_DAI),
    )

    /**
     * Vẽ overlay áo lên ảnh đã có nền.
     * Áo được vẽ ở phần dưới của ảnh (từ ~65% chiều cao trở xuống).
     *
     * @param bitmap Ảnh đã composite (có nền)
     * @param foregroundMask Alpha mask của người (để biết vùng nào là người)
     * @param outfit Kiểu áo cần vẽ
     */
    fun applyOutfit(
        bitmap: Bitmap,
        foregroundMask: FloatArray?,
        outfit: OutfitOption
    ): Bitmap {
        if (outfit.type == OutfitType.NONE) return bitmap

        val width = bitmap.width
        val height = bitmap.height
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        when (outfit.type) {
            OutfitType.SUIT_TIE -> drawSuitWithTie(canvas, width, height, outfit)
            OutfitType.SUIT_NO_TIE -> drawSuitNoTie(canvas, width, height, outfit)
            OutfitType.SHIRT_WHITE, OutfitType.SHIRT_BLUE -> drawShirt(canvas, width, height, outfit)
            OutfitType.BLOUSE -> drawBlouse(canvas, width, height, outfit)
            OutfitType.AO_DAI -> drawAoDai(canvas, width, height, outfit)
            else -> {}
        }

        return result
    }

    // ─── Draw Suit with Tie ────────────────────────

    private fun drawSuitWithTie(canvas: Canvas, w: Int, h: Int, outfit: OutfitOption) {
        val startY = h * 0.72f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Vai + thân áo vest
        paint.color = outfit.primaryColor
        val suitPath = Path().apply {
            // Vai trái
            moveTo(0f, startY + h * 0.05f)
            lineTo(w * 0.30f, startY)
            // Cổ áo V trái
            lineTo(w * 0.42f, startY - h * 0.02f)
            lineTo(w * 0.46f, startY + h * 0.12f)
            // Giữa (V-neck)
            lineTo(w * 0.50f, startY + h * 0.08f)
            // Cổ áo V phải
            lineTo(w * 0.54f, startY + h * 0.12f)
            lineTo(w * 0.58f, startY - h * 0.02f)
            // Vai phải
            lineTo(w * 0.70f, startY)
            lineTo(w.toFloat(), startY + h * 0.05f)
            // Xuống đáy
            lineTo(w.toFloat(), h.toFloat())
            lineTo(0f, h.toFloat())
            close()
        }
        canvas.drawPath(suitPath, paint)

        // Sơ mi trắng bên trong
        paint.color = Color.WHITE
        val shirtPath = Path().apply {
            moveTo(w * 0.42f, startY - h * 0.02f)
            lineTo(w * 0.46f, startY + h * 0.12f)
            lineTo(w * 0.50f, startY + h * 0.08f)
            lineTo(w * 0.54f, startY + h * 0.12f)
            lineTo(w * 0.58f, startY - h * 0.02f)
            lineTo(w * 0.55f, startY - h * 0.04f)
            lineTo(w * 0.50f, startY)
            lineTo(w * 0.45f, startY - h * 0.04f)
            close()
        }
        canvas.drawPath(shirtPath, paint)

        // Cà vạt
        paint.color = outfit.secondaryColor
        val tiePath = Path().apply {
            moveTo(w * 0.48f, startY + h * 0.01f)
            lineTo(w * 0.52f, startY + h * 0.01f)
            lineTo(w * 0.53f, startY + h * 0.08f)
            lineTo(w * 0.50f, startY + h * 0.14f)
            lineTo(w * 0.47f, startY + h * 0.08f)
            close()
        }
        canvas.drawPath(tiePath, paint)

        // Nút cà vạt
        paint.style = Paint.Style.FILL
        canvas.drawCircle(w * 0.50f, startY + h * 0.02f, w * 0.012f, paint)

        // Lapel lines
        paint.color = Color.argb(60, 0, 0, 0)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = w * 0.003f
        val lapelLeft = Path().apply {
            moveTo(w * 0.42f, startY - h * 0.02f)
            lineTo(w * 0.38f, startY + h * 0.15f)
        }
        canvas.drawPath(lapelLeft, paint)
        val lapelRight = Path().apply {
            moveTo(w * 0.58f, startY - h * 0.02f)
            lineTo(w * 0.62f, startY + h * 0.15f)
        }
        canvas.drawPath(lapelRight, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawSuitNoTie(canvas: Canvas, w: Int, h: Int, outfit: OutfitOption) {
        val startY = h * 0.72f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Vest body
        paint.color = outfit.primaryColor
        val suitPath = Path().apply {
            moveTo(0f, startY + h * 0.05f)
            lineTo(w * 0.30f, startY)
            lineTo(w * 0.42f, startY - h * 0.02f)
            lineTo(w * 0.46f, startY + h * 0.10f)
            lineTo(w * 0.50f, startY + h * 0.06f)
            lineTo(w * 0.54f, startY + h * 0.10f)
            lineTo(w * 0.58f, startY - h * 0.02f)
            lineTo(w * 0.70f, startY)
            lineTo(w.toFloat(), startY + h * 0.05f)
            lineTo(w.toFloat(), h.toFloat())
            lineTo(0f, h.toFloat())
            close()
        }
        canvas.drawPath(suitPath, paint)

        // Sơ mi bên trong
        paint.color = outfit.secondaryColor
        val shirtPath = Path().apply {
            moveTo(w * 0.43f, startY)
            lineTo(w * 0.46f, startY + h * 0.10f)
            lineTo(w * 0.50f, startY + h * 0.06f)
            lineTo(w * 0.54f, startY + h * 0.10f)
            lineTo(w * 0.57f, startY)
            close()
        }
        canvas.drawPath(shirtPath, paint)

        // Nút áo
        paint.color = Color.argb(80, 0, 0, 0)
        canvas.drawCircle(w * 0.50f, startY + h * 0.08f, w * 0.008f, paint)
    }

    private fun drawShirt(canvas: Canvas, w: Int, h: Int, outfit: OutfitOption) {
        val startY = h * 0.72f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Thân sơ mi
        paint.color = outfit.primaryColor
        val shirtPath = Path().apply {
            moveTo(0f, startY + h * 0.06f)
            lineTo(w * 0.28f, startY)
            lineTo(w * 0.40f, startY - h * 0.01f)
            lineTo(w * 0.45f, startY + h * 0.06f)
            lineTo(w * 0.50f, startY + h * 0.04f)
            lineTo(w * 0.55f, startY + h * 0.06f)
            lineTo(w * 0.60f, startY - h * 0.01f)
            lineTo(w * 0.72f, startY)
            lineTo(w.toFloat(), startY + h * 0.06f)
            lineTo(w.toFloat(), h.toFloat())
            lineTo(0f, h.toFloat())
            close()
        }
        canvas.drawPath(shirtPath, paint)

        // Cổ áo
        paint.color = outfit.secondaryColor
        val collarLeft = Path().apply {
            moveTo(w * 0.40f, startY - h * 0.01f)
            lineTo(w * 0.45f, startY + h * 0.06f)
            lineTo(w * 0.48f, startY + h * 0.02f)
            lineTo(w * 0.44f, startY - h * 0.03f)
            close()
        }
        canvas.drawPath(collarLeft, paint)
        val collarRight = Path().apply {
            moveTo(w * 0.60f, startY - h * 0.01f)
            lineTo(w * 0.55f, startY + h * 0.06f)
            lineTo(w * 0.52f, startY + h * 0.02f)
            lineTo(w * 0.56f, startY - h * 0.03f)
            close()
        }
        canvas.drawPath(collarRight, paint)

        // Nút áo
        paint.color = if (outfit.primaryColor == Color.WHITE)
            Color.argb(40, 0, 0, 0) else Color.argb(60, 255, 255, 255)
        val buttonY = startY + h * 0.06f
        for (i in 0..2) {
            canvas.drawCircle(w * 0.50f, buttonY + i * h * 0.04f, w * 0.006f, paint)
        }
    }

    private fun drawBlouse(canvas: Canvas, w: Int, h: Int, outfit: OutfitOption) {
        val startY = h * 0.72f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Thân áo nữ — cổ tròn
        paint.color = outfit.primaryColor
        val blousePath = Path().apply {
            moveTo(0f, startY + h * 0.06f)
            lineTo(w * 0.28f, startY)
            // Cổ tròn
            cubicTo(
                w * 0.35f, startY - h * 0.04f,
                w * 0.45f, startY - h * 0.05f,
                w * 0.50f, startY - h * 0.04f
            )
            cubicTo(
                w * 0.55f, startY - h * 0.05f,
                w * 0.65f, startY - h * 0.04f,
                w * 0.72f, startY
            )
            lineTo(w.toFloat(), startY + h * 0.06f)
            lineTo(w.toFloat(), h.toFloat())
            lineTo(0f, h.toFloat())
            close()
        }
        canvas.drawPath(blousePath, paint)

        // Viền cổ
        paint.color = outfit.secondaryColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = w * 0.005f
        val neckPath = Path().apply {
            moveTo(w * 0.35f, startY - h * 0.01f)
            cubicTo(
                w * 0.40f, startY - h * 0.04f,
                w * 0.45f, startY - h * 0.05f,
                w * 0.50f, startY - h * 0.04f
            )
            cubicTo(
                w * 0.55f, startY - h * 0.05f,
                w * 0.60f, startY - h * 0.04f,
                w * 0.65f, startY - h * 0.01f
            )
        }
        canvas.drawPath(neckPath, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawAoDai(canvas: Canvas, w: Int, h: Int, outfit: OutfitOption) {
        val startY = h * 0.70f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Thân áo dài — cổ cao mandarin
        paint.color = outfit.primaryColor
        val aoPath = Path().apply {
            moveTo(0f, startY + h * 0.06f)
            lineTo(w * 0.25f, startY)
            lineTo(w * 0.40f, startY - h * 0.02f)
            // Cổ cao
            lineTo(w * 0.42f, startY - h * 0.06f)
            lineTo(w * 0.50f, startY - h * 0.07f)
            lineTo(w * 0.58f, startY - h * 0.06f)
            lineTo(w * 0.60f, startY - h * 0.02f)
            lineTo(w * 0.75f, startY)
            lineTo(w.toFloat(), startY + h * 0.06f)
            lineTo(w.toFloat(), h.toFloat())
            lineTo(0f, h.toFloat())
            close()
        }
        canvas.drawPath(aoPath, paint)

        // Viền cổ áo dài vàng
        paint.color = outfit.secondaryColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = w * 0.004f
        val collarPath = Path().apply {
            moveTo(w * 0.42f, startY - h * 0.06f)
            lineTo(w * 0.50f, startY - h * 0.07f)
            lineTo(w * 0.58f, startY - h * 0.06f)
        }
        canvas.drawPath(collarPath, paint)
        paint.style = Paint.Style.FILL

        // Đường khuy áo dài (giữa)
        paint.color = outfit.secondaryColor
        paint.strokeWidth = w * 0.003f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(w * 0.50f, startY - h * 0.06f, w * 0.50f, h.toFloat(), paint)
        paint.style = Paint.Style.FILL

        // Nút áo nhỏ
        for (i in 0..3) {
            val ny = startY - h * 0.04f + i * h * 0.04f
            canvas.drawCircle(w * 0.50f, ny, w * 0.005f, paint)
        }
    }
}
