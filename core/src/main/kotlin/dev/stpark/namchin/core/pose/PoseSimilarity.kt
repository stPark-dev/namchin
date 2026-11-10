package dev.stpark.namchin.core.pose

import dev.stpark.namchin.core.model.BodyPart
import dev.stpark.namchin.core.model.PersonPose
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2

/**
 * @property score 0..1, 비교 가능한 마디들의 평균 일치도
 * @property mirrored 레퍼런스를 좌우반전했을 때 더 잘 맞았는지
 * @property worstPart 현재 인물 기준으로 가장 어긋난 마디
 */
data class PoseMatch(
    val score: Float,
    val mirrored: Boolean,
    val partScores: Map<BodyPart, Float>,
) {
    val worstPart: BodyPart? = partScores.minByOrNull { it.value }?.key
}

/**
 * 마디 방향(각도)만 비교하므로 촬영 거리·인물 위치와 무관하다.
 * 레퍼런스를 거울 반전한 버전과도 비교해 더 높은 쪽을 택한다.
 */
object PoseSimilarity {
    const val MIN_CONFIDENCE = 0.5f
    const val MIN_PARTS = 4

    /** 이 각도 이상 어긋나면 해당 마디 점수 0. */
    private const val MAX_ANGLE_DIFF = PI / 2

    fun compare(reference: PersonPose, current: PersonPose): PoseMatch? {
        val direct = partScores(reference, current, mirrored = false)
        val mirrored = partScores(reference, current, mirrored = true)
        val best = listOf(direct to false, mirrored to true)
            .filter { (scores, _) -> scores.size >= MIN_PARTS }
            .maxByOrNull { (scores, _) -> scores.values.average() }
            ?: return null
        val (scores, isMirrored) = best
        return PoseMatch(scores.values.average().toFloat(), isMirrored, scores)
    }

    private fun partScores(
        reference: PersonPose,
        current: PersonPose,
        mirrored: Boolean,
    ): Map<BodyPart, Float> = BodyPart.entries.mapNotNull { part ->
        val currentAngle = angleOf(current, part, flip = false) ?: return@mapNotNull null
        val refPart = if (mirrored) part.opposite() else part
        val refAngle = angleOf(reference, refPart, flip = mirrored) ?: return@mapNotNull null
        part to scoreFor(angleDiff(currentAngle, refAngle))
    }.toMap()

    private fun angleOf(pose: PersonPose, part: BodyPart, flip: Boolean): Double? {
        val from = pose.reliable(part.from, MIN_CONFIDENCE) ?: return null
        val to = pose.reliable(part.to, MIN_CONFIDENCE) ?: return null
        val dx = (to.x - from.x).toDouble().let { if (flip) -it else it }
        return atan2((to.y - from.y).toDouble(), dx)
    }

    private fun angleDiff(a: Double, b: Double): Double {
        val d = abs(a - b) % (2 * PI)
        return if (d > PI) 2 * PI - d else d
    }

    private fun scoreFor(diff: Double): Float =
        (1 - diff / MAX_ANGLE_DIFF).coerceAtLeast(0.0).toFloat()

    private fun BodyPart.opposite(): BodyPart =
        BodyPart.entries.first { it.from == from.opposite() && it.to == to.opposite() }
}
