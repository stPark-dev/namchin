package dev.stpark.namchin.core.tagging

/**
 * 상세 화면에 보여줄 태그 제안.
 * 구도·시간은 그 사진에서 직접 판단한 값(stored)을 그대로 쓰고,
 * 장소·계절은 사용자가 확정한 비슷한 레퍼런스들에게서 배운다 (학습 없는 kNN).
 */
object TagSuggestion {
    private val LEARNABLE = (TagVocabulary.PLACE.tags + TagVocabulary.SEASON.tags).toSet()

    fun suggest(stored: Set<Tag>, embedding: Embedding?, confirmed: List<TaggedExample>): Set<Tag> {
        val learned = embedding
            ?.let { TagInference.suggest(it, confirmed, emptySet(), emptyMap()) }
            .orEmpty()
            .filter { it in LEARNABLE }
        return stored + learned
    }
}
