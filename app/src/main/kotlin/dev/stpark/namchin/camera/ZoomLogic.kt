package dev.stpark.namchin.camera

import java.util.Locale
import kotlin.math.abs

/** 줌 배율 계산. */
object ZoomLogic {
    /** S23 울트라 렌즈 구성에 맞춘 버튼 (초광각, 기본, 3배, 10배 망원). */
    val PRESETS = listOf(0.6f, 1f, 3f, 10f)

    /** 현재 배율이 버튼 배율과 이 비율 이내면 그 버튼을 선택된 것으로 본다. */
    private const val SELECT_TOLERANCE = 0.25f

    fun presets(min: Float, max: Float): List<Float> = PRESETS.filter { it in min..max }

    fun pinch(current: Float, scale: Float, min: Float, max: Float): Float = (current * scale).coerceIn(min, max)

    fun selected(current: Float, presets: List<Float>): Float? =
        presets.filter { abs(current / it - 1f) <= SELECT_TOLERANCE }.minByOrNull { abs(current - it) }

    fun label(ratio: Float): String =
        String.format(Locale.US, "%.1f", ratio).removeSuffix(".0") + "×"
}
