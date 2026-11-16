package dev.stpark.namchin.core.composition

import dev.stpark.namchin.core.model.Joint
import dev.stpark.namchin.core.model.PersonPose
import dev.stpark.namchin.core.model.Point
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * 포즈 한마디. 두 팔을 몸 옆에 곧게 내린 '차렷'은 사진이 굳어 보인다 —
 * 한 손을 얼굴·허리로 가져가 얼굴·팔·몸이 삼각형을 이루면 안정감 있고 자연스럽다.
 */
object PoseHint {
    private const val MIN_CONFIDENCE = 0.5f

    /** 팔꿈치 각도가 이 이상이면 곧은 팔. 직전에 차렷이었으면 더 느슨하게 */
    private const val STRAIGHT_ENTER_DEG = 160.0
    private const val STRAIGHT_EXIT_DEG = 145.0

    /** 어깨→손목이 세로에서 이 이내면 아래로 내린 팔 */
    private const val DOWN_ENTER_DEG = 25.0
    private const val DOWN_EXIT_DEG = 35.0

    /** 두 팔 모두 보이고, 모두 곧게 아래로 내렸는가. */
    fun stiffArms(pose: PersonPose, wasStiff: Boolean): Boolean =
        armDown(pose, Joint.LEFT_SHOULDER, Joint.LEFT_ELBOW, Joint.LEFT_WRIST, wasStiff) &&
            armDown(pose, Joint.RIGHT_SHOULDER, Joint.RIGHT_ELBOW, Joint.RIGHT_WRIST, wasStiff)

    private fun armDown(pose: PersonPose, shoulderJ: Joint, elbowJ: Joint, wristJ: Joint, wasStiff: Boolean): Boolean {
        val shoulder = pose.reliable(shoulderJ, MIN_CONFIDENCE) ?: return false
        val elbow = pose.reliable(elbowJ, MIN_CONFIDENCE) ?: return false
        val wrist = pose.reliable(wristJ, MIN_CONFIDENCE) ?: return false
        val ax = pose.aspectRatio
        fun dx(a: Point, b: Point) = (b.x - a.x) * ax
        val straight = if (wasStiff) STRAIGHT_EXIT_DEG else STRAIGHT_ENTER_DEG
        val down = if (wasStiff) DOWN_EXIT_DEG else DOWN_ENTER_DEG
        val elbowDeg = angleDeg(dx(elbow, shoulder), shoulder.y - elbow.y, dx(elbow, wrist), wrist.y - elbow.y)
        val fromVertical = Math.toDegrees(atan2(abs(dx(shoulder, wrist)).toDouble(), (wrist.y - shoulder.y).toDouble()))
        return wrist.y > shoulder.y && elbowDeg >= straight && fromVertical <= down
    }

    private fun angleDeg(ax: Float, ay: Float, bx: Float, by: Float): Double {
        val norm = hypot(ax, ay) * hypot(bx, by)
        if (norm == 0f) return 0.0
        return Math.toDegrees(acos(((ax * bx + ay * by) / norm).toDouble().coerceIn(-1.0, 1.0)))
    }
}
