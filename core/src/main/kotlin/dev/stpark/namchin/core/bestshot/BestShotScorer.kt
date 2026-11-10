package dev.stpark.namchin.core.bestshot

/**
 * @property poseScore 레퍼런스와의 포즈 일치도 0..1 (없으면 null)
 * @property eyesOpen 두 눈 중 더 감긴 쪽의 뜸 확률 0..1 (얼굴 미검출 시 null)
 * @property sharpness 선명도 원시값 (클수록 선명, 같은 연사 묶음 안에서 상대 비교)
 */
data class ShotCandidate(
    val id: String,
    val poseScore: Float?,
    val eyesOpen: Float?,
    val sharpness: Float,
)

data class RankedShot(val candidate: ShotCandidate, val score: Float)

/** 연사 묶음에서 베스트 컷 순위를 매긴다. 눈 감은 컷은 제외. */
object BestShotScorer {
    const val EYES_CLOSED_THRESHOLD = 0.3f
    const val NEUTRAL_EYES = 0.5f
    private const val POSE_WEIGHT = 0.5f
    private const val EYES_WEIGHT = 0.3f
    private const val SHARPNESS_WEIGHT = 0.2f

    fun rank(shots: List<ShotCandidate>): List<RankedShot> {
        val maxSharpness = shots.maxOfOrNull { it.sharpness } ?: return emptyList()
        return shots
            .filter { (it.eyesOpen ?: NEUTRAL_EYES) >= EYES_CLOSED_THRESHOLD }
            .map { RankedShot(it, scoreOf(it, maxSharpness)) }
            .sortedByDescending { it.score }
    }

    private fun scoreOf(shot: ShotCandidate, maxSharpness: Float): Float {
        val sharpness = if (maxSharpness > 0f) shot.sharpness / maxSharpness else 0f
        return POSE_WEIGHT * (shot.poseScore ?: 0f) +
            EYES_WEIGHT * (shot.eyesOpen ?: NEUTRAL_EYES) +
            SHARPNESS_WEIGHT * sharpness
    }
}
