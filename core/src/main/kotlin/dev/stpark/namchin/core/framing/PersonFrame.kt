package dev.stpark.namchin.core.framing

import dev.stpark.namchin.core.model.Joint
import dev.stpark.namchin.core.model.PersonPose
import dev.stpark.namchin.core.model.Point
import kotlin.math.hypot

/**
 * 인물의 화면 위치와 크기. 팔다리 포즈의 영향을 받지 않도록 머리·몸통만 쓴다.
 * 레퍼런스와 현재 프레임은 같은 비율의 영역(촬영될 크롭)에서 정규화된 좌표여야 한다.
 *
 * @property anchorX 어깨 중점 x (0..1)
 * @property anchorY 어깨 중점 y (0..1)
 * @property torso 어깨 중점 ~ 골반 중점 거리 (이미지 높이 단위). 골반이 안 보이면 null
 * @property neck 코 ~ 어깨 중점 거리 (이미지 높이 단위). 클로즈업용 대체 기준. 코가 안 보이면 null
 */
data class PersonFrame(val anchorX: Float, val anchorY: Float, val torso: Float?, val neck: Float?) {

    companion object {
        const val MIN_CONFIDENCE = 0.5f

        fun from(pose: PersonPose): PersonFrame? {
            fun at(joint: Joint) = pose.reliable(joint, MIN_CONFIDENCE)
            fun mid(a: Point?, b: Point?) = if (a != null && b != null) Point((a.x + b.x) / 2, (a.y + b.y) / 2) else null
            fun length(a: Point, b: Point?) =
                b?.let { hypot((it.x - a.x) * pose.aspectRatio, it.y - a.y) }?.takeIf { it > 0f }

            val shoulder = mid(at(Joint.LEFT_SHOULDER), at(Joint.RIGHT_SHOULDER)) ?: return null
            val torso = length(shoulder, mid(at(Joint.LEFT_HIP), at(Joint.RIGHT_HIP)))
            val neck = length(shoulder, at(Joint.NOSE))
            if (torso == null && neck == null) return null
            return PersonFrame(shoulder.x, shoulder.y, torso, neck)
        }
    }
}
