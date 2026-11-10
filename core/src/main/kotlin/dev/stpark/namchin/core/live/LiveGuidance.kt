package dev.stpark.namchin.core.live

import dev.stpark.namchin.core.framing.FramingAnalyzer
import dev.stpark.namchin.core.framing.PersonFrame
import dev.stpark.namchin.core.guidance.Guidance
import dev.stpark.namchin.core.guidance.GuidancePrioritizer
import dev.stpark.namchin.core.guidance.GuidanceState
import dev.stpark.namchin.core.model.PersonPose
import dev.stpark.namchin.core.pose.PoseMatch
import dev.stpark.namchin.core.pose.PoseSimilarity
import dev.stpark.namchin.core.shutter.Alignment

/**
 * @property reference 레퍼런스 포즈
 * @property current 촬영 영역 기준으로 바꾼 현재 포즈. 인물 미검출이면 null
 * @property rollDegrees 폰 기울기 (시계 방향 양수). 알 수 없으면 null
 * @property poseEnabled false면 포즈 비교 생략 (야경 등)
 */
data class LiveInput(
    val reference: PersonPose,
    val current: PersonPose?,
    val rollDegrees: Float?,
    val poseEnabled: Boolean,
    val nowMs: Long,
)

/** 프레임 사이에 이어지는 상태. */
data class LiveState(
    val previousCues: List<Guidance> = emptyList(),
    val preferMirrored: Boolean = false,
    val guidance: GuidanceState = GuidanceState(),
)

/**
 * @property cues 이번 프레임의 모든 안내 (display는 그중 최우선)
 * @property aligned 찍어도 되는가. 비교 기준(구도·포즈)이 하나도 없으면 항상 false
 */
data class LiveOutput(
    val display: Guidance?,
    val cues: List<Guidance>,
    val announce: Guidance?,
    val aligned: Boolean,
    val match: PoseMatch?,
)

/** 카메라 한 프레임 → 안내/일치 판정. 순수 함수. */
object LiveGuidance {

    fun step(state: LiveState, input: LiveInput): Pair<LiveState, LiveOutput> {
        val referenceFrame = PersonFrame.from(input.reference)
        val currentFrame = input.current?.let(PersonFrame::from)
        val levelCue = input.rollDegrees?.let { FramingAnalyzer.levelCue(it, state.previousCues) }
        val framingCues = when {
            currentFrame == null -> null
            referenceFrame == null -> listOfNotNull(levelCue)
            else -> FramingAnalyzer.analyze(referenceFrame, currentFrame, input.rollDegrees, state.previousCues)
        }
        val match = input.current
            ?.takeIf { input.poseEnabled }
            ?.let { PoseSimilarity.compare(input.reference, it, state.preferMirrored) }
        val alignment = Alignment.evaluate(framingCues, match, input.poseEnabled)
        // 인물이 없어도 수평은 미리 맞출 수 있게 함께 낸다
        val cues = if (framingCues == null) alignment.cues + listOfNotNull(levelCue) else alignment.cues
        val hasConstraint = referenceFrame != null || match != null
        val decision = GuidancePrioritizer.decide(cues, state.guidance, input.nowMs)

        val nextState = LiveState(
            previousCues = cues,
            preferMirrored = match?.mirrored ?: state.preferMirrored,
            guidance = decision.state,
        )
        return nextState to LiveOutput(
            display = decision.display,
            cues = cues,
            announce = decision.announce,
            aligned = alignment.aligned && hasConstraint,
            match = match,
        )
    }
}
