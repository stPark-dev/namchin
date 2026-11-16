package dev.stpark.namchin.camera

import java.util.Locale
import kotlin.math.abs

/** 줌 배율 계산. */
object ZoomLogic {
    /** S23 울트라 렌즈 구성에 맞춘 버튼 (초광각, 기본, 2배(기본 렌즈 고화소 크롭), 3배, 10배 망원). */
    val PRESETS = listOf(0.6f, 1f, 2f, 3f, 10f)

    /** 확대 촬영 설정에서 고르는 기본 배율 (1배 = 끔). 인물은 2~3배가 왜곡이 적다 */
    val TELE_CHOICES = listOf(1f, 2f, 3f)

    /** 현재 배율이 버튼 배율과 이 비율 이내면 그 버튼을 선택된 것으로 본다. */
    private const val SELECT_TOLERANCE = 0.25f

    /** 인물사진(배경 흐림) 모드 배율. 삼성 인물사진처럼 1·2·3배만 (초광각 등에선 처리가 느려진다) */
    val PORTRAIT_PRESETS = listOf(1f, 2f, 3f)

    fun presets(min: Float, max: Float, portrait: Boolean = false): List<Float> =
        (if (portrait) PORTRAIT_PRESETS else PRESETS).filter { it in min..max }

    /** 인물사진 모드에서 쓸 배율: 1·2·3배 중 가장 가까운 것 */
    fun portraitZoom(ratio: Float): Float = PORTRAIT_PRESETS.minBy { abs(it - ratio) }

    fun pinch(current: Float, scale: Float, min: Float, max: Float): Float = (current * scale).coerceIn(min, max)

    fun selected(current: Float, presets: List<Float>): Float? =
        presets.filter { abs(current / it - 1f) <= SELECT_TOLERANCE }.minByOrNull { abs(current - it) }

    fun label(ratio: Float): String = number(ratio) + "×"

    /** "2", "2.4" — 문장 속 "N배" 용 */
    fun number(ratio: Float): String = String.format(Locale.US, "%.1f", ratio).removeSuffix(".0")
}
