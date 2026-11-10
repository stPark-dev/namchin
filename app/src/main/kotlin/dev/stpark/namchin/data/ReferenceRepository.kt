package dev.stpark.namchin.data

import android.content.Context
import android.graphics.Bitmap
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
) {
    val aspectRatio: Float get() = width.toFloat() / height
    val profile: ReferenceProfile by lazy { ReferenceProfile.of(pose) }
}

class ReferenceRepository(private val dao: ReferenceDao, private val imageDir: File) {

    fun observeAll(): Flow<List<Reference>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }.flowOn(Dispatchers.Default)

    suspend fun get(id: Long): Reference? = withContext(Dispatchers.IO) { dao.get(id)?.toDomain() }

    /**
     * 크롭된 이미지를 앱 저장소에 JPEG로 쓰고 DB에 등록한다.
     * 호출한 화면이 사라져도 파일·DB가 어긋나지 않게 취소 불가로 끝까지 수행한다.
     */
    suspend fun save(bitmap: Bitmap, pose: PersonPose?): Long = withContext(Dispatchers.IO + NonCancellable) {
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
