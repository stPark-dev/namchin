package dev.stpark.namchin.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * @property imagePath 앱 내부 저장소의 크롭된 레퍼런스 이미지
 * @property pose PoseCodec으로 인코딩한 포즈. 인물 미검출이면 null
 */
@Entity(tableName = "reference")
data class ReferenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val imagePath: String,
    val width: Int,
    val height: Int,
    val pose: String?,
    val createdAt: Long,
)
