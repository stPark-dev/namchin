package dev.stpark.namchin.core.live

import dev.stpark.namchin.core.composition.CompositionAnalyzer
import dev.stpark.namchin.core.composition.CompositionTemplate
import dev.stpark.namchin.core.composition.TemplateCoach
import dev.stpark.namchin.core.framing.FramingAnalyzer
import dev.stpark.namchin.core.framing.PersonFrame
import dev.stpark.namchin.core.guidance.Guidance
import dev.stpark.namchin.core.guidance.GuidancePrioritizer
import dev.stpark.namchin.core.guidance.GuidanceState
import dev.stpark.namchin.core.model.PersonPose
import dev.stpark.namchin.core.pose.PoseMatch
import dev.stpark.namchin.core.pose.PoseDirection
import dev.stpark.namchin.core.pose.PoseSimilarity
import dev.stpark.namchin.core.shutter.Alignment
import dev.stpark.namchin.core.shutter.AutoShutter
import dev.stpark.namchin.core.shutter.ShutterState

/**
 * @property reference 레퍼런스 포즈
 * @property current 촬영 영역 기준으로 바꾼 현재 포즈. 인물 미검출이면 null
 * @property rollDegrees 폰 기울기 (시계 방향 양수). 알 수 없으면 null
 * @property poseEnabled false면 포즈 비교 생략 (야경 등)
 * @property autoShutter 자동 셔터 사용 여부
 * @property lowLight 어두움(야간): 골격 인식이 부정확하므로 포즈 비교와 자동 셔터를 끈다
 * @property shaky 손떨림. 어두울 때만 안내한다
 * @property zoomRatio 지금 카메라 배율
 * @property teleTarget 확대 촬영 설정 배율 (1이면 끔)
 */
data class LiveInput(
    val reference: PersonPose,
    val current: PersonPose?,
    val rollDegrees: Float?,
    val poseEnabled: Boolean,
    val nowMs: Long,
    val autoShutter: Boolean = false,
    val lowLight: Boolean = false,
    val shaky: Boolean = false,
    val zoomRatio: Float = 1f,
    val teleTarget: Float = 1f,
)

/** 프레임 사이에 이어지는 상태. */
data class LiveState(
    val previousCues: List<Guidance> = emptyList(),
    val preferMirrored: Boolean = false,
    /** 구도 틀 3분할에서 고른 쪽 */
    val templateSide: Guidance.Side? = null,
    /** 구도 틀에 마지막으로 맞았던 시각 (인식이 잠깐 끊겨도 맞음을 유지) */
    val templateFitAtMs: Long? = null,
    val guidance: GuidanceState = GuidanceState(),
    val shutter: ShutterState = ShutterState.Idle,
)

/**
 * @property cues 이번 프레임의 모든 안내 (display는 그중 최우선)
 * @property aligned 찍어도 되는가. 비교 기준(구도·포즈)이 하나도 없으면 항상 false
 * @property fire 이번 프레임에 자동 셔터를 누른다
 */
data class LiveOutput(
    val display: Guidance?,
    val cues: List<Guidance>,
    val announce: Guidance?,
    val aligned: Boolean,
    val match: PoseMatch?,
    val fire: Boolean = false,
)

/** 카메라 한 프레임 → 안내/일치 판정. 순수 함수. */
object LiveGuidance {
    /** 구도 틀에 맞은 뒤 인물 인식이 이만큼 끊겨도 맞음을 유지 (진동·문구가 반복되지 않게) */
    const val FIT_GRACE_MS = 600L

    /** 수동으로 찍었으면 자동 셔터가 바로 이어 찍지 않게 쉰다. */
    fun afterManualShot(state: LiveState, nowMs: Long): LiveState =
        state.copy(shutter = AutoShutter.afterManualShot(nowMs))

    /** 자동 셔터가 발사됐지만 실제로 찍지 못했으면(셔터 막힘 등) 처음부터 다시 기다린다. */
    fun cancelFire(state: LiveState): LiveState = state.copy(shutter = ShutterState.Idle)

    /**
     * 기본 모드(레퍼런스 없음): 수평 + 일반 구도 규칙, 구도 틀을 골랐으면 틀에 맞추는 안내.
     * 풍경만 찍을 수도 있으니 인물이 없어도 재촉하지 않는다.
     * 자동 셔터는 없다. aligned는 구도 틀에 맞고 다른 안내도 없을 때만 (초록 표시용).
     */
    fun stepBasic(
        state: LiveState,
        current: PersonPose?,
        rollDegrees: Float?,
        nowMs: Long,
        lowLight: Boolean = false,
        shaky: Boolean = false,
        zoomRatio: Float = 1f,
        teleTarget: Float = 1f,
        template: CompositionTemplate? = null,
    ): Pair<LiveState, LiveOutput> {
        val tele = TeleShot.cue(zoomRatio, teleTarget, current != null)
        val fitted = template?.let { TemplateCoach.analyze(it, current, state.templateSide, state.previousCues) }
        // 배율을 바꾸면 구도가 통째로 바뀌므로, 그동안 잘림·여백 안내는 하지 않는다
        val composition = when {
            tele != null -> emptyList()
            fitted != null -> fitted.cues
            else -> current?.let { CompositionAnalyzer.analyze(it, state.previousCues) }.orEmpty()
        }
        val cues = holdSteady(lowLight, shaky) +
            listOfNotNull(rollDegrees?.let { FramingAnalyzer.levelCue(it, state.previousCues) }) +
            composition + listOfNotNull(tele)
        val decision = GuidancePrioritizer.decide(cues, state.guidance, nowMs)
        // 야간엔 골격이 부정확하므로 맞음(노랑·진동)을 내지 않는다
        val fitsNow = fitted?.fits == true && cues.isEmpty() && !lowLight
        val lastFit = state.templateFitAtMs
        val dropout = template != null && current == null && tele == null &&
            lastFit != null && nowMs - lastFit <= FIT_GRACE_MS
        val aligned = fitsNow || dropout
        val nextState = state.copy(
            previousCues = cues,
            guidance = decision.state,
            templateSide = fitted?.side,
            templateFitAtMs = if (fitsNow) nowMs else lastFit.takeIf { dropout },
        )
        return nextState to LiveOutput(decision.display, cues, decision.announce, aligned = aligned, match = null)
    }

    private fun holdSteady(lowLight: Boolean, shaky: Boolean) =
        if (lowLight && shaky) listOf(Guidance.HoldSteady) else emptyList()

    fun step(state: LiveState, input: LiveInput): Pair<LiveState, LiveOutput> {
        val poseEnabled = input.poseEnabled && !input.lowLight
        val referenceFrame = PersonFrame.from(input.reference)
        val currentFrame = input.current?.let(PersonFrame::from)
        val levelCue = input.rollDegrees?.let { FramingAnalyzer.levelCue(it, state.previousCues) }
        val framingCues = when {
            currentFrame == null -> null
            referenceFrame == null -> listOfNotNull(levelCue)
            else -> FramingAnalyzer.analyze(referenceFrame, currentFrame, input.rollDegrees, state.previousCues)
        }
        val match = input.current
            ?.takeIf { poseEnabled }
            ?.let { PoseSimilarity.compare(input.reference, it, state.preferMirrored) }
        val alignment = Alignment.evaluate(framingCues, match, poseEnabled).let { a ->
            // 포즈 안내에 "어떻게" 고칠지 붙인다
            val current = input.current
            if (current == null || match == null) a else a.copy(cues = a.cues.map { cue ->
                if (cue !is Guidance.Pose) return@map cue
                val previous = state.previousCues.firstNotNullOfOrNull { (it as? Guidance.Pose)?.takeIf { p -> p.part == cue.part }?.direction }
                cue.copy(direction = PoseDirection.of(input.reference, current, cue.part, match.mirrored, previous))
            })
        }
        // 인물이 없어도 수평은 미리 맞출 수 있게 함께 낸다
        val tele = TeleShot.cue(input.zoomRatio, input.teleTarget, input.current != null)
        val cues = TeleShot.withoutDistance(
            holdSteady(input.lowLight, input.shaky) +
                (if (framingCues == null) alignment.cues + listOfNotNull(levelCue) else alignment.cues) +
                listOfNotNull(tele),
        )
        val hasConstraint = referenceFrame != null || match != null
        val decision = GuidancePrioritizer.decide(cues, state.guidance, input.nowMs)
        // 야간엔 포즈를 비교하지 않으므로 "맞음"(초록·진동·'그대로 유지')을 내지 않는다
        val aligned = alignment.aligned && hasConstraint && !input.lowLight && tele == null
        val shutter = AutoShutter.step(state.shutter, aligned, input.autoShutter && !input.lowLight, input.nowMs)

        val nextState = LiveState(
            previousCues = cues,
            preferMirrored = match?.mirrored ?: state.preferMirrored,
            guidance = decision.state,
            shutter = shutter.state,
        )
        return nextState to LiveOutput(
            display = decision.display,
            cues = cues,
            announce = decision.announce,
            aligned = aligned,
            match = match,
            fire = shutter.fire,
        )
    }
}
