package dev.stpark.namchin.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import dev.stpark.namchin.core.tagging.Embedding
import dev.stpark.namchin.core.tagging.EmbeddingCodec
import dev.stpark.namchin.core.tagging.Tag
import dev.stpark.namchin.core.tagging.TagCodec
import dev.stpark.namchin.core.tagging.TagSuggestion
import dev.stpark.namchin.core.tagging.TaggedExample
import dev.stpark.namchin.core.model.PersonPose
import dev.stpark.namchin.core.model.PoseCodec
import dev.stpark.namchin.core.reference.ReferenceProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

data class Reference(
    val id: Long,
    val imagePath: String,
    val width: Int,
    val height: Int,
    val pose: PersonPose?,
    val embedding: Embedding? = null,
    val tags: Set<Tag> = emptySet(),
    val tagsConfirmed: Boolean = false,
) {
    val aspectRatio: Float get() = width.toFloat() / height
    val profile: ReferenceProfile by lazy { ReferenceProfile.of(pose) }

    /**
     * 화면에 보여줄 태그: 확정했으면 그대로, 아니면 자동 태그 + 확정된 비슷한 레퍼런스에게서 배운 장소·계절.
     */
    fun displayTags(all: List<Reference>): Set<Tag> = if (tagsConfirmed) {
        tags
    } else {
        val examples = all.filter { it.tagsConfirmed && it.id != id && it.embedding != null }
            .map { TaggedExample(it.embedding!!, it.tags) }
        TagSuggestion.suggest(tags, embedding, examples)
    }
}

class ReferenceRepository(private val dao: ReferenceDao, private val imageDir: File) {

    fun observeAll(): Flow<List<Reference>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }.flowOn(Dispatchers.Default)

    /**
     * 크롭된 이미지를 앱 저장소에 JPEG로 쓰고 DB에 등록한다.
     * 호출한 화면이 사라져도 파일·DB가 어긋나지 않게 취소 불가로 끝까지 수행한다.
     */
    suspend fun save(bitmap: Bitmap, pose: PersonPose?, embedding: Embedding?, tags: Set<Tag>?): Long =
        withContext(Dispatchers.IO + NonCancellable) {
        imageDir.mkdirs()
        val file = File(imageDir, "${UUID.randomUUID()}.jpg")
        try {
            val written = file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
            if (!written) throw IOException("JPEG 인코딩 실패: $file")
            dao.insert(
                ReferenceEntity(
                    imagePath = file.absolutePath,
                    width = bitmap.width,
                    height = bitmap.height,
                    pose = pose?.let(PoseCodec::encode),
                    createdAt = System.currentTimeMillis(),
                    embedding = embedding?.let(EmbeddingCodec::encode),
                    tags = tags?.let(TagCodec::encode),
                ),
            )
        } catch (e: Exception) {
            file.delete()
            throw e
        }
    }

    /** 행을 먼저 지운다. 중간에 실패해도 남는 것은 보이지 않는 고아 파일뿐이다. */
    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        val entity = dao.get(id) ?: return@withContext
        dao.delete(id)
        File(entity.imagePath).delete()
    }

    /** 임베딩이 없는 레퍼런스(이전 버전에서 저장한 것)를 채운다. 채운 개수를 돌려준다. */
    suspend fun backfillEmbeddings(embed: (Bitmap) -> Embedding?): Int = withContext(Dispatchers.IO) {
        dao.withoutEmbedding().count { entity ->
            val bitmap = BitmapFactory.decodeFile(entity.imagePath, BitmapFactory.Options().apply { inSampleSize = 4 })
                ?: return@count false
            val embedding = embed(bitmap) ?: return@count false
            dao.updateEmbedding(entity.id, EmbeddingCodec.encode(embedding))
            true
        }
    }

    /** 자동 태그가 없는 레퍼런스(이전 버전에서 저장한 것)를 채운다. 채운 개수를 돌려준다. */
    suspend fun backfillTags(autoTag: suspend (Bitmap, PersonPose?) -> Set<Tag>): Int {
        val missing = withContext(Dispatchers.IO) { dao.withoutTags() }
        return missing.count { entity ->
            val bitmap = withContext(Dispatchers.IO) {
                BitmapFactory.decodeFile(entity.imagePath, BitmapFactory.Options().apply { inSampleSize = 2 })
            } ?: return@count false
            val tags = autoTag(bitmap, entity.pose?.let(PoseCodec::decode))
            withContext(Dispatchers.IO) { dao.updateTags(entity.id, TagCodec.encode(tags), false) }
            true
        }
    }

    suspend fun updateTags(id: Long, tags: Set<Tag>, confirmed: Boolean) = withContext(Dispatchers.IO) {
        dao.updateTags(id, TagCodec.encode(tags), confirmed)
    }

    private fun ReferenceEntity.toDomain() = Reference(
        id, imagePath, width, height,
        pose = pose?.let(PoseCodec::decode),
        embedding = embedding?.let(EmbeddingCodec::decode),
        tags = tags?.let(TagCodec::decode).orEmpty(),
        tagsConfirmed = tagsConfirmed,
    )

    companion object {
        private const val JPEG_QUALITY = 92

        @Volatile private var instance: ReferenceRepository? = null

        fun get(context: Context): ReferenceRepository = instance ?: synchronized(this) {
            instance ?: ReferenceRepository(
                AppDatabase.get(context).referenceDao(),
                File(context.filesDir, "references"),
            ).also { instance = it }
        }
    }
}
