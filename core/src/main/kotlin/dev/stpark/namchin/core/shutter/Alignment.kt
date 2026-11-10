package dev.stpark.namchin.core.shutter

import dev.stpark.namchin.core.guidance.Guidance
import dev.stpark.namchin.core.pose.PoseMatch

data class AlignmentResult(val cues: List<Guidance>, val aligned: Boolean)

/** 구도 안내 + 포즈 일치도를 합쳐 "지금 찍어도 되는가"를 판정한다. */
object Alignment {
    const val POSE_THRESHOLD = 0.8f

    /**
     * @param poseEnabled false면(야경 등) 포즈는 무시하고 구도만 본다
     */
    fun evaluate(framingCues: List<Guidance>, poseMatch: PoseMatch?, poseEnabled: Boolean): AlignmentResult {
        if (!poseEnabled) return AlignmentResult(framingCues, aligned = framingCues.isEmpty())
        val poseOk = poseMatch != null && poseMatch.score >= POSE_THRESHOLD
        val poseCue = poseMatch
            ?.takeIf { !poseOk }
            ?.worstPart
            ?.let { Guidance.Pose(it) }
        return AlignmentResult(
            cues = framingCues + listOfNotNull(poseCue),
            aligned = framingCues.isEmpty() && poseOk,
        )
    }
}
