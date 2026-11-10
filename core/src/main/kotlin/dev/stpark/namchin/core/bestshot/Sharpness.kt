package dev.stpark.namchin.core.bestshot

import dev.stpark.namchin.core.crop.LumaImage

/** 선명도: 라플라시안(4방향) 응답의 분산. 흔들리거나 초점이 나간 사진일수록 작다. */
object Sharpness {
    fun laplacianVariance(image: LumaImage): Double {
        if (image.width < 3 || image.height < 3) return 0.0
        var sum = 0.0
        var sumSq = 0.0
        var n = 0
        for (y in 1 until image.height - 1) {
            for (x in 1 until image.width - 1) {
                val lap = (image[x - 1, y] + image[x + 1, y] + image[x, y - 1] + image[x, y + 1] - 4 * image[x, y]).toDouble()
                sum += lap
                sumSq += lap * lap
                n++
            }
        }
        val mean = sum / n
        return sumSq / n - mean * mean
    }
}
