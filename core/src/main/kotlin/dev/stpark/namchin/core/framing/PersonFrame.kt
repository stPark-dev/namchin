package dev.stpark.namchin.core.framing

import dev.stpark.namchin.core.model.Joint
import dev.stpark.namchin.core.model.PersonPose
import kotlin.math.hypot

/**
 * 인물의 화면 위치와 크기. 팔다리 포즈의 영향을 받지 않도록 몸통만 쓴다.
 * 레퍼런스와 현재 프레임은 같은 비율의 영역(촬영될 크롭)에서 정규화된 좌표여야 한다.
 *
 * @property anchorX 어깨 중점 x (0..1)
 * @property anchorY 어깨 중점 y (0..1)
 * @property torso 어깨 중점 ~ 골반 중점 거리 (이미지 높이 단위)
 */
data class PersonFrame(val anchorX: Float, val anchorY: Float, val torso: Float) {

    companion object {
        const val MIN_CONFIDENCE = 0.5f

        fun from(pose: PersonPose): PersonFrame? {
            fun at(joint: Joint) = pose.reliable(joint, MIN_CONFIDENCE)
            val ls = at(Joint.LEFT_SHOULDER) ?: return null
            val rs = at(Joint.RIGHT_SHOULDER) ?: return null
            val lh = at(Joint.LEFT_HIP) ?: return null
            val rh = at(Joint.RIGHT_HIP) ?: return null
            val shoulderX = (ls.x + rs.x) / 2
            val shoulderY = (ls.y + rs.y) / 2
            val hipX = (lh.x + rh.x) / 2
            val hipY = (lh.y + rh.y) / 2
            val torso = hypot((hipX - shoulderX) * pose.aspectRatio, hipY - shoulderY)
            if (torso <= 0f) return null
            return PersonFrame(shoulderX, shoulderY, torso)
        }
    }
}
