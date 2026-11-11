package dev.stpark.namchin.core.color

import kotlin.math.abs
import kotlin.math.pow

/** 그림자·밝은 곳에 더할 색 (각 채널 0..1 단위, 보통 0.2 이하). */
data class Tint(val r: Float, val g: Float, val b: Float)

/**
 * 감성 필터 한 가지의 조리법. 모든 값 0 = 원본 그대로.
 * 순서: 노출 → 색온도·색조 → 대비 → 채도 → 그림자/밝은 곳 색 → 바랜 느낌 → 피부 보호.
 *
 * @property exposure 밝기 (스톱, 0.5 = 1.4배)
 * @property contrast -1..1 (양수 = 진하게)
 * @property saturation -1..1 (-1 = 흑백)
 * @property warmth -1..1 (양수 = 따뜻한 노랑·빨강, 음수 = 차가운 파랑)
 * @property tint -1..1 (양수 = 분홍, 음수 = 초록)
 * @property fade 0..1 검은색을 띄워 필름처럼 바랜 느낌
 * @property skinProtect 피부색은 효과를 덜 받게 (얼굴이 창백·노랗게 되지 않게)
 */
data class FilterRecipe(
    val exposure: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val warmth: Float = 0f,
    val tint: Float = 0f,
    val fade: Float = 0f,
    val shadowTint: Tint? = null,
    val highlightTint: Tint? = null,
    val skinProtect: Boolean = false,
) {
    fun apply(argb: Int): Int {
        val r0 = ((argb shr 16) and 0xFF) / 255f
        val g0 = ((argb shr 8) and 0xFF) / 255f
        val b0 = (argb and 0xFF) / 255f
        val gain = 2f.pow(exposure)
        var r = r0 * gain + warmth * WARMTH_SCALE
        var g = g0 * gain - tint * TINT_SCALE
        var b = b0 * gain - warmth * WARMTH_SCALE
        r = curve(r.coerceIn(0f, 1f))
        g = curve(g.coerceIn(0f, 1f))
        b = curve(b.coerceIn(0f, 1f))
        val luma = LUMA_R * r + LUMA_G * g + LUMA_B * b
        val keep = 1f + saturation
        r = luma + (r - luma) * keep
        g = luma + (g - luma) * keep
        b = luma + (b - luma) * keep
        shadowTint?.let { t ->
            val w = (1f - luma).pow(2)
            r += t.r * w; g += t.g * w; b += t.b * w
        }
        highlightTint?.let { t ->
            val w = luma.pow(2)
            r += t.r * w; g += t.g * w; b += t.b * w
        }
        val lift = fade * FADE_SCALE
        r = lift + r.coerceIn(0f, 1f) * (1f - lift)
        g = lift + g.coerceIn(0f, 1f) * (1f - lift)
        b = lift + b.coerceIn(0f, 1f) * (1f - lift)
        if (skinProtect) {
            // 색(밝기를 뺀 차이)만 원본 쪽으로 되돌리고, 밝기는 필터대로 둔다 (얼굴만 어두워 보이지 않게)
            val keepOriginal = skinWeight(r0, g0, b0) * SKIN_KEEP
            val luma0 = LUMA_R * r0 + LUMA_G * g0 + LUMA_B * b0
            val lumaNow = LUMA_R * r + LUMA_G * g + LUMA_B * b
            r += ((r0 - luma0) - (r - lumaNow)) * keepOriginal
            g += ((g0 - luma0) - (g - lumaNow)) * keepOriginal
            b += ((b0 - luma0) - (b - lumaNow)) * keepOriginal
            r = r.coerceIn(0f, 1f)
            g = g.coerceIn(0f, 1f)
            b = b.coerceIn(0f, 1f)
        }
        return (argb and ALPHA) or (channel(r) shl 16) or (channel(g) shl 8) or channel(b)
    }

    /** 0.5를 기준으로 S자(양수) 또는 완만하게(음수). */
    private fun curve(x: Float): Float {
        if (contrast == 0f) return x
        val s = x * x * (3f - 2f * x)
        return x + (s - x) * contrast
    }

    private fun channel(v: Float): Int = (v * 255f + 0.5f).toInt().coerceIn(0, 255)

    companion object {
        private const val ALPHA = 0xFF shl 24
        private const val WARMTH_SCALE = 0.12f
        private const val TINT_SCALE = 0.08f
        private const val FADE_SCALE = 0.16f
        private const val SKIN_KEEP = 0.6f
        private const val LUMA_R = 0.299f
        private const val LUMA_G = 0.587f
        private const val LUMA_B = 0.114f

        /**
         * 피부색일수록 1: 빨강 > 초록 > 파랑, 색상 0~50도, 적당한 채도와 밝기.
         * 경계에서 갑자기 바뀌지 않게 부드럽게 줄어든다.
         */
        fun skinWeight(r: Float, g: Float, b: Float): Float {
            val max = maxOf(r, g, b)
            val min = minOf(r, g, b)
            if (max != r || g < b) return 0f
            val chroma = max - min
            if (chroma <= 0f) return 0f
            val hue = 60f * (g - b) / chroma // 0 = 빨강, 60 = 노랑
            val sat = chroma / max
            val hueWeight = 1f - (abs(hue - 25f) / 25f).coerceIn(0f, 1f)
            val satWeight = 1f - (abs(sat - 0.35f) / 0.3f).coerceIn(0f, 1f)
            // 아주 어두운 곳은 피부인지 알 수 없으므로 서서히 줄인다 (경계 줄무늬가 생기지 않게)
            val lightWeight = ((max - 0.1f) / 0.15f).coerceIn(0f, 1f)
            return hueWeight * satWeight * lightWeight
        }
    }
}

/** 앱에 들어 있는 감성 필터. 이름은 화면에 보이는 한글 이름과 짝지어 쓴다. */
enum class Filter(val recipe: FilterRecipe) {
    /** 맑음: 살짝 밝고 깨끗하게 */
    CLEAR(FilterRecipe(exposure = 0.15f, contrast = 0.1f, saturation = 0.1f, warmth = -0.05f, skinProtect = true)),

    /** 포근: 따뜻하고 부드럽게 바랜 */
    COZY(
        FilterRecipe(
            exposure = 0.05f, contrast = -0.15f, saturation = -0.05f, warmth = 0.3f, fade = 0.3f,
            highlightTint = Tint(0.04f, 0.02f, 0f), skinProtect = true,
        ),
    ),

    /** 필름: 바랜 검정, 그림자 청록 + 밝은 곳 노랑 */
    FILM(
        FilterRecipe(
            contrast = 0.15f, saturation = -0.15f, fade = 0.45f,
            shadowTint = Tint(0f, 0.04f, 0.06f), highlightTint = Tint(0.06f, 0.04f, 0f),
        ),
    ),

    /** 노을: 주황·분홍 */
    SUNSET(
        FilterRecipe(
            saturation = 0.15f, warmth = 0.4f, tint = 0.15f,
            highlightTint = Tint(0.08f, 0.02f, 0f), skinProtect = true,
        ),
    ),

    /** 청량: 시원한 파랑, 밝게 */
    FRESH(
        FilterRecipe(
            exposure = 0.1f, contrast = 0.1f, saturation = 0.15f, warmth = -0.3f,
            shadowTint = Tint(0f, 0.02f, 0.06f), skinProtect = true,
        ),
    ),

    /** 밤거리: 진하게, 그림자 청록 + 불빛 주황 */
    NIGHT(
        FilterRecipe(
            contrast = 0.25f, saturation = 0.2f, warmth = -0.1f,
            shadowTint = Tint(0f, 0.05f, 0.08f), highlightTint = Tint(0.1f, 0.04f, 0f), skinProtect = true,
        ),
    ),

    /** 흑백 */
    MONO(FilterRecipe(contrast = 0.2f, saturation = -1f)),

    /** 빈티지: 많이 바래고 누렇게 */
    VINTAGE(FilterRecipe(contrast = -0.1f, saturation = -0.3f, warmth = 0.2f, tint = -0.08f, fade = 0.55f));

    fun lut(): ColorLut = ColorLut.build(recipe::apply)
}
