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
 * @param keepLightness true면 밝기(L)는 원본 그대로, 색(a·b)만 레퍼런스를 따른다
 *   (어두운 레퍼런스 때문에 사진까지 어두워지지 않게). 아주 밝거나 어둡거나 진한 색은 옮긴 색을
 *   다 담을 수 없어 잘리면 밝기가 바뀌므로, 담을 수 있는 만큼만 옮긴다.
 */
class ColorTransfer(
    private val source: ColorStats,
    private val reference: ColorStats,
    private val strength: Float = 1f,
    private val keepLightness: Boolean = false,
) {
    val scale: DoubleArray = DoubleArray(3) { c ->
        if (source.std[c] < MIN_STD) 1.0 else (reference.std[c] / source.std[c]).coerceIn(MIN_SCALE, MAX_SCALE)
    }

    fun apply(argb: Int): Int {
        if (strength <= 0f) return argb
        val lab = ColorLab.fromArgb(argb)
        val out = DoubleArray(3) { c ->
            if (c == L && keepLightness) return@DoubleArray lab[c]
            val moved = (lab[c] - source.mean[c]) * scale[c] + reference.mean[c]
            lab[c] + strength * (moved - lab[c])
        }
        if (keepLightness) fitInGamut(lab, out)
        return ColorLab.toArgb(out, (argb ushr 24) and 0xFF)
    }

    /**
     * 밝기는 그대로 두고, 색을 원본(항상 표현 가능)에서 목표 쪽으로 표현 가능한 데까지만 옮긴다.
     * 원본과 목표 사이를 이분 탐색한다.
     */
    private fun fitInGamut(original: DoubleArray, out: DoubleArray) {
        val l = out[L]
        if (ColorLab.inGamut(l, out[1], out[2])) return
        val da = out[1] - original[1]
        val db = out[2] - original[2]
        var lo = 0.0
        var hi = 1.0
        repeat(GAMUT_STEPS) {
            val mid = (lo + hi) / 2
            if (ColorLab.inGamut(l, original[1] + da * mid, original[2] + db * mid)) lo = mid else hi = mid
        }
        out[1] = original[1] + da * lo
        out[2] = original[2] + db * lo
    }

    companion object {
        const val MIN_SCALE = 0.5
        const val MAX_SCALE = 2.0
        private const val MIN_STD = 1e-3
        private const val L = 0
        private const val GAMUT_STEPS = 12
    }
}
