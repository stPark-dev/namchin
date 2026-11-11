package dev.stpark.namchin.review

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import dev.stpark.namchin.core.color.ColorLut
import dev.stpark.namchin.core.color.ColorStats
import dev.stpark.namchin.core.color.ColorTransfer
import dev.stpark.namchin.data.Reference
import dev.stpark.namchin.image.BitmapLoader
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope

/** 레퍼런스 색감·필터: 변환표 만들기, 미리보기, 원본 해상도로 덮어쓰기. 느리므로 백그라운드에서 부른다. */
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

    /** 덮어쓰다 실패했고 원본을 되돌리지도 못했다 (사진이 깨졌을 수 있다) */
    class OriginalLostException(cause: Throwable) : IOException("원본을 되돌리지 못함", cause)

    /**
     * 원본 해상도 그대로 색감을 입혀 원본 자리에 덮어쓴다 (필터 입힌 사진만 남긴다).
     * 찍은 날짜·기종·위치 정보는 옮겨 붙인다.
     * 쓰다가 실패하면 미리 읽어 둔 원본을 되돌려 쓴다. 되돌리지도 못하면 [OriginalLostException].
     */
    suspend fun overwriteColored(context: Context, shot: Uri, lut: ColorLut, strength: Float) {
        val resolver = context.contentResolver
        val original = resolver.openInputStream(shot)?.use { it.readBytes() } ?: throw IOException("원본을 읽지 못함: $shot")
        val full = ImageDecoder.decodeBitmap(ImageDecoder.createSource(original)) { decoder, _, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired = true
        }
        val jpeg = try {
            val started = SystemClock.elapsedRealtime()
            applyInPlace(full, lut, strength)
            Log.i(TAG, "색감 적용 ${full.width}x${full.height}: ${SystemClock.elapsedRealtime() - started}ms")
            // 크기를 미리 잡아 두어 버퍼가 커지며 복사되는 일을 줄인다
            ByteArrayOutputStream(original.size + original.size / 2).also {
                if (!full.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it)) throw IOException("JPEG 압축 실패")
            }.toByteArray()
        } finally {
            full.recycle()
        }
        // 디코더가 회전을 이미 반영했으므로 회전 정보는 "똑바로"로
        val withInfo = ExifCarry.carry(original, jpeg, context.cacheDir)
        try {
            write(resolver, shot, withInfo)
        } catch (e: Exception) {
            try {
                write(resolver, shot, original)
            } catch (restore: Exception) {
                e.addSuppressed(restore)
                throw OriginalLostException(e)
            }
            throw e
        }
        // 갤러리 목록의 회전 값도 맞춘다 (원본은 90도로 기록돼 있어 그대로 두면 두 번 돌아간다)
        runCatching {
            resolver.update(shot, ContentValues().apply { put(MediaStore.MediaColumns.ORIENTATION, 0) }, null, null)
        }.onFailure { Log.w(TAG, "회전 값 갱신 실패: $shot", it) }
    }

    private fun write(resolver: ContentResolver, shot: Uri, bytes: ByteArray) {
        resolver.openOutputStream(shot, "wt")?.use { it.write(bytes) } ?: throw IOException("사진을 쓰지 못함: $shot")
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
