package dev.stpark.namchin.core.testutil

import dev.stpark.namchin.core.model.Joint
import dev.stpark.namchin.core.model.Keypoint
import dev.stpark.namchin.core.model.PersonPose
import dev.stpark.namchin.core.model.Point

/** 정면 차렷 자세. 이미지 좌표(0..1, y 아래 방향). 인물의 왼쪽 = 화면 오른쪽. */
val STANDING: Map<Joint, Point> = mapOf(
    Joint.NOSE to Point(0.50f, 0.20f),
    Joint.LEFT_SHOULDER to Point(0.55f, 0.30f),
    Joint.RIGHT_SHOULDER to Point(0.45f, 0.30f),
    Joint.LEFT_ELBOW to Point(0.56f, 0.40f),
    Joint.RIGHT_ELBOW to Point(0.44f, 0.40f),
    Joint.LEFT_WRIST to Point(0.57f, 0.50f),
    Joint.RIGHT_WRIST to Point(0.43f, 0.50f),
    Joint.LEFT_HIP to Point(0.53f, 0.55f),
    Joint.RIGHT_HIP to Point(0.47f, 0.55f),
    Joint.LEFT_KNEE to Point(0.53f, 0.70f),
    Joint.RIGHT_KNEE to Point(0.47f, 0.70f),
    Joint.LEFT_ANKLE to Point(0.53f, 0.85f),
    Joint.RIGHT_ANKLE to Point(0.47f, 0.85f),
)

fun pose(points: Map<Joint, Point>, confidence: Float = 0.9f, aspectRatio: Float = 1f): PersonPose =
    PersonPose(points.mapValues { (joint, p) -> Keypoint(joint, p, confidence) }, aspectRatio)

fun Map<Joint, Point>.with(vararg overrides: Pair<Joint, Point>): Map<Joint, Point> =
    this + overrides

/** 전체를 scale 배 키우고 (dx, dy) 이동 — 거리/위치만 다른 같은 포즈. */
fun Map<Joint, Point>.transformed(scale: Float, dx: Float, dy: Float): Map<Joint, Point> =
    mapValues { (_, p) -> Point(p.x * scale + dx, p.y * scale + dy) }

/** 좌우 거울 반전: x 뒤집고 왼/오 라벨 교환. */
fun Map<Joint, Point>.mirrored(): Map<Joint, Point> =
    entries.associate { (joint, p) -> joint.opposite() to Point(1f - p.x, p.y) }

fun PersonPose.withConfidence(joint: Joint, confidence: Float): PersonPose =
    copy(keypoints = keypoints + (joint to keypoints.getValue(joint).copy(confidence = confidence)))

/** 정사각 기준 좌표를 가로/세로 비율 aspect(w/h)인 이미지의 정규화 좌표로 옮긴다. */
fun Map<Joint, Point>.inAspect(aspect: Float): Map<Joint, Point> =
    mapValues { (_, p) -> Point(0.5f + (p.x - 0.5f) / aspect, p.y) }
