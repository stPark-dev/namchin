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

    /** 작은 전신을 3분할 교차점에 (몸 가운데가 아래쪽 1/3 선), 반대쪽은 배경 */
    SIDE_THIRD,

    /** 수평선을 허리 높이에 두고 인물은 가운데 (T자) */
    HORIZON_T,

    /** 작은 전신을 황금 나선이 감겨 들어가는 점에 */
    GOLDEN_SPIRAL,

    /** 비스듬한 언덕·계단 선 위에 작게, 위쪽은 하늘로 크게 비운다 */
    DIAGONAL_SPACE,

    /** 길·난간처럼 화면 아래에서 모이는 선 끝에 작게 */
    LEADING_LINES,

    /**
     * 가운데 세로 칸에 허리 위 상반신, 정수리는 위 가로선 바로 아래 (기류: 가장 쉬운 기본, 1.5~2배로 물러나서).
     * 허벅지 중간에서 잘리게 둔다 — 무릎·발목 근처에서 끝나면 관절 잘림 안내와 부딪혀 맞추기 어렵다.
     */
    CENTER,

    /** 얼굴(코)을 위쪽 3분할 교차점에, 허리 위 상반신 (기류: 상단 두 꼭지점에 인물) */
    FACE_THIRD,

    /** 작은 전신을 아래 가운데 칸에, 위 2/3는 하늘·풍경 여백 (기류: 하단 중앙 배치) */
    BOTTOM_CENTER;

    /** 왼쪽·오른쪽 중 인물에 가까운 쪽을 고르는 틀 */
    val hasSide: Boolean get() = this == SIDE_THIRD || this == GOLDEN_SPIRAL || this == DIAGONAL_SPACE || this == FACE_THIRD

    /** 인물이 있어야 할 자리. side는 hasSide인 틀에서만 쓴다 */
    fun target(side: Side?): PersonFrame = when (this) {
        FULL_BODY -> PersonFrame(0.5f, 0.251f, torso = 0.2925f, neck = 0.117f)
        HALF_BODY -> PersonFrame(0.5f, 0.42f, torso = 0.5f, neck = 0.2f)
        SIDE_THIRD -> PersonFrame(if (side == Side.RIGHT) 2f / 3 else 1f / 3, 0.538f, torso = 0.15f, neck = 0.06f)
        HORIZON_T -> PersonFrame(0.5f, 0.55f, torso = 0.1375f, neck = 0.055f)
        // 골반(몸 가운데)이 나선의 눈에 온다
        GOLDEN_SPIRAL -> PersonFrame(onSide(1f - GoldenSpiral.EYE, side), GoldenSpiral.EYE - SMALL_TORSO, SMALL_TORSO, SMALL_NECK)
        DIAGONAL_SPACE -> PersonFrame(onSide(DIAGONAL_X, side), SMALL_FEET_Y - TINY_TORSO * FEET_PER_TORSO, TINY_TORSO, TINY_NECK)
        LEADING_LINES -> PersonFrame(0.5f, SMALL_FEET_Y - TINY_TORSO * FEET_PER_TORSO, TINY_TORSO, TINY_NECK)
        // 정수리(어깨 - 목 × 1.7)가 위 가로선 바로 아래
        CENTER -> PersonFrame(0.5f, CENTER_HEAD_TOP + HEAD_PER_NECK * CENTER_TORSO * NECK_PER_TORSO, CENTER_TORSO, CENTER_TORSO * NECK_PER_TORSO)
        // 코(어깨 - 목)가 위 가로선
        FACE_THIRD -> PersonFrame(if (side == Side.RIGHT) 2f / 3 else 1f / 3, 1f / 3 + FACE_THIRD_TORSO * NECK_PER_TORSO, FACE_THIRD_TORSO, FACE_THIRD_TORSO * NECK_PER_TORSO)
        // 정수리가 아래 가로선(2/3) 근처, 발끝 약 0.95
        BOTTOM_CENTER -> PersonFrame(0.5f, 2f / 3 + HEAD_PER_NECK * TINY_NECK, TINY_TORSO, TINY_NECK)
    }

    companion object {
        /** 몸통 대비 발목 높이 (정면 차렷) */
        const val FEET_PER_TORSO = 2.4f

        /** 대각선·모이는 길 틀에서 발끝이 닿는 높이 */
        const val SMALL_FEET_Y = 0.72f

        /** 대각선 틀에서 인물 자리 (왼쪽 기준) */
        const val DIAGONAL_X = 0.25f

        private const val SMALL_TORSO = 0.1f
        private const val SMALL_NECK = 0.04f
        // 더 작으면 골반 인식이 흔들려 맞음/안 맞음이 깜빡인다
        private const val TINY_TORSO = 0.09f
        private const val TINY_NECK = 0.036f

        /** 목(코~어깨) / 몸통 (정면 차렷) */
        private const val NECK_PER_TORSO = 0.4f

        /** 정수리~어깨 / 목 */
        private const val HEAD_PER_NECK = 1.7f
        // 크기 ±15%·위치 ±0.06 안에서 화면 아래 끝이 늘 허벅지 중간에 오도록 (무릎이 1.04 아래로 안 올라옴)
        private const val CENTER_TORSO = 0.38f
        private const val CENTER_HEAD_TOP = 0.335f
        private const val FACE_THIRD_TORSO = 0.46f

        /** 왼쪽 기준 x를 고른 쪽으로 */
        private fun onSide(leftX: Float, side: Side?) = if (side == Side.RIGHT) 1f - leftX else leftX

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
        val side = if (template.hasSide) side(frame.anchorX, previousSide) else null
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

    /** 작은 전신을 배경 속에 두는 틀은 발끝·머리 위 여백 규칙과 반대, 한쪽에 두는 틀은 시선 여백 규칙과 부딪힌다 */
    private val CENTERED = setOf(CompositionTemplate.CENTER, CompositionTemplate.BOTTOM_CENTER)

    private fun conflicts(template: CompositionTemplate, cue: Guidance): Boolean = when (template) {
        CompositionTemplate.FULL_BODY, CompositionTemplate.HALF_BODY -> false
        // 가운데에 두는 틀에서 고개를 돌리면 "옆에 여백" 안내가 가운데로 끌어오는 안내와 부딪힌다
        else -> cue == Guidance.FeetToBottom || cue == Guidance.TooMuchHeadroom ||
            (cue is Guidance.GazeSpace && (template.hasSide || template in CENTERED))
    }
}
