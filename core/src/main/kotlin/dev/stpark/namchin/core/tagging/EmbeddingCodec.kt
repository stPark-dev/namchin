package dev.stpark.namchin.core.tagging

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Embedding ↔ 바이트 배열 (float32 리틀 엔디언). DB 저장용. */
object EmbeddingCodec {
    fun encode(embedding: Embedding): ByteArray {
        val buffer = ByteBuffer.allocate(embedding.values.size * Float.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN)
        embedding.values.forEach(buffer::putFloat)
        return buffer.array()
    }

    /** 길이가 float 단위가 아니면 null. */
    fun decode(bytes: ByteArray): Embedding? {
        if (bytes.size % Float.SIZE_BYTES != 0) return null
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        return Embedding(FloatArray(bytes.size / Float.SIZE_BYTES) { buffer.float })
    }
}
