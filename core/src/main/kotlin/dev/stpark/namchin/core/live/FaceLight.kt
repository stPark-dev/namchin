package dev.stpark.namchin.core.live

import dev.stpark.namchin.core.model.Joint
import dev.stpark.namchin.core.model.PersonPose
import dev.stpark.namchin.core.model.Point
import kotlin.math.abs

/**
 * 얼굴에 빛이 드는가. 인물 사진은 빛 방향이 먼저 — 얼굴이 화면보다 한참 어두우면(역광·그늘) 알려 준다.
 * 하늘이 아무리 밝아도 얼굴 자체가 충분히 밝으면 괜찮다고 본다.
 */
object FaceLight {
    private const val MIN_CONFIDENCE = 0.5f

    /** 얼굴 밝기 / 화면 밝기 */
    const val ENTER_RATIO = 0.6f
    const val EXIT_RATIO = 0.75f

    /** 얼굴 밝기(0..255)가 이 이상이면 비율과 상관없이 밝은 얼굴 */
    const val BRIGHT_ENTER = 100f
    const val BRIGHT_EXIT = 115f

    /** 반폭 = 귀 사이 거리 × 이 값. 얼굴 안쪽만 (역광일 때 머리 둘레의 밝은 하늘이 섞이지 않게) */
    private const val HALF_WIDTH_PER_EAR_SPAN = 0.6f
    private const val HALF_HEIGHT_PER_EAR_SPAN = 0.7f

    /** 정규화 좌표 사각형. */
    data class Box(val left: Float, val top: Float, val right: Float, val bottom: Float)

    fun dark(faceLuma: Float, frameLuma: Float, wasDark: Boolean): Boolean {
        if (frameLuma <= 0f) return false
        val ratio = if (wasDark) EXIT_RATIO else ENTER_RATIO
        val bright = if (wasDark) BRIGHT_EXIT else BRIGHT_ENTER
        return faceLuma < bright && faceLuma / frameLuma < ratio
    }

    /** 코를 가운데로 한 얼굴 안쪽 사각형 (pose와 같은 정규화 좌표). 크기는 귀 사이 거리 기준, 귀가 하나면 코~귀 × 2. */
    fun faceBox(pose: PersonPose): Box? {
        val nose = pose.reliable(Joint.NOSE, MIN_CONFIDENCE) ?: return null
        val left = pose.reliable(Joint.LEFT_EAR, MIN_CONFIDENCE)
        val right = pose.reliable(Joint.RIGHT_EAR, MIN_CONFIDENCE)
        val span = when {
            left != null && right != null -> abs(left.x - right.x)
            left != null -> 2 * abs(left.x - nose.x)
            right != null -> 2 * abs(right.x - nose.x)
            else -> return null
        }
        if (span <= 0f) return null
        val halfX = span * HALF_WIDTH_PER_EAR_SPAN
        val halfY = span * HALF_HEIGHT_PER_EAR_SPAN * pose.aspectRatio
        return Box(nose.x - halfX, nose.y - halfY, nose.x + halfX, nose.y + halfY)
    }

    /**
     * 세로로 세운 이미지의 정규화 좌표 → 센서 버퍼의 정규화 좌표.
     * @param rotationDegrees 버퍼를 세우려고 시계 방향으로 돌리는 각도 (CameraX rotationDegrees)
     */
    fun toBuffer(upright: Point, rotationDegrees: Int): Point = when (rotationDegrees) {
        90 -> Point(upright.y, 1f - upright.x)
        180 -> Point(1f - upright.x, 1f - upright.y)
        270 -> Point(1f - upright.y, upright.x)
        else -> upright
    }
}
