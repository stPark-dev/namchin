package dev.stpark.namchin.core.tagging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TagSuggestionTest {

    private val autumn = Tag("가을")
    private val cafe = Tag("카페")
    private fun emb(vararg v: Float) = Embedding(v)

    @Test
    fun `stored auto tags are kept and similar confirmed places and seasons are added`() {
        val examples = listOf(TaggedExample(emb(1f, 0.1f), setOf(autumn, cafe, TagVocabulary.FULL_BODY)))

        val tags = TagSuggestion.suggest(setOf(TagVocabulary.DAY, TagVocabulary.UPPER_BODY), emb(1f, 0f), examples)

        // 구도·시간은 이 사진에서 직접 판단한 값을 쓰고, 이웃에게서는 장소·계절만 배운다
        assertEquals(setOf(TagVocabulary.DAY, TagVocabulary.UPPER_BODY, autumn, cafe), tags)
    }

    @Test
    fun `without an embedding only stored tags are used`() {
        val stored = setOf(TagVocabulary.DAY)

        assertEquals(stored, TagSuggestion.suggest(stored, null, listOf(TaggedExample(emb(1f), setOf(autumn)))))
    }
}
