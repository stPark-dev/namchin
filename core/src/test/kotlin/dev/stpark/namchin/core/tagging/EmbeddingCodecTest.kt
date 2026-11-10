package dev.stpark.namchin.core.tagging

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class EmbeddingCodecTest {

    @Test
    fun `round trips float values exactly`() {
        val values = floatArrayOf(0f, 1.5f, -2.25f, Float.MIN_VALUE, 1e-7f)

        assertArrayEquals(values, EmbeddingCodec.decode(EmbeddingCodec.encode(Embedding(values)))!!.values)
    }

    @Test
    fun `round trips an empty embedding`() {
        assertArrayEquals(floatArrayOf(), EmbeddingCodec.decode(EmbeddingCodec.encode(Embedding(floatArrayOf())))!!.values)
    }

    @Test
    fun `bytes that are not whole floats are rejected`() {
        assertNull(EmbeddingCodec.decode(ByteArray(5)))
    }
}
