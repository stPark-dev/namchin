package dev.stpark.namchin.core.model

/** 이미지 정규화 좌표 (0..1, y는 아래 방향). */
data class Point(val x: Float, val y: Float)

/** 인물 기준 좌/우 (ML Kit 규약과 동일: LEFT = 인물의 왼쪽). */
enum class Joint {
    NOSE,
    LEFT_SHOULDER, RIGHT_SHOULDER,
    LEFT_ELBOW, RIGHT_ELBOW,
    LEFT_WRIST, RIGHT_WRIST,
    LEFT_HIP, RIGHT_HIP,
    LEFT_KNEE, RIGHT_KNEE,
    LEFT_ANKLE, RIGHT_ANKLE;

    fun opposite(): Joint = when {
        name.startsWith("LEFT_") -> valueOf("RIGHT_" + name.removePrefix("LEFT_"))
        name.startsWith("RIGHT_") -> valueOf("LEFT_" + name.removePrefix("RIGHT_"))
        else -> this
    }
}

data class Keypoint(val joint: Joint, val position: Point, val confidence: Float)

/**
 * @property aspectRatio 원본 이미지의 가로/세로 비율. 정규화 좌표를 실제 비율로 되돌릴 때 쓴다.
 */
data class PersonPose(val keypoints: Map<Joint, Keypoint>, val aspectRatio: Float) {
    init {
        require(aspectRatio > 0f) { "aspectRatio must be positive: $aspectRatio" }
    }

    fun reliable(joint: Joint, minConfidence: Float): Point? =
        keypoints[joint]?.takeIf { it.confidence >= minConfidence }?.position
}

/** 포즈 비교 단위가 되는 신체 마디 (from → to 방향). */
enum class BodyPart(val from: Joint, val to: Joint) {
    LEFT_UPPER_ARM(Joint.LEFT_SHOULDER, Joint.LEFT_ELBOW),
    LEFT_FOREARM(Joint.LEFT_ELBOW, Joint.LEFT_WRIST),
    RIGHT_UPPER_ARM(Joint.RIGHT_SHOULDER, Joint.RIGHT_ELBOW),
    RIGHT_FOREARM(Joint.RIGHT_ELBOW, Joint.RIGHT_WRIST),
    LEFT_TORSO(Joint.LEFT_SHOULDER, Joint.LEFT_HIP),
    RIGHT_TORSO(Joint.RIGHT_SHOULDER, Joint.RIGHT_HIP),
    LEFT_THIGH(Joint.LEFT_HIP, Joint.LEFT_KNEE),
    LEFT_SHIN(Joint.LEFT_KNEE, Joint.LEFT_ANKLE),
    RIGHT_THIGH(Joint.RIGHT_HIP, Joint.RIGHT_KNEE),
    RIGHT_SHIN(Joint.RIGHT_KNEE, Joint.RIGHT_ANKLE),
}
