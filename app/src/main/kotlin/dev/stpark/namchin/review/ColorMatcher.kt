package dev.stpark.namchin.review

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import dev.stpark.namchin.capture.CaptureNaming
import dev.stpark.namchin.core.color.ColorLut
import dev.stpark.namchin.core.color.ColorStats
import dev.stpark.namchin.core.color.ColorTransfer
import dev.stpark.namchin.data.Reference
import dev.stpark.namchin.image.BitmapLoader
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope

/** 레퍼런스 색감 따라하기: 변환표 만들기, 미리보기, 원본 해상도로 새 파일 저장. 느리므로 백그라운드에서 부른다. */
object ColorMatcher {
    /** 색 통계용으로 줄여 읽는 크기 (통계엔 충분) */
    private const val STATS_DIMENSION = 512
    private const val JPEG_QUALITY = 95
    private const val TAG = "ColorMatcher"
    private const val MAX_WORKERS = 8

    /** @param keepLightness true면 밝기는 원본 그대로, 색만 레퍼런스를 따른다 */
    fun lut(context: Context, reference: Reference, shot: Uri, keepLightness: Boolean): ColorLut {
        val refBitmap = BitmapFactory.decodeFile(reference.imagePath, BitmapFactory.Options().apply { inSampleSize = 4 })
            ?: throw IOException("레퍼런스 이미지를 읽지 못함: ${reference.imagePath}")
        val shotBitmap = BitmapLoader.load(context, shot, STATS_DIMENSION)
        val transfer = ColorTransfer(
            ColorStats.of(pixels(shotBitmap)),
            ColorStats.of(pixels(refBitmap)),
            keepLightness = keepLightness,
        )
        return ColorLut.build(transfer::apply)
    }

    /** base 위에 색감을 입힌 새 비트맵 (base는 그대로). */
    suspend fun applied(base: Bitmap, lut: ColorLut, strength: Float): Bitmap {
        val out = base.copy(Bitmap.Config.ARGB_8888, true)
        applyInPlace(out, lut, strength)
        return out
    }

    /**
     * 원본 해상도 그대로 색감을 입혀 갤러리에 새 파일로 저장한다 (원본은 그대로 둔다).
     * @return 저장된 사진 주소
     */
    suspend fun saveColored(context: Context, shot: Uri, lut: ColorLut, strength: Float): Uri {
        val full = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, shot)) { decoder, _, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired = true
        }
        val started = SystemClock.elapsedRealtime()
        applyInPlace(full, lut, strength)
        Log.i(TAG, "색감 적용 ${full.width}x${full.height}: ${SystemClock.elapsedRealtime() - started}ms")
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, CaptureNaming.displayName(System.currentTimeMillis()) + "_color")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, CaptureNaming.RELATIVE_PATH)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("갤러리에 항목을 만들지 못함")
        try {
            val written = resolver.openOutputStream(uri)?.use { full.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
            if (written != true) throw IOException("JPEG 저장 실패")
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            return uri
        } catch (e: Exception) {
            // 반쯤 쓴 항목이 갤러리에 남지 않게
            resolver.delete(uri, null, null)
            throw e
        } finally {
            full.recycle()
        }
    }

    /**
     * 픽셀을 한 번에 읽어 CPU 코어 수만큼 나눠 동시에 바꾼 뒤 한 번에 쓴다.
     * runBlocking을 쓰지 않아 백그라운드 스레드를 붙잡지 않는다 (미리보기가 겹쳐도 스레드가 바닥나지 않게).
     */
    private suspend fun applyInPlace(bitmap: Bitmap, lut: ColorLut, strength: Float) {
        val px = pixels(bitmap)
        val workers = Runtime.getRuntime().availableProcessors().coerceIn(1, MAX_WORKERS)
        val chunk = (px.size + workers - 1) / workers
        coroutineScope {
            (0 until workers).map { w ->
                launch(Dispatchers.Default) { lut.applyInPlace(px, strength, w * chunk, minOf(px.size, (w + 1) * chunk)) }
            }.joinAll()
        }
        bitmap.setPixels(px, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    }

    private fun pixels(bitmap: Bitmap): IntArray {
        val px = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(px, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return px
    }
}
