package dev.stpark.namchin.core.tagging

import kotlin.math.sqrt

data class Tag(val name: String)

/** 이미지 임베딩 벡터. */
class Embedding(val values: FloatArray) {
    fun cosine(other: Embedding): Float {
        require(values.size == other.values.size) { "임베딩 차원 불일치: ${values.size} vs ${other.values.size}" }
        var dot = 0.0
        var normA = 0.0
        var normB = 0.0
        for (i in values.indices) {
            dot += values[i] * other.values[i]
            normA += values[i] * values[i]
            normB += other.values[i] * other.values[i]
        }
        if (normA == 0.0 || normB == 0.0) return 0f
        return (dot / (sqrt(normA) * sqrt(normB))).toFloat()
    }
}

/** 사용자가 태그를 확정(수정)한 레퍼런스. */
class TaggedExample(val embedding: Embedding, val tags: Set<Tag>)

/**
 * 학습 없는 개인화: 확정된 비슷한 레퍼런스들의 태그를 유사도 가중 투표로 고르고,
 * 기기 내 기본 라벨을 매핑한 태그와 합쳐 제안한다.
 * 차원이 다른 임베딩(임베딩 모델 교체 전 데이터)은 건너뛴다.
 */
object TagInference {
    const val K = 5
    /** 기기 실측(MobileNet V3 Small): 비슷한 장면 0.40~0.66, 무관한 장면 약 0.2 → 0.35 */
    const val MIN_SIMILARITY = 0.35f
    const val VOTE_THRESHOLD = 0.5f

    fun suggest(
        query: Embedding,
        examples: List<TaggedExample>,
        baseLabels: Set<String>,
        labelMapping: Map<String, Tag>,
    ): Set<Tag> {
        val baseTags = baseLabels.mapNotNull { labelMapping[it] }.toSet()
        val neighbours = examples
            .filter { it.embedding.values.size == query.values.size }
            .map { it to query.cosine(it.embedding) }
            .filter { (_, sim) -> sim >= MIN_SIMILARITY }
            .sortedByDescending { (_, sim) -> sim }
            .take(K)
        if (neighbours.isEmpty()) return baseTags

        val totalWeight = neighbours.sumOf { (_, sim) -> sim.toDouble() }
        return neighbours
            .flatMap { (example, sim) -> example.tags.map { it to sim } }
            .groupBy({ it.first }, { it.second })
            .filterValues { sims -> sims.sum() / totalWeight >= VOTE_THRESHOLD }
            .keys + baseTags
    }
}
