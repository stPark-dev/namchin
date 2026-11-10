package dev.stpark.namchin.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.net.Uri
import dev.stpark.namchin.core.crop.LumaImage
import dev.stpark.namchin.importer.PixelRect
import kotlin.math.roundToInt

object BitmapLoader {
    /** ML Kit 정확도와 메모리 사이 절충. 긴 변 기준. */
    const val MAX_DIMENSION = 2048
    const val LUMA_GRID_WIDTH = 180

    /** EXIF 회전이 적용된 소프트웨어 비트맵 (픽셀 접근 가능). */
    fun load(context: Context, uri: Uri): Bitmap {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val longest = maxOf(info.size.width, info.size.height)
            if (longest > MAX_DIMENSION) {
                val scale = MAX_DIMENSION.toFloat() / longest
                decoder.setTargetSize(
                    (info.size.width * scale).roundToInt(),
                    (info.size.height * scale).roundToInt(),
                )
            }
        }
    }

    /** 자동 크롭 검출용 축소 흑백 이미지 (원본 비율 유지). */
    fun toLuma(bitmap: Bitmap): LumaImage {
        val width = LUMA_GRID_WIDTH
        val height = maxOf(1, (bitmap.height * width / bitmap.width.toFloat()).roundToInt())
        val small = Bitmap.createScaledBitmap(bitmap, width, height, true)
        val pixels = IntArray(width * height)
        small.getPixels(pixels, 0, width, 0, 0, width, height)
        if (small !== bitmap) small.recycle()
        val luma = IntArray(pixels.size) {
            val c = pixels[it]
            (0.299 * Color.red(c) + 0.587 * Color.green(c) + 0.114 * Color.blue(c)).roundToInt()
        }
        return LumaImage(width, height, luma)
    }

    fun crop(bitmap: Bitmap, rect: PixelRect): Bitmap =
        Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width, rect.height)
}
