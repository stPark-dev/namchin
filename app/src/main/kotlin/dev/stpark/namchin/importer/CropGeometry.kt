package dev.stpark.namchin.importer

import dev.stpark.namchin.core.crop.CropRect
import kotlin.math.roundToInt

/** 0..1 정규화 사각형. */
data class NormRect(val left: Float, val top: Float, val right: Float, val bottom: Float)

/** 픽셀 사각형 (right/bottom 미포함). */
data class PixelRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
}

enum class Corner { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

/** 수동 크롭 UI용 계산. */
object CropGeometry {
    const val MIN_SIZE = 0.1f

    fun drag(rect: NormRect, corner: Corner, dx: Float, dy: Float): NormRect {
        val moveLeft = corner == Corner.TOP_LEFT || corner == Corner.BOTTOM_LEFT
        val moveTop = corner == Corner.TOP_LEFT || corner == Corner.TOP_RIGHT
        return rect.copy(
            left = if (moveLeft) (rect.left + dx).coerceIn(0f, rect.right - MIN_SIZE) else rect.left,
            right = if (!moveLeft) (rect.right + dx).coerceIn(rect.left + MIN_SIZE, 1f) else rect.right,
            top = if (moveTop) (rect.top + dy).coerceIn(0f, rect.bottom - MIN_SIZE) else rect.top,
            bottom = if (!moveTop) (rect.bottom + dy).coerceIn(rect.top + MIN_SIZE, 1f) else rect.bottom,
        )
    }

    fun fromDetector(rect: CropRect, gridWidth: Int, gridHeight: Int): NormRect = NormRect(
        rect.left.toFloat() / gridWidth,
        rect.top.toFloat() / gridHeight,
        rect.right.toFloat() / gridWidth,
        rect.bottom.toFloat() / gridHeight,
    )

    fun toPixels(rect: NormRect, width: Int, height: Int): PixelRect = PixelRect(
        (rect.left * width).roundToInt(),
        (rect.top * height).roundToInt(),
        (rect.right * width).roundToInt(),
        (rect.bottom * height).roundToInt(),
    )
}
