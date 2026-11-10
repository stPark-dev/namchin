package dev.stpark.namchin.ml

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import dev.stpark.namchin.core.model.PersonPose
import dev.stpark.namchin.core.tagging.AutoTags
import dev.stpark.namchin.core.tagging.Tag
import dev.stpark.namchin.image.BitmapLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/** 사진 한 장 → 자동 태그 (구도: 포즈, 시간: 밝기, 장소: ML Kit 라벨). 앱 전체에서 하나. */
object AutoTagger {
    private const val LABEL_CONFIDENCE = 0.5f
    private val labeler by lazy {
        ImageLabeling.getClient(ImageLabelerOptions.Builder().setConfidenceThreshold(LABEL_CONFIDENCE).build())
    }

    suspend fun tags(bitmap: Bitmap, pose: PersonPose?): Set<Tag> {
        val labels = labeler.process(InputImage.fromBitmap(bitmap, 0)).await().map { it.text }
        val time = withContext(Dispatchers.Default) { AutoTags.time(BitmapLoader.toLuma(bitmap)) }
        return AutoTags.framing(pose) + time + AutoTags.place(labels)
    }
}
