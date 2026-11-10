package dev.stpark.namchin.core.shutter

import dev.stpark.namchin.core.guidance.Guidance
import dev.stpark.namchin.core.pose.PoseMatch

data class AlignmentResult(val cues: List<Guidance>, val aligned: Boolean)

/** 구도 안내 + 포즈 일치도를 합쳐 "지금 찍어도 되는가"를 판정한다. */
object Alignment {
    const val POSE_THRESHOLD = 0.8f

    /** 이보다 낮은 마디가 하나라도 있으면 평균이 높아도 불일치. */
    const val MIN_PART_SCORE = 0.5f

    /**
     * @param framingCues 구도 안내. null이면 인물을 찾지 못한 것
     * @param poseMatch null이면 레퍼런스 포즈를 비교할 수 없는 것 → 구도만 본다
     * @param poseEnabled false면(야경 등) 포즈는 무시하고 구도만 본다
     */
    fun evaluate(framingCues: List<Guidance>?, poseMatch: PoseMatch?, poseEnabled: Boolean): AlignmentResult {
        if (framingCues == null) return AlignmentResult(listOf(Guidance.NoPerson), aligned = false)
        if (!poseEnabled || poseMatch == null) {
            return AlignmentResult(framingCues, aligned = framingCues.isEmpty())
        }
        val poseOk = poseMatch.score >= POSE_THRESHOLD && poseMatch.minPartScore >= MIN_PART_SCORE
        val poseCue = poseMatch.worstPart?.takeIf { !poseOk }?.let { Guidance.Pose(it) }
        return AlignmentResult(
            cues = framingCues + listOfNotNull(poseCue),
            aligned = framingCues.isEmpty() && poseOk,
        )
    }
}
