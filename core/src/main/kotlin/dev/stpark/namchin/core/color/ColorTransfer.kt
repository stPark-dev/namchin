package dev.stpark.namchin.core.color

import kotlin.math.sqrt

/** 사진 한 장의 Lab 채널별 평균·표준편차. */
class ColorStats(val mean: DoubleArray, val std: DoubleArray) {
    companion object {
        /** 표본이 많으면 듬성듬성 훑는다 (통계엔 충분). */
        private const val MAX_SAMPLES = 60_000

        fun of(pixels: IntArray): ColorStats {
            val step = maxOf(1, pixels.size / MAX_SAMPLES)
            val sum = DoubleArray(3)
            val sumSq = DoubleArray(3)
            var n = 0
            var i = 0
            while (i < pixels.size) {
                val lab = ColorLab.fromArgb(pixels[i])
                for (c in 0..2) {
                    sum[c] += lab[c]
                    sumSq[c] += lab[c] * lab[c]
                }
                n++
                i += step
            }
            val mean = DoubleArray(3) { sum[it] / n }
            val std = DoubleArray(3) { sqrt((sumSq[it] / n - mean[it] * mean[it]).coerceAtLeast(0.0)) }
            return ColorStats(mean, std)
        }
    }
}

/**
 * 색감 따라하기 (Reinhard 색 전이, Lab 공간):
 * 찍은 사진의 채널별 평균·퍼짐을 레퍼런스에 맞춘다.
 * 퍼짐 비율은 MIN_SCALE..MAX_SCALE로 제한해 피부톤·대비가 과하게 튀지 않게 한다.
 *
 * @param strength 0 = 원본, 1 = 레퍼런스 색감 그대로
 */
class ColorTransfer(
    private val source: ColorStats,
    private val reference: ColorStats,
    private val strength: Float = 1f,
) {
    val scale: DoubleArray = DoubleArray(3) { c ->
        if (source.std[c] < MIN_STD) 1.0 else (reference.std[c] / source.std[c]).coerceIn(MIN_SCALE, MAX_SCALE)
    }

    fun apply(argb: Int): Int {
        if (strength <= 0f) return argb
        val lab = ColorLab.fromArgb(argb)
        val out = DoubleArray(3) { c ->
            val moved = (lab[c] - source.mean[c]) * scale[c] + reference.mean[c]
            lab[c] + strength * (moved - lab[c])
        }
        return ColorLab.toArgb(out, (argb ushr 24) and 0xFF)
    }

    companion object {
        const val MIN_SCALE = 0.5
        const val MAX_SCALE = 2.0
        private const val MIN_STD = 1e-3
    }
}
