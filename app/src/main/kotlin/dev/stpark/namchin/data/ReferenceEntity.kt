package dev.stpark.namchin.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * @property imagePath 앱 내부 저장소의 크롭된 레퍼런스 이미지
 * @property pose PoseCodec으로 인코딩한 포즈. 인물 미검출이면 null
 * @property embedding 장면 임베딩 (EmbeddingCodec). 아직 계산 전이면 null
 * @property tags 쉼표로 이은 태그 이름. 아직 없으면 null
 * @property tagsConfirmed 사용자가 태그를 직접 확인·수정했는지 (태그 추론의 정답 예시가 된다)
 */
@Entity(tableName = "reference")
data class ReferenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val imagePath: String,
    val width: Int,
    val height: Int,
    val pose: String?,
    val createdAt: Long,
    val embedding: ByteArray? = null,
    val tags: String? = null,
    @ColumnInfo(defaultValue = "0") val tagsConfirmed: Boolean = false,
)
