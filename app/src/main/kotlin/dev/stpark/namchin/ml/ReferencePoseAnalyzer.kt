package dev.stpark.namchin.ml

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import dev.stpark.namchin.core.model.PersonPose
import kotlinx.coroutines.tasks.await

/** 정지 이미지(레퍼런스)용 고정밀 포즈 분석. */
class ReferencePoseAnalyzer : AutoCloseable {
    private val detector = PoseDetection.getClient(
        AccuratePoseDetectorOptions.Builder()
            .setDetectorMode(AccuratePoseDetectorOptions.SINGLE_IMAGE_MODE)
            .build(),
    )

    /** 인물을 찾지 못하면 null. */
    suspend fun analyze(bitmap: Bitmap): PersonPose? {
        val pose = detector.process(InputImage.fromBitmap(bitmap, 0)).await()
        val landmarks = pose.allPoseLandmarks.map {
            RawLandmark(it.landmarkType, it.position.x, it.position.y, it.inFrameLikelihood)
        }
        return MlKitPoseMapper.toPersonPose(landmarks, bitmap.width, bitmap.height)
    }

    override fun close() = detector.close()
}
