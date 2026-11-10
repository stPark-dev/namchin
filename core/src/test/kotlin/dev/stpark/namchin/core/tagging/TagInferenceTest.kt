package dev.stpark.namchin.core.tagging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TagInferenceTest {

    private val cafe = Tag("카페")
    private val night = Tag("야경")
    private val autumn = Tag("가을")
    private val mapping = mapOf("Coffee" to cafe, "Night" to night)

    private fun emb(vararg v: Float) = Embedding(v)

    @Test
    fun `cosine similarity basics`() {
        assertEquals(1f, emb(1f, 2f).cosine(emb(2f, 4f)), 1e-5f)
        assertEquals(0f, emb(1f, 0f).cosine(emb(0f, 1f)), 1e-5f)
        assertEquals(0f, emb(0f, 0f).cosine(emb(1f, 1f)), 1e-5f)
        assertThrows<IllegalArgumentException> { emb(1f).cosine(emb(1f, 2f)) }
    }

    @Test
    fun `without examples falls back to mapped base labels`() {
        val tags = TagInference.suggest(emb(1f, 0f), emptyList(), setOf("Coffee", "Table"), mapping)

        assertEquals(setOf(cafe), tags)
    }

    @Test
    fun `neighbour votes are combined with mapped base labels`() {
        val examples = listOf(TaggedExample(emb(1f, 0.1f), setOf(autumn)))

        val tags = TagInference.suggest(emb(1f, 0f), examples, setOf("Coffee"), mapping)

        assertEquals(setOf(autumn, cafe), tags)
    }

    @Test
    fun `examples from a different embedding model are skipped`() {
        val stale = listOf(TaggedExample(emb(1f, 0f, 0f), setOf(autumn)))

        val tags = TagInference.suggest(emb(1f, 0f), stale, setOf("Night"), mapping)

        assertEquals(setOf(night), tags)
    }

    @Test
    fun `dissimilar examples are ignored`() {
        val examples = listOf(TaggedExample(emb(0f, 1f), setOf(autumn)))

        val tags = TagInference.suggest(emb(1f, 0f), examples, setOf("Night"), mapping)

        assertEquals(setOf(night), tags)
    }

    @Test
    fun `tags need a weighted majority of neighbours`() {
        val examples = listOf(
            TaggedExample(emb(1f, 0.05f), setOf(autumn, night)),
            TaggedExample(emb(1f, 0.1f), setOf(autumn)),
            TaggedExample(emb(1f, 0.15f), setOf(autumn, cafe)),
        )

        val tags = TagInference.suggest(emb(1f, 0f), examples, emptySet(), mapping)

        assertEquals(setOf(autumn), tags)
    }

    @Test
    fun `only the K nearest examples vote`() {
        val near = List(TagInference.K) { TaggedExample(emb(1f, 0.01f * it), setOf(autumn)) }
        val farther = List(TagInference.K + 1) { TaggedExample(emb(1f, 0.5f), setOf(cafe)) }

        val tags = TagInference.suggest(emb(1f, 0f), farther + near, emptySet(), mapping)

        assertEquals(setOf(autumn), tags)
    }

    @Test
    fun `moderately similar scenes count as neighbours`() {
        // 실제 기기 임베딩(MobileNet V3 Small)에서 비슷한 장면끼리 유사도 0.40~0.66, 무관한 장면 약 0.2
        val moderate = Embedding(floatArrayOf(0.4f, 0.9165f)) // (1,0)과 유사도 0.4
        val examples = listOf(TaggedExample(moderate, setOf(autumn)))

        assertEquals(setOf(autumn), TagInference.suggest(emb(1f, 0f), examples, emptySet(), mapping))
    }
}
