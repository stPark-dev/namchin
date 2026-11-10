package dev.stpark.namchin.ml

import com.google.mlkit.vision.pose.PoseLandmark
import dev.stpark.namchin.core.model.Joint
import dev.stpark.namchin.core.model.Keypoint
import dev.stpark.namchin.core.model.PersonPose
import dev.stpark.namchin.core.model.Point

/** ML Kit 랜드마크에서 필요한 값만 뽑은 것 (픽셀 좌표). */
data class RawLandmark(val type: Int, val x: Float, val y: Float, val likelihood: Float)

/** ML Kit 결과 → core PersonPose (이미지 크기로 정규화). */
object MlKitPoseMapper {

    val JOINT_BY_LANDMARK: Map<Int, Joint> = mapOf(
        PoseLandmark.NOSE to Joint.NOSE,
        PoseLandmark.LEFT_EAR to Joint.LEFT_EAR,
        PoseLandmark.RIGHT_EAR to Joint.RIGHT_EAR,
        PoseLandmark.LEFT_SHOULDER to Joint.LEFT_SHOULDER,
        PoseLandmark.RIGHT_SHOULDER to Joint.RIGHT_SHOULDER,
        PoseLandmark.LEFT_ELBOW to Joint.LEFT_ELBOW,
        PoseLandmark.RIGHT_ELBOW to Joint.RIGHT_ELBOW,
        PoseLandmark.LEFT_WRIST to Joint.LEFT_WRIST,
        PoseLandmark.RIGHT_WRIST to Joint.RIGHT_WRIST,
        PoseLandmark.LEFT_HIP to Joint.LEFT_HIP,
        PoseLandmark.RIGHT_HIP to Joint.RIGHT_HIP,
        PoseLandmark.LEFT_KNEE to Joint.LEFT_KNEE,
        PoseLandmark.RIGHT_KNEE to Joint.RIGHT_KNEE,
        PoseLandmark.LEFT_ANKLE to Joint.LEFT_ANKLE,
        PoseLandmark.RIGHT_ANKLE to Joint.RIGHT_ANKLE,
    )

    /** 랜드마크가 하나도 없으면(사람 미검출) null. */
    fun toPersonPose(landmarks: List<RawLandmark>, width: Int, height: Int): PersonPose? {
        if (landmarks.isEmpty()) return null
        val keypoints = landmarks.mapNotNull { lm ->
            val joint = JOINT_BY_LANDMARK[lm.type] ?: return@mapNotNull null
            joint to Keypoint(joint, Point(lm.x / width, lm.y / height), lm.likelihood)
        }.toMap()
        return PersonPose(keypoints, width.toFloat() / height)
    }
}
