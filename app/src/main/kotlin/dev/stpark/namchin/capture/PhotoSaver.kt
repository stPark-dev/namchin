package dev.stpark.namchin.capture

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.media.MediaActionSound
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat

/** ImageCapture → 갤러리 `DCIM/남친렌즈`. 결과 콜백은 메인 스레드. */
object PhotoSaver {
    private const val TAG = "PhotoSaver"
    /** 셔터음은 촬영마다 쓰므로 앱이 살아 있는 동안 하나를 계속 둔다 (SoundPool 하나, 작음). */
    private val sound by lazy { MediaActionSound().apply { load(MediaActionSound.SHUTTER_CLICK) } }

    /**
     * count장을 차례로 찍는다. playSound면 각 장마다 셔터음.
     * 실패하거나 isActive()가 false가 되면(화면 이탈) 멈춘다. 결과는 저장된 장 수.
     */
    fun takeBurst(
        context: Context,
        capture: ImageCapture,
        count: Int,
        playSound: Boolean,
        isActive: () -> Boolean,
        onDone: (saved: Int) -> Unit,
    ) = Burst.run(count, isActive, { done -> take(context, capture, playSound) { uri -> done(uri != null) } }, onDone)

    /** @param playSound 설정 화면의 셔터음 켬/끔 */
    fun take(context: Context, capture: ImageCapture, playSound: Boolean, onResult: (Uri?) -> Unit) {
        if (playSound) sound.play(MediaActionSound.SHUTTER_CLICK)
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, CaptureNaming.displayName(System.currentTimeMillis()))
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, CaptureNaming.RELATIVE_PATH)
        }
        val options = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values,
        ).build()
        capture.takePicture(
            options,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) = onResult(output.savedUri)

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "촬영 저장 실패", exception)
                    onResult(null)
                }
            },
        )
    }
}
