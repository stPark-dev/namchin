package dev.stpark.namchin.data

import android.content.Context
import android.graphics.Bitmap
import dev.stpark.namchin.core.model.PersonPose
import dev.stpark.namchin.core.model.PoseCodec
import dev.stpark.namchin.core.reference.ReferenceProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class Reference(
    val id: Long,
    val imagePath: String,
    val width: Int,
    val height: Int,
    val pose: PersonPose?,
) {
    val aspectRatio: Float get() = width.toFloat() / height
    val profile: ReferenceProfile get() = ReferenceProfile.of(pose)
}

class ReferenceRepository(private val dao: ReferenceDao, private val imageDir: File) {

    fun observeAll(): Flow<List<Reference>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    /** 크롭된 이미지를 앱 저장소에 JPEG로 쓰고 DB에 등록한다. */
    suspend fun save(bitmap: Bitmap, pose: PersonPose?): Long = withContext(Dispatchers.IO) {
        imageDir.mkdirs()
        val file = File(imageDir, "${UUID.randomUUID()}.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        try {
            dao.insert(
                ReferenceEntity(
                    imagePath = file.absolutePath,
                    width = bitmap.width,
                    height = bitmap.height,
                    pose = pose?.let(PoseCodec::encode),
                    createdAt = System.currentTimeMillis(),
                ),
            )
        } catch (e: Exception) {
            file.delete()
            throw e
        }
    }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        dao.get(id)?.let { File(it.imagePath).delete() }
        dao.delete(id)
    }

    private fun ReferenceEntity.toDomain() =
        Reference(id, imagePath, width, height, pose?.let(PoseCodec::decode))

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
