package dev.stpark.namchin.core.bestshot

/**
 * 결과 화면의 한 장.
 * @property score 베스트 컷 점수. 눈 감아서 순위에서 빠졌으면 null
 */
data class ReviewItem(val id: String, val score: Float?, val best: Boolean, val eyesClosed: Boolean)

/** 연사 결과를 찍은 순서대로 두고, 베스트 컷 하나와 눈 감은 컷을 표시한다. */
object ShotReview {
    fun of(shots: List<ShotCandidate>): List<ReviewItem> {
        val ranked = BestShotScorer.rank(shots)
        val scores = ranked.associate { it.candidate.id to it.score }
        val bestId = ranked.firstOrNull()?.candidate?.id
        return shots.map { shot ->
            ReviewItem(
                id = shot.id,
                score = scores[shot.id],
                best = shot.id == bestId,
                eyesClosed = (shot.eyesOpen ?: BestShotScorer.NEUTRAL_EYES) < BestShotScorer.EYES_CLOSED_THRESHOLD,
            )
        }
    }

    /**
     * 분석에 실패한 컷도 찍은 순서 자리에 두되, 점수·베스트 후보에서는 뺀다.
     * 전부 실패했으면 베스트가 없다 (근거 없이 지우지 않게).
     */
    fun merge(ids: List<String>, analyzed: List<ShotCandidate>): List<ReviewItem> {
        val reviewed = of(analyzed).associateBy { it.id }
        return ids.map { id -> reviewed[id] ?: ReviewItem(id, score = null, best = false, eyesClosed = false) }
    }
}
