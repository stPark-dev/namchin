package dev.stpark.namchin.core.live

import dev.stpark.namchin.core.model.PersonPose
import dev.stpark.namchin.core.model.Point

/** 0..1 정규화 영역. */
data class Region(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

/** 카메라 프레임 안에서 레퍼런스 비율로 실제 찍힐 영역을 잡고, 좌표를 그 영역 기준으로 바꾼다. */
object CropRegion {

    /** @param sourceAspect, targetAspect 가로/세로 */
    fun centerCrop(sourceAspect: Float, targetAspect: Float): Region =
        if (targetAspect > sourceAspect) {
            val h = sourceAspect / targetAspect
            Region(0f, (1 - h) / 2, 1f, (1 + h) / 2)
        } else {
            val w = targetAspect / sourceAspect
            Region((1 - w) / 2, 0f, (1 + w) / 2, 1f)
        }

    /** 영역 밖 관절은 최종 사진에서 잘리므로 신뢰도 0 (안 보이는 것으로) 처리한다. */
    fun remap(pose: PersonPose, region: Region): PersonPose = PersonPose(
        keypoints = pose.keypoints.mapValues { (_, k) ->
            val p = Point((k.position.x - region.left) / region.width, (k.position.y - region.top) / region.height)
            val inside = p.x in 0f..1f && p.y in 0f..1f
            k.copy(position = p, confidence = if (inside) k.confidence else 0f)
        },
        aspectRatio = pose.aspectRatio * region.width / region.height,
    )
}
