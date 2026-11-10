package dev.stpark.namchin.core.color

import kotlin.math.roundToInt

/**
 * 3D 색 변환표. 1200만 화소에 Lab 변환을 하나하나 하면 느리므로
 * SIZE³ 격자점만 미리 계산하고 픽셀은 삼선형 보간으로 찾는다.
 * 픽셀마다 불리므로 할당 없이 채널별 배열에서 바로 읽는다.
 */
class ColorLut private constructor(
    private val red: FloatArray,
    private val green: FloatArray,
    private val blue: FloatArray,
) {

    fun apply(argb: Int): Int = apply(argb, 1f)

    /**
     * 강도 조절: 표는 100%로 한 번만 만들고, 원본과 결과를 섞는다 (슬라이더를 움직여도 표를 다시 만들지 않게).
     * @param strength 0 = 원본, 1 = 레퍼런스 색감 그대로
     */
    fun apply(argb: Int, strength: Float): Int {
        if (strength <= 0f) return argb
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        val fr = r * STEP
        val fg = g * STEP
        val fb = b * STEP
        val r0 = minOf(fr.toInt(), SIZE - 2)
        val g0 = minOf(fg.toInt(), SIZE - 2)
        val b0 = minOf(fb.toInt(), SIZE - 2)
        val dr = fr - r0
        val dg = fg - g0
        val db = fb - b0
        val i000 = (r0 * SIZE + g0) * SIZE + b0
        val nr = interpolate(red, i000, dr, dg, db)
        val ng = interpolate(green, i000, dr, dg, db)
        val nb = interpolate(blue, i000, dr, dg, db)
        val outR: Float
        val outG: Float
        val outB: Float
        if (strength >= 1f) {
            outR = nr
            outG = ng
            outB = nb
        } else {
            outR = r + (nr - r) * strength
            outG = g + (ng - g) * strength
            outB = b + (nb - b) * strength
        }
        return (argb and ALPHA) or (channel(outR) shl 16) or (channel(outG) shl 8) or channel(outB)
    }

    /** 배열 일부를 제자리에서 변환. 여러 스레드가 서로 다른 구간을 나눠 맡을 수 있다. */
    fun applyInPlace(pixels: IntArray, strength: Float = 1f, from: Int = 0, to: Int = pixels.size) {
        for (i in from until to) pixels[i] = apply(pixels[i], strength)
    }

    private fun interpolate(t: FloatArray, i000: Int, dr: Float, dg: Float, db: Float): Float {
        val c00 = t[i000] + (t[i000 + 1] - t[i000]) * db
        val c01 = t[i000 + SIZE] + (t[i000 + SIZE + 1] - t[i000 + SIZE]) * db
        val c10 = t[i000 + PLANE] + (t[i000 + PLANE + 1] - t[i000 + PLANE]) * db
        val c11 = t[i000 + PLANE + SIZE] + (t[i000 + PLANE + SIZE + 1] - t[i000 + PLANE + SIZE]) * db
        val c0 = c00 + (c01 - c00) * dg
        val c1 = c10 + (c11 - c10) * dg
        return c0 + (c1 - c0) * dr
    }

    private fun channel(v: Float): Int = (v + 0.5f).toInt().coerceIn(0, 255)

    companion object {
        const val SIZE = 65
        private const val PLANE = SIZE * SIZE
        private const val STEP = (SIZE - 1) / 255f
        private const val ALPHA = 0xFF shl 24

        fun build(transform: (Int) -> Int): ColorLut {
            val n = SIZE * SIZE * SIZE
            val red = FloatArray(n)
            val green = FloatArray(n)
            val blue = FloatArray(n)
            for (r in 0 until SIZE) for (g in 0 until SIZE) for (b in 0 until SIZE) {
                fun level(i: Int) = (i * 255f / (SIZE - 1)).roundToInt()
                val out = transform(ALPHA or (level(r) shl 16) or (level(g) shl 8) or level(b))
                val i = (r * SIZE + g) * SIZE + b
                red[i] = ((out shr 16) and 0xFF).toFloat()
                green[i] = ((out shr 8) and 0xFF).toFloat()
                blue[i] = (out and 0xFF).toFloat()
            }
            return ColorLut(red, green, blue)
        }
    }
}
