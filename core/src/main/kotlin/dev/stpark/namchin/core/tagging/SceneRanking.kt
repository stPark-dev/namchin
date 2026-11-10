package dev.stpark.namchin.core.tagging

/** 지금 카메라 장면과 비슷한 레퍼런스 순서. 카드 손패 앞쪽에 둘 순서를 정한다. */
object SceneRanking {
    /** 직전 1등은 새 1등보다 이만큼 이내로만 뒤지면 자리를 지킨다 (카드가 흔들리지 않게). */
    const val KEEP_TOP_MARGIN = 0.05f

    /**
     * @param query 지금 장면 임베딩. null이면 원래 순서 그대로
     * @param items (레퍼런스 id, 임베딩). 임베딩이 없거나 차원이 다르면 맨 뒤(원래 순서 유지)
     */
    fun rank(query: Embedding?, items: List<Pair<Long, Embedding?>>, previousTop: Long?): List<Long> {
        if (query == null) return items.map { it.first }
        val scored = items.mapNotNull { (id, emb) ->
            emb?.takeIf { it.values.size == query.values.size }?.let { id to query.cosine(it) }
        }.sortedByDescending { it.second }
        val unscored = items.map { it.first }.filter { id -> scored.none { it.first == id } }
        val best = scored.firstOrNull()?.second
        val keep = scored.firstOrNull { it.first == previousTop }
            ?.takeIf { best != null && it.second >= best - KEEP_TOP_MARGIN }
        val ordered = if (keep != null) listOf(keep) + (scored - keep) else scored
        return ordered.map { it.first } + unscored
    }
}
