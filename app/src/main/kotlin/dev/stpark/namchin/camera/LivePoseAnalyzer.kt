package dev.stpark.namchin.camera

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import android.util.Log
import dev.stpark.namchin.ml.RawLandmark
import java.util.concurrent.Executor

/**
 * @property width, height 회전이 적용된(세로) 분석 이미지 크기
 * @property analysisMs 이 프레임의 ML Kit 처리 시간
 * @property brightness 프레임 평균 밝기 (Y 평면, 0..255). 야간 판정용
 */
class PoseFrame(
    val landmarks: List<RawLandmark>,
    val width: Int,
    val height: Int,
    val analysisMs: Long,
    val brightness: Float,
)

/**
 * CameraX 프레임 → ML Kit 스트림 포즈 분석. 결과 콜백은 메인 스레드.
 * 프레임 해제는 분석 스레드에서 해서 메인 스레드가 바빠도 다음 분석이 밀리지 않게 한다.
 */
class LivePoseAnalyzer(
    private val analysisExecutor: Executor,
    private val onFrame: (PoseFrame) -> Unit,
    /** SCENE_INTERVAL_MS마다 세로로 돌린 작은 프레임 (분석 스레드에서 호출) */
    private val onScene: (Bitmap) -> Unit = {},
) : ImageAnalysis.Analyzer, AutoCloseable {
    private var lastSceneAt = 0L
    private val closeExecutor = FallbackExecutor(analysisExecutor)
    private val detector = PoseDetection.getClient(
        AccuratePoseDetectorOptions.Builder()
            .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
            .build(),
    )

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(proxy: ImageProxy) {
        val media = proxy.image ?: return proxy.close()
        val rotation = proxy.imageInfo.rotationDegrees
        val upright = rotation == 90 || rotation == 270
        val width = if (upright) proxy.height else proxy.width
        val height = if (upright) proxy.width else proxy.height
        val start = SystemClock.elapsedRealtime()
        val brightness = meanLuma(proxy)
        if (start - lastSceneAt >= SCENE_INTERVAL_MS) {
            lastSceneAt = start
            runCatching { onScene(sceneBitmap(proxy, rotation)) }.onFailure { Log.w(TAG, "장면 프레임 실패", it) }
        }
        val task = try {
            detector.process(InputImage.fromMediaImage(media, rotation))
        } catch (e: Exception) {
            Log.w(TAG, "포즈 분석 시작 실패", e)
            return proxy.close()
        }
        task
            .addOnSuccessListener { pose ->
                val landmarks = pose.allPoseLandmarks.map {
                    RawLandmark(it.landmarkType, it.position.x, it.position.y, it.inFrameLikelihood)
                }
                onFrame(PoseFrame(landmarks, width, height, SystemClock.elapsedRealtime() - start, brightness))
            }
            .addOnCompleteListener(closeExecutor) { proxy.close() }
    }

    override fun close() = detector.close()

    /** Y(밝기) 평면을 듬성듬성 훑어 평균. 프레임마다 부담 없게 LUMA_STEP 간격으로. */
    private fun meanLuma(proxy: ImageProxy): Float {
        val plane = proxy.planes[0]
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        var sum = 0L
        var count = 0
        for (y in 0 until proxy.height step LUMA_STEP) {
            for (x in 0 until proxy.width step LUMA_STEP) {
                sum += buffer.get(y * rowStride + x * plane.pixelStride).toInt() and 0xFF
                count++
            }
        }
        return if (count == 0) 0f else sum.toFloat() / count
    }

    /** 프레임을 세로로 돌리고 긴 변 SCENE_SIZE로 줄인다. */
    private fun sceneBitmap(proxy: ImageProxy, rotation: Int): Bitmap {
        val full = proxy.toBitmap()
        val scale = SCENE_SIZE.toFloat() / maxOf(full.width, full.height)
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(rotation.toFloat())
        }
        return Bitmap.createBitmap(full, 0, 0, full.width, full.height, matrix, true).also { full.recycle() }
    }

    private companion object {
        const val TAG = "LivePoseAnalyzer"
        const val SCENE_INTERVAL_MS = 2_000L
        const val SCENE_SIZE = 256
        const val LUMA_STEP = 16
    }
}
