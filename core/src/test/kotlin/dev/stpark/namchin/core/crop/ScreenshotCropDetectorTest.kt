package dev.stpark.namchin.core.crop

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.random.Random

class ScreenshotCropDetectorTest {

    private val w = 100
    private val h = 250

    private fun image(pixel: (x: Int, y: Int) -> Int): LumaImage =
        LumaImage(w, h, IntArray(w * h) { pixel(it % w, it / w) })

    private val rnd = Random(42)
    private fun photo() = rnd.nextInt(30, 220)
    private fun lightUi(x: Int) = if (x % 10 == 0) 0 else 255 // 흰 배경 + 드문 글자
    private fun darkUi(x: Int) = if (x % 10 == 0) 255 else 0

    @Test
    fun `finds a 4 by 5 photo between light UI bars`() {
        val img = image { x, y -> if (y in 40 until 165) photo() else lightUi(x) }

        assertEquals(CropRect(0, 40, w, 165), ScreenshotCropDetector.detect(img))
    }

    @Test
    fun `finds a square photo between dark mode UI bars`() {
        val img = image { x, y -> if (y in 50 until 150) photo() else darkUi(x) }

        assertEquals(CropRect(0, 50, w, 150), ScreenshotCropDetector.detect(img))
    }

    @Test
    fun `plain photo returns the whole image regardless of ratio`() {
        assertEquals(CropRect(0, 0, w, h), ScreenshotCropDetector.detect(image { _, _ -> photo() }))
    }

    @Test
    fun `a thin white line inside the photo does not split it`() {
        val img = image { x, y ->
            when {
                y == 100 -> 255
                y in 40 until 165 -> photo()
                else -> lightUi(x)
            }
        }

        assertEquals(CropRect(0, 40, w, 165), ScreenshotCropDetector.detect(img))
    }

    @Test
    fun `detects side margins`() {
        val img = image { x, y -> if (y in 40 until 140 && x in 10 until 90) photo() else lightUi(x + 1) }

        assertEquals(CropRect(10, 40, 90, 140), ScreenshotCropDetector.detect(img))
    }

    @Test
    fun `a dark band splitting a night photo gives up instead of a wrong crop`() {
        val img = image { x, y ->
            when {
                y in 80..100 -> 0
                y in 40 until 165 -> photo()
                else -> lightUi(x)
            }
        }

        assertNull(ScreenshotCropDetector.detect(img))
    }

    @Test
    fun `an image not shaped like a phone screen is used whole even with dark bands`() {
        val photo = LumaImage(80, 100, IntArray(80 * 100) { if (it / 80 < 10) 0 else photo() })

        assertEquals(CropRect(0, 0, 80, 100), ScreenshotCropDetector.detect(photo))
    }

    @Test
    fun `all UI returns null`() {
        assertNull(ScreenshotCropDetector.detect(image { x, _ -> lightUi(x) }))
    }

    @Test
    fun `too small photo region returns null`() {
        assertNull(ScreenshotCropDetector.detect(image { x, y -> if (y in 40 until 50) photo() else lightUi(x) }))
    }

    @Test
    fun `rejects data that does not match dimensions`() {
        assertThrows<IllegalArgumentException> { LumaImage(2, 2, IntArray(3)) }
    }
}
