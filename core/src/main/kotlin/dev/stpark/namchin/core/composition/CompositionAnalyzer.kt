package dev.stpark.namchin.core.composition

import dev.stpark.namchin.core.framing.PersonFrame
import dev.stpark.namchin.core.guidance.Guidance
import dev.stpark.namchin.core.guidance.Guidance.CutPoint
import dev.stpark.namchin.core.guidance.Guidance.Side
import dev.stpark.namchin.core.model.Joint
import dev.stpark.namchin.core.model.PersonPose
import dev.stpark.namchin.core.model.Point
import kotlin.math.abs

/**
 * 레퍼런스 없이 일반 인물 사진 규칙으로 구도를 코칭한다.
 * 좌표는 촬영 영역 기준 (0..1). 직전 프레임 안내는 더 느슨한 기준으로 유지한다 (깜빡임 방지).
 */
object CompositionAnalyzer {
    private const val MIN_CONFIDENCE = 0.5f

    /** 가장자리에서 이 거리 안(안팎)에 관절이 있으면 "관절에서 잘림". */
    private const val CUT_BAND = 0.04f

    /** 전신일 때 발끝이 이보다 위면 "발끝을 아래로". 해제는 FEET_EXIT 이상. */
    private const val FEET_ENTER = 0.85f
    private const val FEET_EXIT = 0.88f

    /** 정수리 ≈ 코 - 이 비율 × (어깨 중점 - 코) 세로 거리. */
    private const val HEAD_TOP_FACTOR = 0.7f
    private const val HEAD_CUT_ENTER = 0f
    private const val HEAD_CUT_EXIT = 0.03f
    private const val HEADROOM_ENTER = 0.20f
    private const val HEADROOM_EXIT = 0.15f

    /** 인물 아래쪽이 이보다 아래까지 차 있어야 "머리 위 여백 과다"를 본다 (풍경 속 작은 인물 제외). */
    private const val LARGE_SUBJECT_BOTTOM = 0.8f

    /** (코 - 어깨 중점) / 어깨 폭 이 이보다 크면 옆을 보는 것으로 본다. */
    private const val GAZE_OFFSET = 0.3f

    /** 시선 반대쪽 1/3 지점 근처면 이미 여백이 있다. */
    private const val GAZE_PLACED_LEFT = 0.45f
    private const val GAZE_PLACED_RIGHT = 0.55f

    fun analyze(pose: PersonPose, previous: Collection<Guidance>): List<Guidance> {
        val frame = PersonFrame.from(pose) ?: return emptyList()
        fun reliable(joint: Joint) = pose.reliable(joint, MIN_CONFIDENCE)
        fun raw(joint: Joint) = pose.keypoints[joint]?.position
        fun limit(cue: Guidance, enter: Float, exit: Float) = if (cue in previous) exit else enter

        val cues = mutableListOf<Guidance>()
        cutPoint(::reliable, ::raw)?.let { cues += Guidance.CutAtJoint(it) }

        val headTop = reliable(Joint.NOSE)?.let { it.y - HEAD_TOP_FACTOR * (frame.anchorY - it.y) }
        if (headTop != null && headTop < limit(Guidance.HeadCut, HEAD_CUT_ENTER, HEAD_CUT_EXIT)) {
            cues += Guidance.HeadCut
        }

        val ankles = listOfNotNull(reliable(Joint.LEFT_ANKLE), reliable(Joint.RIGHT_ANKLE))
            .filter { it.y in 0f..1f }
        if (ankles.size == 2 && ankles.maxOf { it.y } < limit(Guidance.FeetToBottom, FEET_ENTER, FEET_EXIT)) {
            cues += Guidance.FeetToBottom
        }

        val lowest = pose.keypoints.values.filter { it.confidence >= MIN_CONFIDENCE }.maxOfOrNull { it.position.y }
        if (headTop != null && lowest != null && lowest > LARGE_SUBJECT_BOTTOM &&
            headTop > limit(Guidance.TooMuchHeadroom, HEADROOM_ENTER, HEADROOM_EXIT)
        ) {
            cues += Guidance.TooMuchHeadroom
        }

        gazeCue(pose, frame)?.let { cues += it }
        return cues
    }

    private fun cutPoint(reliable: (Joint) -> Point?, raw: (Joint) -> Point?): CutPoint? {
        fun nearBottom(p: Point?) = p != null && abs(p.y - 1f) <= CUT_BAND
        fun nearEdge(p: Point?) = p != null &&
            (abs(p.x) <= CUT_BAND || abs(p.x - 1f) <= CUT_BAND || abs(p.y - 1f) <= CUT_BAND)
        // 바로 위 관절이 화면 안에 보일 때만 판정 (몸이 아예 안 보이면 잘림이 아님)
        fun cut(parent: Joint, child: Joint, near: (Point?) -> Boolean) = reliable(parent) != null && near(raw(child))
        return when {
            cut(Joint.LEFT_KNEE, Joint.LEFT_ANKLE, ::nearBottom) || cut(Joint.RIGHT_KNEE, Joint.RIGHT_ANKLE, ::nearBottom) -> CutPoint.ANKLE
            cut(Joint.LEFT_HIP, Joint.LEFT_KNEE, ::nearBottom) || cut(Joint.RIGHT_HIP, Joint.RIGHT_KNEE, ::nearBottom) -> CutPoint.KNEE
            cut(Joint.LEFT_ELBOW, Joint.LEFT_WRIST, ::nearEdge) || cut(Joint.RIGHT_ELBOW, Joint.RIGHT_WRIST, ::nearEdge) -> CutPoint.WRIST
            else -> null
        }
    }

    private fun gazeCue(pose: PersonPose, frame: PersonFrame): Guidance? {
        val nose = pose.reliable(Joint.NOSE, MIN_CONFIDENCE) ?: return null
        val left = pose.reliable(Joint.LEFT_SHOULDER, MIN_CONFIDENCE) ?: return null
        val right = pose.reliable(Joint.RIGHT_SHOULDER, MIN_CONFIDENCE) ?: return null
        // 옆모습이면 어깨 폭이 거의 0 → 목 길이로 하한을 둔다
        val scale = maxOf(abs(left.x - right.x), (frame.neck ?: 0f) * 0.5f)
        if (scale <= 0f) return null
        val offset = (nose.x - frame.anchorX) / scale
        return when {
            offset > GAZE_OFFSET && frame.anchorX > GAZE_PLACED_LEFT -> Guidance.GazeSpace(Side.RIGHT)
            offset < -GAZE_OFFSET && frame.anchorX < GAZE_PLACED_RIGHT -> Guidance.GazeSpace(Side.LEFT)
            else -> null
        }
    }
}
