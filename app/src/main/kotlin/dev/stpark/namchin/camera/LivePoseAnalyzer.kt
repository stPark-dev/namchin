package dev.stpark.namchin.camera

import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import dev.stpark.namchin.ml.RawLandmark

/**
 * @property width, height 회전이 적용된(세로) 분석 이미지 크기
 * @property analysisMs 이 프레임의 ML Kit 처리 시간
 */
class PoseFrame(
    val landmarks: List<RawLandmark>,
    val width: Int,
    val height: Int,
    val analysisMs: Long,
)

/** CameraX 프레임 → ML Kit 스트림 포즈 분석. 결과 콜백은 메인 스레드. */
class LivePoseAnalyzer(private val onFrame: (PoseFrame) -> Unit) : ImageAnalysis.Analyzer, AutoCloseable {
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
        detector.process(InputImage.fromMediaImage(media, rotation))
            .addOnSuccessListener { pose ->
                val landmarks = pose.allPoseLandmarks.map {
                    RawLandmark(it.landmarkType, it.position.x, it.position.y, it.inFrameLikelihood)
                }
                onFrame(PoseFrame(landmarks, width, height, SystemClock.elapsedRealtime() - start))
            }
            .addOnCompleteListener { proxy.close() }
    }

    override fun close() = detector.close()
}
