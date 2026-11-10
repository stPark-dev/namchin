package dev.stpark.namchin.ml

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.imageembedder.ImageEmbedder
import dev.stpark.namchin.core.tagging.Embedding

/**
 * 이미지 → 장면 임베딩 (MobileNet V3 Small, 기기 안에서 처리).
 * 비슷한 배경의 레퍼런스를 찾고 태그를 추론하는 데 쓴다. 앱 전체에서 하나만 둔다.
 */
class SceneEmbedder private constructor(context: Context) {
    private val embedder = ImageEmbedder.createFromOptions(
        context,
        ImageEmbedder.ImageEmbedderOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetPath(MODEL).build())
            .setL2Normalize(true)
            .build(),
    )

    /** 느린 호출이므로 메인 스레드가 아닌 곳에서 부른다. */
    @Synchronized
    fun embed(bitmap: Bitmap): Embedding? {
        val input = if (bitmap.config == Bitmap.Config.ARGB_8888) bitmap else bitmap.copy(Bitmap.Config.ARGB_8888, false)
        val result = embedder.embed(BitmapImageBuilder(input).build())
        return result.embeddingResult().embeddings().firstOrNull()?.floatEmbedding()?.let { Embedding(it) }
    }

    companion object {
        private const val MODEL = "mobilenet_v3_small.tflite"

        @Volatile private var instance: SceneEmbedder? = null

        fun get(context: Context): SceneEmbedder = instance ?: synchronized(this) {
            instance ?: SceneEmbedder(context.applicationContext).also { instance = it }
        }
    }
}
