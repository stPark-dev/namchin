package dev.stpark.namchin.capture

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat

/** ImageCapture → 갤러리 `DCIM/남친렌즈`. 결과 콜백은 메인 스레드. */
object PhotoSaver {
    private const val TAG = "PhotoSaver"

    fun take(context: Context, capture: ImageCapture, onResult: (Uri?) -> Unit) {
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
