package dev.stpark.namchin.core.composition

import dev.stpark.namchin.core.framing.PersonFrame
import dev.stpark.namchin.core.guidance.Guidance
import dev.stpark.namchin.core.guidance.Guidance.CutPoint
import dev.stpark.namchin.core.guidance.Guidance.Side
import dev.stpark.namchin.core.model.Joint
import dev.stpark.namchin.core.model.PersonPose
import dev.stpark.namchin.core.model.Point
import kotlin.math.abs
import kotlin.math.hypot

/**
 * 레퍼런스 없이 일반 인물 사진 규칙으로 구도를 코칭한다.
 * 좌표는 촬영 영역 기준(0..1). 영역 밖 관절은 신뢰도 0이지만 예측 위치는 남아 있어 잘림 판정에 쓴다.
 * 모든 규칙은 직전 프레임에 나온 안내를 더 느슨한 기준으로 유지한다 (깜빡임 방지).
 */
object CompositionAnalyzer {
    private const val MIN_CONFIDENCE = 0.5f

    // 잘림: 발목은 "닿거나 넘어갈 때"만 (발끝을 아래에 붙이는 게 목표이므로)
    private const val ANKLE_CUT_ENTER = 0.985f
    private const val ANKLE_CUT_EXIT = 0.97f
    private const val ANKLE_CUT_MAX = 1.06f
    private const val EDGE_BAND_ENTER = 0.04f
    private const val EDGE_BAND_EXIT = 0.06f

    // 다리 길게: 전신일 때 발목이 이보다 위면 안내
    private const val FEET_ENTER = 0.85f
    private const val FEET_EXIT = 0.88f

    /** 다리 관절 사슬이 자연스러운지: 정강이/허벅지 길이 비율. 가려진 다리의 엉뚱한 추측 배제. */
    private const val LEG_RATIO_MIN = 0.6f
    private const val LEG_RATIO_MAX = 1.6f

    // 머리: 정수리 ≈ 코 - 이 비율 × (어깨 중점 - 코) 세로 거리
    private const val HEAD_TOP_FACTOR = 0.7f
    private const val HEAD_CUT_ENTER = 0f
    private const val HEAD_CUT_EXIT = 0.03f
    private const val HEADROOM_ENTER = 0.20f
    private const val HEADROOM_EXIT = 0.15f

    /** 상반신 인물의 세로 크기(정수리~가장 아래)가 이 이상일 때만 머리 위 여백을 본다. */
    private const val LARGE_SPAN_ENTER = 0.5f
    private const val LARGE_SPAN_EXIT = 0.45f

    // 시선: (코 - 두 귀 중점) / 귀 사이 거리. 몸이 아닌 얼굴 방향
    private const val GAZE_YAW_ENTER = 0.4f
    private const val GAZE_YAW_EXIT = 0.25f
    private const val MIN_EAR_DISTANCE = 0.01f

    /** 오른쪽을 볼 때 얼굴이 이보다 오른쪽이면 안내 시작, 왼쪽 1/3(0.38)에 올 때까지 유지. 왼쪽은 대칭. */
    private const val GAZE_START = 0.45f
    private const val GAZE_THIRD = 0.38f

    fun analyze(pose: PersonPose, previous: Collection<Guidance>): List<Guidance> {
        fun active(cue: Guidance) = cue in previous
        fun reliable(joint: Joint) = pose.reliable(joint, MIN_CONFIDENCE)
        fun raw(joint: Joint) = pose.keypoints[joint]?.position

        val cues = mutableListOf<Guidance>()
        val frame = PersonFrame.from(pose)
        if (frame != null) {
            val legs = listOfNotNull(
                leg(pose, Joint.LEFT_HIP, Joint.LEFT_KNEE, Joint.LEFT_ANKLE),
                leg(pose, Joint.RIGHT_HIP, Joint.RIGHT_KNEE, Joint.RIGHT_ANKLE),
            )
            cutPoint(legs, ::reliable, ::raw, ::active)?.let { cues += Guidance.CutAtJoint(it) }

            val nose = raw(Joint.NOSE)
            val headTop = nose?.let { it.y - HEAD_TOP_FACTOR * (frame.anchorY - it.y) }
            val headCutLimit = if (active(Guidance.HeadCut)) HEAD_CUT_EXIT else HEAD_CUT_ENTER
            if (headTop != null && headTop < headCutLimit) cues += Guidance.HeadCut

            // 전신 = 자연스러운 다리 사슬 + 화면 안의 발목
            val feet = legs.mapNotNull { leg -> leg.ankle?.takeIf { leg.ankleVisible } }
            val fullBody = feet.size == 2 || (feet.size == 1 && active(Guidance.FeetToBottom))
            val feetLimit = if (active(Guidance.FeetToBottom)) FEET_EXIT else FEET_ENTER
            if (fullBody && feet.maxOf { it.y } < feetLimit) cues += Guidance.FeetToBottom

            // 전신에는 머리 위 여백을 말하지 않는다 (발끝 안내와 반대로 오가지 않게, 풍경 속 전신은 여백이 자연스러움)
            if (!fullBody && headTop != null && headTop >= 0f) {
                val lowest = pose.keypoints.values.filter { it.confidence >= MIN_CONFIDENCE }.maxOfOrNull { it.position.y }
                val on = active(Guidance.TooMuchHeadroom)
                val big = lowest != null && lowest - headTop > (if (on) LARGE_SPAN_EXIT else LARGE_SPAN_ENTER)
                if (big && headTop > (if (on) HEADROOM_EXIT else HEADROOM_ENTER)) cues += Guidance.TooMuchHeadroom
            }
        }
        gazeCue(::reliable, ::active)?.let { cues += it }
        return cues
    }

    /** 엉덩이 아래로 자연스럽게 이어지는 다리. 발목이 어색하면 발목만 버린다. */
    private class Leg(val knee: Point, val ankle: Point?, val ankleVisible: Boolean)

    private fun leg(pose: PersonPose, hipJoint: Joint, kneeJoint: Joint, ankleJoint: Joint): Leg? {
        val hip = pose.reliable(hipJoint, MIN_CONFIDENCE)
        val knee = pose.keypoints[kneeJoint]?.position
        val ankle = pose.keypoints[ankleJoint]
        if (hip == null || knee == null || knee.y <= hip.y) return null
        fun length(a: Point, b: Point) = hypot((a.x - b.x) * pose.aspectRatio, a.y - b.y)
        val thigh = length(hip, knee)
        val plausible = ankle?.takeIf {
            it.position.y > knee.y && length(knee, it.position) / thigh in LEG_RATIO_MIN..LEG_RATIO_MAX
        }
        val visible = plausible != null && plausible.confidence >= MIN_CONFIDENCE && plausible.position.y in 0f..1f
        return Leg(knee, plausible?.position, visible)
    }

    private fun cutPoint(
        legs: List<Leg>,
        reliable: (Joint) -> Point?,
        raw: (Joint) -> Point?,
        active: (Guidance) -> Boolean,
    ): CutPoint? {
        fun band(point: CutPoint) = if (active(Guidance.CutAtJoint(point))) EDGE_BAND_EXIT else EDGE_BAND_ENTER
        val ankleMin = if (active(Guidance.CutAtJoint(CutPoint.ANKLE))) ANKLE_CUT_EXIT else ANKLE_CUT_ENTER
        val kneeBand = band(CutPoint.KNEE)
        val wristBand = band(CutPoint.WRIST)
        fun nearEdge(p: Point?) = p != null &&
            (abs(p.x) <= wristBand || abs(p.x - 1f) <= wristBand || abs(p.y - 1f) <= wristBand)
        // 팔꿈치가 화면 안에 보일 때만 손목 잘림 판정
        fun wristCut(elbow: Joint, wrist: Joint) = reliable(elbow) != null && nearEdge(raw(wrist))
        return when {
            legs.any { leg -> leg.ankle?.y?.let { it in ankleMin..ANKLE_CUT_MAX } == true } -> CutPoint.ANKLE
            legs.any { abs(it.knee.y - 1f) <= kneeBand } -> CutPoint.KNEE
            wristCut(Joint.LEFT_ELBOW, Joint.LEFT_WRIST) || wristCut(Joint.RIGHT_ELBOW, Joint.RIGHT_WRIST) -> CutPoint.WRIST
            else -> null
        }
    }

    /** 얼굴(코와 두 귀)이 보는 방향 쪽에 여백이 부족하면 안내. 어깨가 가려져도 판단한다. */
    private fun gazeCue(reliable: (Joint) -> Point?, active: (Guidance) -> Boolean): Guidance? {
        val nose = reliable(Joint.NOSE) ?: return null
        val leftEar = reliable(Joint.LEFT_EAR) ?: return null
        val rightEar = reliable(Joint.RIGHT_EAR) ?: return null
        val yaw = (nose.x - (leftEar.x + rightEar.x) / 2) / maxOf(abs(leftEar.x - rightEar.x), MIN_EAR_DISTANCE)
        val right = Guidance.GazeSpace(Side.RIGHT)
        val left = Guidance.GazeSpace(Side.LEFT)
        val rightYaw = if (active(right)) GAZE_YAW_EXIT else GAZE_YAW_ENTER
        val leftYaw = if (active(left)) GAZE_YAW_EXIT else GAZE_YAW_ENTER
        val rightLimit = if (active(right)) GAZE_THIRD else GAZE_START
        val leftLimit = if (active(left)) 1f - GAZE_THIRD else 1f - GAZE_START
        return when {
            yaw > rightYaw && nose.x > rightLimit -> right
            yaw < -leftYaw && nose.x < leftLimit -> left
            else -> null
        }
    }
}
