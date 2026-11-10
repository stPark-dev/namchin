package dev.stpark.namchin.core.color

import kotlin.math.cbrt
import kotlin.math.pow
import kotlin.math.roundToInt

/** sRGB(ARGB Int) ↔ CIE Lab (D65). 사람 눈에 가까운 색 공간이라 색감 맞추기에 쓴다. */
object ColorLab {
    private const val XN = 0.95047
    private const val YN = 1.0
    private const val ZN = 1.08883
    private const val EPS = 216.0 / 24389.0
    private const val KAPPA = 24389.0 / 27.0

    /** 0..255 → 선형 밝기 (자주 쓰므로 미리 계산) */
    private val LINEAR = DoubleArray(256) { i ->
        val c = i / 255.0
        if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    fun fromArgb(argb: Int): DoubleArray {
        val r = LINEAR[(argb shr 16) and 0xFF]
        val g = LINEAR[(argb shr 8) and 0xFF]
        val b = LINEAR[argb and 0xFF]
        val x = (0.4124564 * r + 0.3575761 * g + 0.1804375 * b) / XN
        val y = (0.2126729 * r + 0.7151522 * g + 0.0721750 * b) / YN
        val z = (0.0193339 * r + 0.1191920 * g + 0.9503041 * b) / ZN
        val fx = f(x)
        val fy = f(y)
        val fz = f(z)
        return doubleArrayOf(116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz))
    }

    /** @param alpha 결과에 그대로 붙일 투명도 (0..255) */
    fun toArgb(lab: DoubleArray, alpha: Int = 0xFF): Int {
        val fy = (lab[0] + 16) / 116
        val fx = fy + lab[1] / 500
        val fz = fy - lab[2] / 200
        val x = fInv(fx) * XN
        val y = fInv(fy) * YN
        val z = fInv(fz) * ZN
        val r = 3.2404542 * x - 1.5371385 * y - 0.4985314 * z
        val g = -0.9692660 * x + 1.8760108 * y + 0.0415560 * z
        val b = 0.0556434 * x - 0.2040259 * y + 1.0572252 * z
        return (alpha shl 24) or (gamma(r) shl 16) or (gamma(g) shl 8) or gamma(b)
    }

    private fun f(t: Double) = if (t > EPS) cbrt(t) else (KAPPA * t + 16) / 116

    private fun fInv(t: Double): Double {
        val t3 = t * t * t
        return if (t3 > EPS) t3 else (116 * t - 16) / KAPPA
    }

    private fun gamma(linear: Double): Int {
        val c = linear.coerceIn(0.0, 1.0)
        val s = if (c <= 0.0031308) 12.92 * c else 1.055 * c.pow(1 / 2.4) - 0.055
        return (s * 255).roundToInt().coerceIn(0, 255)
    }
}
