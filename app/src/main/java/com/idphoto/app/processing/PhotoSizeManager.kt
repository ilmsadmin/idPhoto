package com.idphoto.app.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF

/**
 * Cấu hình kích thước ảnh thẻ chuẩn.
 */
data class PhotoSize(
    val name: String,
    val widthMm: Float,   // Chiều rộng (mm)
    val heightMm: Float,  // Chiều cao (mm)
    val description: String,
    val widthPx: Int,     // Pixel ở 300 DPI
    val heightPx: Int,    // Pixel ở 300 DPI
    val country: String = "VN",  // Country code for tab filtering
) {
    val aspectRatio: Float get() = widthMm / heightMm
    val displaySize: String get() = "${widthMm.toInt()}x${heightMm.toInt()}mm"
    val pixelSize: String get() = "${widthPx}×${heightPx} px"
}

/**
 * Quản lý kích thước ảnh thẻ chuẩn và tạo print layout.
 */
object PhotoSizeManager {

    val standardSizes = listOf(
        // Việt Nam
        PhotoSize("2x3 cm", 20f, 30f, "Visa, CMND cũ", 236, 354, "VN"),
        PhotoSize("3x4 cm", 30f, 40f, "CCCD, Hộ chiếu VN", 354, 472, "VN"),
        PhotoSize("4x6 cm", 40f, 60f, "Sơ yếu lý lịch, Hồ sơ", 472, 709, "VN"),
        PhotoSize("2x3 cm", 20f, 30f, "Thẻ sinh viên, Nhân viên", 236, 354, "VN"),
        PhotoSize("3x4 cm", 30f, 40f, "Bằng lái xe (GPLX)", 354, 472, "VN"),

        // US
        PhotoSize("2x2 inch", 51f, 51f, "US Passport", 600, 600, "US"),
        PhotoSize("5x5 cm", 50f, 50f, "US Visa", 591, 591, "US"),
        PhotoSize("2x2 inch", 51f, 51f, "Green Card", 600, 600, "US"),

        // EU / Schengen
        PhotoSize("3.5x4.5 cm", 35f, 45f, "Schengen Visa", 413, 531, "EU"),
        PhotoSize("3.5x4.5 cm", 35f, 45f, "EU Passport", 413, 531, "EU"),
        PhotoSize("3.5x4.5 cm", 35f, 45f, "UK Passport", 413, 531, "EU"),

        // Japan
        PhotoSize("3x4 cm", 30f, 40f, "Japan Passport", 354, 472, "JP"),
        PhotoSize("4.5x4.5 cm", 45f, 45f, "Japan Visa", 531, 531, "JP"),
        PhotoSize("2.4x3 cm", 24f, 30f, "Zairyu Card", 283, 354, "JP"),

        // Korea
        PhotoSize("3.5x4.5 cm", 35f, 45f, "Korea Passport", 413, 531, "KR"),
        PhotoSize("3x4 cm", 30f, 40f, "Korea ID Card", 354, 472, "KR"),
        PhotoSize("3.5x4.5 cm", 35f, 45f, "Korea Visa", 413, 531, "KR"),

        // China
        PhotoSize("3.3x4.8 cm", 33f, 48f, "China Visa", 390, 567, "CN"),
        PhotoSize("2.5x3.5 cm", 25f, 35f, "China ID Card", 295, 413, "CN"),
        PhotoSize("3.3x4.8 cm", 33f, 48f, "China Passport", 390, 567, "CN"),

        // Thailand
        PhotoSize("3.5x4.5 cm", 35f, 45f, "Thailand Visa", 413, 531, "TH"),
        PhotoSize("2.5x3.2 cm", 25f, 32f, "Thai ID Card", 295, 378, "TH"),

        // Australia
        PhotoSize("3.5x4.5 cm", 35f, 45f, "Australia Visa", 413, 531, "AU"),
        PhotoSize("3.5x4.5 cm", 35f, 45f, "Australia Passport", 413, 531, "AU"),
    )

    val countries = listOf(
        CountryTab("VN", "Việt Nam"),
        CountryTab("US", "Mỹ / US"),
        CountryTab("EU", "Châu Âu"),
        CountryTab("JP", "Nhật Bản"),
        CountryTab("KR", "Hàn Quốc"),
        CountryTab("CN", "Trung Quốc"),
        CountryTab("TH", "Thái Lan"),
        CountryTab("AU", "Úc"),
    )

    data class CountryTab(val code: String, val name: String)

    /** Popular sizes for home screen quick access */
    val popularSizes = listOf(
        standardSizes[0],  // 2x3 VN
        standardSizes[1],  // 3x4 VN
        standardSizes[2],  // 4x6 VN
        standardSizes[8],  // 3.5x4.5 Schengen
        standardSizes[5],  // 2x2 inch US Passport
        standardSizes[11], // 3x4 Japan Passport
    )

    fun getSizesByCountry(country: String): List<PhotoSize> {
        return standardSizes.filter { it.country == country }
    }

    fun searchSizes(query: String): List<PhotoSize> {
        val q = query.lowercase()
        return standardSizes.filter {
            it.name.lowercase().contains(q) ||
            it.description.lowercase().contains(q) ||
            it.country.lowercase().contains(q)
        }
    }

    /**
     * Crop ảnh theo tỷ lệ kích thước, center-crop giữ phần trung tâm.
     */
    fun cropToSize(bitmap: Bitmap, size: PhotoSize): Bitmap {
        val targetRatio = size.widthMm / size.heightMm
        val srcW = bitmap.width
        val srcH = bitmap.height
        val srcRatio = srcW.toFloat() / srcH.toFloat()

        val cropW: Int
        val cropH: Int

        if (srcRatio > targetRatio) {
            // Ảnh rộng hơn → crop width
            cropH = srcH
            cropW = (srcH * targetRatio).toInt()
        } else {
            // Ảnh cao hơn → crop height
            cropW = srcW
            cropH = (srcW / targetRatio).toInt()
        }

        val x = (srcW - cropW) / 2
        val y = (srcH - cropH) / 6 // Offset lên trên 1 chút cho ảnh thẻ (đầu ở trên)

        val safeX = x.coerceIn(0, srcW - cropW)
        val safeY = y.coerceIn(0, srcH - cropH)

        val cropped = Bitmap.createBitmap(bitmap, safeX, safeY, cropW, cropH)

        // Scale về kích thước chuẩn 300 DPI
        return Bitmap.createScaledBitmap(cropped, size.widthPx, size.heightPx, true)
    }

    /**
     * Tạo print layout — sắp xếp ảnh trên giấy 4x6 inch (10x15 cm) để in.
     * Tự động tính số ảnh vừa trên trang.
     */
    fun createPrintLayout(
        photo: Bitmap,
        size: PhotoSize,
        paperWidthMm: Float = 152f,  // 6 inches
        paperHeightMm: Float = 102f, // 4 inches
    ): Bitmap {
        val dpi = 300
        val paperWidthPx = (paperWidthMm / 25.4f * dpi).toInt()
        val paperHeightPx = (paperHeightMm / 25.4f * dpi).toInt()

        // Tính số ảnh trên mỗi hàng/cột
        val gapMm = 2f // khoảng cách giữa các ảnh
        val gapPx = (gapMm / 25.4f * dpi).toInt()
        val marginMm = 3f
        val marginPx = (marginMm / 25.4f * dpi).toInt()

        val availableW = paperWidthPx - 2 * marginPx
        val availableH = paperHeightPx - 2 * marginPx

        val cols = ((availableW + gapPx) / (size.widthPx + gapPx)).coerceAtLeast(1)
        val rows = ((availableH + gapPx) / (size.heightPx + gapPx)).coerceAtLeast(1)

        // Tạo bitmap giấy trắng
        val paperBitmap = Bitmap.createBitmap(paperWidthPx, paperHeightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(paperBitmap)
        canvas.drawColor(Color.WHITE)

        // Vẽ đường viền nhạt
        val borderPaint = Paint().apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        // Scale photo nếu cần
        val scaledPhoto = if (photo.width == size.widthPx && photo.height == size.heightPx) {
            photo
        } else {
            Bitmap.createScaledBitmap(photo, size.widthPx, size.heightPx, true)
        }

        // Center layout trên giấy
        val totalW = cols * size.widthPx + (cols - 1) * gapPx
        val totalH = rows * size.heightPx + (rows - 1) * gapPx
        val startX = (paperWidthPx - totalW) / 2
        val startY = (paperHeightPx - totalH) / 2

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val x = startX + col * (size.widthPx + gapPx)
                val y = startY + row * (size.heightPx + gapPx)

                canvas.drawBitmap(scaledPhoto, x.toFloat(), y.toFloat(), null)

                // Vẽ đường cắt nhẹ
                canvas.drawRect(
                    RectF(x.toFloat(), y.toFloat(),
                        (x + size.widthPx).toFloat(), (y + size.heightPx).toFloat()),
                    borderPaint
                )
            }
        }

        // Thêm text thông tin
        val textPaint = Paint().apply {
            color = Color.GRAY
            textSize = 24f
            isAntiAlias = true
        }
        val infoText = "${size.name} | ${cols}x${rows} = ${cols * rows} ảnh | 300 DPI"
        canvas.drawText(infoText, marginPx.toFloat(), (paperHeightPx - 8).toFloat(), textPaint)

        return paperBitmap
    }

    /**
     * Tính số ảnh trên 1 trang giấy 4x6.
     */
    fun getPhotosPerSheet(size: PhotoSize): Int {
        val paperW = 152f
        val paperH = 102f
        val gap = 2f
        val margin = 3f
        val availW = paperW - 2 * margin
        val availH = paperH - 2 * margin
        val cols = ((availW + gap) / (size.widthMm + gap)).toInt().coerceAtLeast(1)
        val rows = ((availH + gap) / (size.heightMm + gap)).toInt().coerceAtLeast(1)
        return cols * rows
    }

    /**
     * Tạo print layout với số lượng ảnh tuỳ chọn.
     * Nếu số ảnh vượt quá 1 trang, chỉ tạo số trang cần thiết (trả về trang đầu tiên).
     * Ảnh sẽ được xếp từ trái sang phải, trên xuống dưới, đủ số lượng thì dừng.
     */
    fun createPrintLayoutWithCount(
        photo: Bitmap,
        size: PhotoSize,
        count: Int,
        paperWidthMm: Float = 152f,
        paperHeightMm: Float = 102f,
    ): Bitmap {
        val dpi = 300
        val paperWidthPx = (paperWidthMm / 25.4f * dpi).toInt()
        val paperHeightPx = (paperHeightMm / 25.4f * dpi).toInt()

        val gapMm = 2f
        val gapPx = (gapMm / 25.4f * dpi).toInt()
        val marginMm = 3f
        val marginPx = (marginMm / 25.4f * dpi).toInt()

        val availableW = paperWidthPx - 2 * marginPx
        val availableH = paperHeightPx - 2 * marginPx

        val cols = ((availableW + gapPx) / (size.widthPx + gapPx)).coerceAtLeast(1)
        val rows = ((availableH + gapPx) / (size.heightPx + gapPx)).coerceAtLeast(1)
        val perSheet = cols * rows

        // Tính số trang cần thiết
        val totalSheets = ((count + perSheet - 1) / perSheet).coerceAtLeast(1)
        val totalRows = rows * totalSheets
        val totalHeightPx = if (totalSheets > 1) {
            paperHeightPx * totalSheets
        } else {
            paperHeightPx
        }

        val paperBitmap = Bitmap.createBitmap(paperWidthPx, totalHeightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(paperBitmap)
        canvas.drawColor(Color.WHITE)

        val borderPaint = Paint().apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        val scaledPhoto = if (photo.width == size.widthPx && photo.height == size.heightPx) {
            photo
        } else {
            Bitmap.createScaledBitmap(photo, size.widthPx, size.heightPx, true)
        }

        var placed = 0
        for (sheet in 0 until totalSheets) {
            val sheetOffsetY = sheet * paperHeightPx
            val totalW = cols * size.widthPx + (cols - 1) * gapPx
            val totalH = rows * size.heightPx + (rows - 1) * gapPx
            val startX = (paperWidthPx - totalW) / 2
            val startY = sheetOffsetY + (paperHeightPx - totalH) / 2

            for (row in 0 until rows) {
                for (col in 0 until cols) {
                    if (placed >= count) break
                    val x = startX + col * (size.widthPx + gapPx)
                    val y = startY + row * (size.heightPx + gapPx)

                    canvas.drawBitmap(scaledPhoto, x.toFloat(), y.toFloat(), null)
                    canvas.drawRect(
                        RectF(x.toFloat(), y.toFloat(),
                            (x + size.widthPx).toFloat(), (y + size.heightPx).toFloat()),
                        borderPaint
                    )
                    placed++
                }
                if (placed >= count) break
            }

            // Info text per sheet
            val textPaint = Paint().apply {
                color = Color.GRAY
                textSize = 24f
                isAntiAlias = true
            }
            val photosOnSheet = minOf(perSheet, count - sheet * perSheet)
            val infoText = "${size.name} | $photosOnSheet ảnh | 300 DPI"
            canvas.drawText(infoText, marginPx.toFloat(), (sheetOffsetY + paperHeightPx - 8).toFloat(), textPaint)
        }

        return paperBitmap
    }
}
