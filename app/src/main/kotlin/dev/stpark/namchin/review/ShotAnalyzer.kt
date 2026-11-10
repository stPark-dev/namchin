package dev.stpark.namchin.review

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import dev.stpark.namchin.core.bestshot.Sharpness
import dev.stpark.namchin.core.bestshot.ShotCandidate
import dev.stpark.namchin.core.model.PersonPose
import dev.stpark.namchin.core.pose.PoseSimilarity
import dev.stpark.namchin.image.BitmapLoader
import dev.stpark.namchin.ml.ReferencePoseAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/** 찍힌 사진 한 장 → 베스트 컷 점수 재료 (포즈 일치도, 눈 뜸, 선명도). */
class ShotAnalyzer(context: Context) : AutoCloseable {
    private val appContext = context.applicationContext
    private val pose = ReferencePoseAnalyzer()
    private val face = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build(),
    )

    /** @param reference 레퍼런스 포즈. 없으면(기본 모드) 포즈 점수는 null */
    suspend fun analyze(uri: Uri, reference: PersonPose?): ShotCandidate {
        val bitmap = withContext(Dispatchers.IO) { BitmapLoader.load(appContext, uri, ANALYSIS_DIMENSION) }
        val sharpness = withContext(Dispatchers.Default) {
            Sharpness.laplacianVariance(BitmapLoader.toLuma(bitmap, SHARPNESS_WIDTH)).toFloat()
        }
        val poseScore = reference?.let { ref -> pose.analyze(bitmap)?.let { PoseSimilarity.compare(ref, it)?.score } }
        // 가장 큰 얼굴의 두 눈 중 더 감긴 쪽
        val eyesOpen = face.process(InputImage.fromBitmap(bitmap, 0)).await()
            .maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
            ?.let { f ->
                val left = f.leftEyeOpenProbability
                val right = f.rightEyeOpenProbability
                if (left != null && right != null) minOf(left, right) else left ?: right
            }
        return ShotCandidate(uri.toString(), poseScore, eyesOpen, sharpness)
    }

    override fun close() {
        pose.close()
        face.close()
    }

    private companion object {
        const val ANALYSIS_DIMENSION = 1600
        const val SHARPNESS_WIDTH = 540
    }
}
