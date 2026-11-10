package dev.stpark.namchin.core.tagging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SceneRankingTest {

    private fun emb(vararg v: Float) = Embedding(v)

    private val items = listOf(
        1L to emb(1f, 0f),
        2L to emb(0f, 1f),
        3L to null,
        4L to emb(0.7f, 0.7f),
    )

    @Test
    fun `without a scene the original order is kept`() {
        assertEquals(listOf(1L, 2L, 3L, 4L), SceneRanking.rank(null, items, previousTop = null))
    }

    @Test
    fun `most similar first, references without embeddings last`() {
        assertEquals(listOf(2L, 4L, 1L, 3L), SceneRanking.rank(emb(0f, 1f), items, previousTop = null))
    }

    @Test
    fun `previous top keeps its place when the new best is only slightly better`() {
        // 1과 4가 거의 같은 유사도 → 직전 1등(4)을 유지해 카드가 흔들리지 않게
        val query = emb(1f, 0.38f)

        assertEquals(4L, SceneRanking.rank(query, items, previousTop = 4L).first())
        assertEquals(1L, SceneRanking.rank(query, items, previousTop = null).first())
    }

    @Test
    fun `previous top gives way when clearly beaten`() {
        assertEquals(2L, SceneRanking.rank(emb(0f, 1f), items, previousTop = 1L).first())
    }

    @Test
    fun `embeddings of a different size are treated as missing`() {
        val mixed = listOf(1L to emb(1f, 0f, 0f), 2L to emb(0f, 1f))

        assertEquals(listOf(2L, 1L), SceneRanking.rank(emb(0f, 1f), mixed, previousTop = null))
    }
}
