package dev.stpark.namchin.core.bestshot

import dev.stpark.namchin.core.crop.LumaImage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SharpnessTest {

    private fun image(w: Int = 40, h: Int = 40, pixel: (Int, Int) -> Int) =
        LumaImage(w, h, IntArray(w * h) { pixel(it % w, it / w) })

    private val checker = image { x, y -> if ((x / 2 + y / 2) % 2 == 0) 0 else 255 }

    /** 3x3 평균 흐림 */
    private fun blurred(src: LumaImage) = image(src.width, src.height) { x, y ->
        var sum = 0
        var n = 0
        for (dy in -1..1) for (dx in -1..1) {
            val xx = (x + dx).coerceIn(0, src.width - 1)
            val yy = (y + dy).coerceIn(0, src.height - 1)
            sum += src[xx, yy]
            n++
        }
        sum / n
    }

    @Test
    fun `flat image has zero sharpness`() {
        assertEquals(0.0, Sharpness.laplacianVariance(image { _, _ -> 128 }), 1e-9)
    }

    @Test
    fun `blurring lowers sharpness`() {
        assertTrue(Sharpness.laplacianVariance(checker) > Sharpness.laplacianVariance(blurred(checker)))
    }

    @Test
    fun `too small images have zero sharpness`() {
        assertEquals(0.0, Sharpness.laplacianVariance(image(2, 2) { x, _ -> x * 255 }), 1e-9)
    }
}
