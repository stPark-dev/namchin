package dev.stpark.namchin.core.pose

import dev.stpark.namchin.core.model.BodyPart
import dev.stpark.namchin.core.model.PersonPose
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2

/**
 * @property score 0..1, 레퍼런스에 보이는 마디 기준 평균 일치도 (현재 안 보이는 마디는 0)
 * @property mirrored 레퍼런스를 좌우반전해서 비교했는지
 * @property partScores 현재 인물 기준 마디별 점수
 */
data class PoseMatch(
    val score: Float,
    val mirrored: Boolean,
    val partScores: Map<BodyPart, Float>,
) {
    val worstPart: BodyPart? = partScores.minByOrNull { it.value }?.key
    val minPartScore: Float = partScores.values.minOrNull() ?: 0f
}

/**
 * 마디 방향(각도)만 비교하므로 촬영 거리·인물 위치와 무관하다.
 * 각도는 이미지 비율을 보정해 계산한다.
 * 레퍼런스를 거울 반전한 버전과도 비교하되, 이전 선택을 MIRROR_MARGIN 이상 이겨야 바꾼다.
 */
object PoseSimilarity {
    const val MIN_CONFIDENCE = 0.5f
    const val MIN_PARTS = 4
    const val MIRROR_MARGIN = 0.05f

    /** 이 각도 이상 어긋나면 해당 마디 점수 0. */
    private const val MAX_ANGLE_DIFF = PI / 2

    /** 레퍼런스가 포즈 비교에 쓸 만한지. false면 포즈 매칭을 끄고 구도만 본다. */
    fun isComparable(reference: PersonPose): Boolean = comparablePartCount(reference) >= MIN_PARTS

    /** 신뢰도 높은 양 끝 관절이 있는 마디 수. */
    fun comparablePartCount(pose: PersonPose): Int =
        BodyPart.entries.count { angleOf(pose, it, flip = false) != null }

    fun compare(reference: PersonPose, current: PersonPose, preferMirrored: Boolean = false): PoseMatch? {
        if (!isComparable(reference)) return null
        val preferred = match(reference, current, mirrored = preferMirrored)
        val alternative = match(reference, current, mirrored = !preferMirrored)
        return if (alternative.score > preferred.score + MIRROR_MARGIN) alternative else preferred
    }

    private fun match(reference: PersonPose, current: PersonPose, mirrored: Boolean): PoseMatch {
        val scores = BodyPart.entries.mapNotNull { part ->
            val refPart = if (mirrored) part.opposite() else part
            val refAngle = angleOf(reference, refPart, flip = mirrored) ?: return@mapNotNull null
            val currentAngle = angleOf(current, part, flip = false)
            part to (currentAngle?.let { scoreFor(angleDiff(it, refAngle)) } ?: 0f)
        }.toMap()
        return PoseMatch(scores.values.average().toFloat(), mirrored, scores)
    }

    private fun angleOf(pose: PersonPose, part: BodyPart, flip: Boolean): Double? {
        val from = pose.reliable(part.from, MIN_CONFIDENCE) ?: return null
        val to = pose.reliable(part.to, MIN_CONFIDENCE) ?: return null
        val dx = (to.x - from.x).toDouble() * pose.aspectRatio
        return atan2((to.y - from.y).toDouble(), if (flip) -dx else dx)
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
