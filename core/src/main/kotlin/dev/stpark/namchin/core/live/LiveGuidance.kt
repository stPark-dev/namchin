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
 * @property rollDegrees 폰 기울기 (시계 방향 양수)
 * @property poseEnabled false면 포즈 비교 생략 (야경 등)
 */
data class LiveInput(
    val reference: PersonPose,
    val current: PersonPose?,
    val rollDegrees: Float,
    val poseEnabled: Boolean,
    val nowMs: Long,
)

/** 프레임 사이에 이어지는 상태. */
data class LiveState(
    val previousCues: List<Guidance> = emptyList(),
    val preferMirrored: Boolean = false,
    val guidance: GuidanceState = GuidanceState(),
)

data class LiveOutput(
    val display: Guidance?,
    val announce: Guidance?,
    val aligned: Boolean,
    val match: PoseMatch?,
)

/** 카메라 한 프레임 → 안내/일치 판정. 순수 함수. */
object LiveGuidance {

    fun step(state: LiveState, input: LiveInput): Pair<LiveState, LiveOutput> {
        val referenceFrame = PersonFrame.from(input.reference)
        val currentFrame = input.current?.let(PersonFrame::from)
        val framingCues = when {
            currentFrame == null -> null
            referenceFrame == null -> emptyList()
            else -> FramingAnalyzer.analyze(referenceFrame, currentFrame, input.rollDegrees, state.previousCues)
        }
        val match = input.current
            ?.takeIf { input.poseEnabled }
            ?.let { PoseSimilarity.compare(input.reference, it, state.preferMirrored) }
        val alignment = Alignment.evaluate(framingCues, match, input.poseEnabled)
        val decision = GuidancePrioritizer.decide(alignment.cues, state.guidance, input.nowMs)

        val nextState = LiveState(
            previousCues = alignment.cues,
            preferMirrored = match?.mirrored ?: state.preferMirrored,
            guidance = decision.state,
        )
        return nextState to LiveOutput(decision.display, decision.announce, alignment.aligned, match)
    }
}
