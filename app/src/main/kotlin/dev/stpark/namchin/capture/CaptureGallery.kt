package dev.stpark.namchin.capture

import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.util.Size

/** 셔터 옆 갤러리 버튼: 앱이 마지막으로 저장한 사진을 찾고, 갤러리 앱에서 연다. */
object CaptureGallery {
    private const val TAG = "CaptureGallery"

    /** 앱이 저장하는 폴더(`DCIM/남친렌즈/`)의 사진만. 앱이 직접 만든 사진이라 권한 없이 읽힌다 */
    const val SELECTION = "relative_path LIKE ?"
    val SELECTION_ARGS = arrayOf(CaptureNaming.RELATIVE_PATH + "%")
    const val SORT = "date_added DESC"

    /** 가장 최근 사진. 없거나 못 읽으면 null. 디스크를 읽으니 메인 스레드 밖에서 부른다 */
    fun latest(context: Context): Uri? = runCatching {
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.MediaColumns._ID),
            SELECTION,
            SELECTION_ARGS,
            SORT,
        )?.use { cursor ->
            if (cursor.moveToFirst()) ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cursor.getLong(0)) else null
        }
    }.onFailure { Log.w(TAG, "최근 사진 찾기 실패", it) }.getOrNull()

    /** 버튼에 그릴 작은 그림. 메인 스레드 밖에서 부른다 */
    fun thumbnail(context: Context, uri: Uri, sizePx: Int): Bitmap? = runCatching {
        context.contentResolver.loadThumbnail(uri, Size(sizePx, sizePx), null)
    }.onFailure { Log.w(TAG, "썸네일 실패", it) }.getOrNull()

    /** 그 사진을 갤러리 앱에서 연다. 사진이 없으면 갤러리 앱의 사진 목록을 연다 */
    fun open(context: Context, uri: Uri?) {
        val intent = if (uri != null) {
            Intent(Intent.ACTION_VIEW).setDataAndType(uri, "image/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            Intent(Intent.ACTION_VIEW, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "갤러리 앱을 찾지 못함", e)
        }
    }
}
