package dev.stpark.namchin.core.composition

import dev.stpark.namchin.core.framing.FramingAnalyzer
import dev.stpark.namchin.core.framing.PersonFrame
import dev.stpark.namchin.core.guidance.Guidance
import dev.stpark.namchin.core.guidance.Guidance.Side
import dev.stpark.namchin.core.model.Joint
import dev.stpark.namchin.core.model.PersonPose

/**
 * 레퍼런스 없이 고르는 인물 구도 틀. 좌표는 촬영 영역 기준(0..1), 크기는 영역 높이 단위.
 * 목표 자리는 정면 차렷 인물(어깨~골반 : 코~어깨 = 2.5 : 1)의 비율로 정했다.
 */
enum class CompositionTemplate {
    /** 전신, 발끝을 화면 맨 아래에 — 다리가 길어 보인다 */
    FULL_BODY,

    /** 허리 위 상반신을 꽉 채움 — 인스타 4:5와 잘 맞는다 */
    HALF_BODY,

    /** 작은 전신을 3분할 선 위에, 반대쪽은 배경 */
    SIDE_THIRD,

    /** 수평선을 허리 높이에 두고 인물은 가운데 (T자) */
    HORIZON_T;

    /** 인물이 있어야 할 자리. side는 SIDE_THIRD에서만 쓴다 */
    fun target(side: Side?): PersonFrame = when (this) {
        FULL_BODY -> PersonFrame(0.5f, 0.251f, torso = 0.2925f, neck = 0.117f)
        HALF_BODY -> PersonFrame(0.5f, 0.42f, torso = 0.5f, neck = 0.2f)
        SIDE_THIRD -> PersonFrame(if (side == Side.RIGHT) 2f / 3 else 1f / 3, 0.49f, torso = 0.15f, neck = 0.06f)
        HORIZON_T -> PersonFrame(0.5f, 0.55f, torso = 0.1375f, neck = 0.055f)
    }

    companion object {
        /** 수평선 높이 (HORIZON_T) */
        const val HORIZON_Y = 0.6f

        /** 발끝 선 (FULL_BODY) */
        const val FEET_Y = 0.95f
    }
}

/**
 * @property cues 틀에 맞추기 위한 안내 (수평 제외)
 * @property side SIDE_THIRD에서 고른 쪽 (다음 프레임에 넘겨 흔들림 방지)
 * @property fits 인물이 틀에 맞음
 */
data class TemplateResult(val cues: List<Guidance>, val side: Side?, val fits: Boolean)

object TemplateCoach {
    /** 틀은 대략적인 목표라 레퍼런스보다 넓게 봐 준다 */
    const val SIZE_TOLERANCE = 0.15f
    const val POSITION_TOLERANCE = 0.06f

    /** 반대쪽 1/3로 바꾸려면 가운데를 이만큼 넘어가야 한다 */
    const val SIDE_SWITCH_MARGIN = 0.1f

    fun analyze(
        template: CompositionTemplate,
        pose: PersonPose?,
        previousSide: Side? = null,
        previousCues: Collection<Guidance> = emptyList(),
    ): TemplateResult {
        val frame = pose?.let(PersonFrame::from) ?: return TemplateResult(emptyList(), previousSide, fits = false)
        val side = if (template == CompositionTemplate.SIDE_THIRD) side(frame.anchorX, previousSide) else null
        val framing = FramingAnalyzer.analyze(
            template.target(side), frame, rollDegrees = null, previousCues, SIZE_TOLERANCE, POSITION_TOLERANCE,
        )
        val composition = CompositionAnalyzer.analyze(pose, previousCues).filterNot { conflicts(template, it) }
        val cues = framing + composition + feetCut(template, pose, framing + composition)
        return TemplateResult(cues, side, fits = cues.isEmpty())
    }

    /**
     * 전신 틀인데 두 발목이 화면에 없으면, 크기·위치가 허용 오차 안이어도 맞지 않다.
     * 인물이 아래로 치우친 것이므로 폰을 아래로 기울이게 한다 (다른 안내가 이미 있으면 그대로).
     */
    private fun feetCut(template: CompositionTemplate, pose: PersonPose, cues: List<Guidance>): List<Guidance> {
        if (template != CompositionTemplate.FULL_BODY || cues.isNotEmpty()) return emptyList()
        val feet = listOf(Joint.LEFT_ANKLE, Joint.RIGHT_ANKLE).count { pose.reliable(it, PersonFrame.MIN_CONFIDENCE) != null }
        return if (feet == 2) emptyList() else listOf(Guidance.TiltDown)
    }

    private fun side(anchorX: Float, previous: Side?): Side = when (previous) {
        Side.LEFT -> if (anchorX > 0.5f + SIDE_SWITCH_MARGIN) Side.RIGHT else Side.LEFT
        Side.RIGHT -> if (anchorX < 0.5f - SIDE_SWITCH_MARGIN) Side.LEFT else Side.RIGHT
        null -> if (anchorX < 0.5f) Side.LEFT else Side.RIGHT
    }

    /** 작은 전신을 배경 속에 두는 틀은 발끝·머리 위 여백 규칙과 반대, 3분할은 시선 여백 규칙과 부딪힌다 */
    private fun conflicts(template: CompositionTemplate, cue: Guidance): Boolean = when (template) {
        CompositionTemplate.FULL_BODY, CompositionTemplate.HALF_BODY -> false
        CompositionTemplate.SIDE_THIRD -> cue == Guidance.FeetToBottom || cue == Guidance.TooMuchHeadroom || cue is Guidance.GazeSpace
        CompositionTemplate.HORIZON_T -> cue == Guidance.FeetToBottom || cue == Guidance.TooMuchHeadroom
    }
}
