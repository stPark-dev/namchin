package dev.stpark.namchin.core.crop

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import kotlin.random.Random

class ScreenshotCropDetectorTest {

    private val w = 100
    private val h = 200

    /** 행 범위마다 픽셀 생성기를 지정해 합성 이미지를 만든다. */
    private fun image(pixel: (x: Int, y: Int) -> Int): LumaImage =
        LumaImage(w, h, IntArray(w * h) { pixel(it % w, it / w) })

    private val rnd = Random(42)
    private fun photo() = rnd.nextInt(30, 220)
    private fun lightUi(x: Int) = if (x % 10 == 0) 0 else 255 // 흰 배경 + 드문 글자
    private fun darkUi(x: Int) = if (x % 10 == 0) 255 else 0

    @Test
    fun `finds photo between light UI bars`() {
        val img = image { x, y -> if (y in 30 until 150) photo() else lightUi(x) }

        assertEquals(CropRect(0, 30, w, 150), ScreenshotCropDetector.detect(img))
    }

    @Test
    fun `finds photo between dark mode UI bars`() {
        val img = image { x, y -> if (y in 40 until 180) photo() else darkUi(x) }

        assertEquals(CropRect(0, 40, w, 180), ScreenshotCropDetector.detect(img))
    }

    @Test
    fun `plain photo returns the whole image`() {
        val img = image { _, _ -> photo() }

        assertEquals(CropRect(0, 0, w, h), ScreenshotCropDetector.detect(img))
    }

    @Test
    fun `a thin white line inside the photo does not split it`() {
        val img = image { x, y ->
            when {
                y == 90 -> 255
                y in 30 until 150 -> photo()
                else -> lightUi(x)
            }
        }

        assertEquals(CropRect(0, 30, w, 150), ScreenshotCropDetector.detect(img))
    }

    @Test
    fun `detects side margins`() {
        val img = image { x, y -> if (y in 30 until 150 && x in 10 until 90) photo() else lightUi(x + 1) }

        assertEquals(CropRect(10, 30, 90, 150), ScreenshotCropDetector.detect(img))
    }

    @Test
    fun `all UI returns null`() {
        assertNull(ScreenshotCropDetector.detect(image { x, _ -> lightUi(x) }))
    }

    @Test
    fun `too small photo region returns null`() {
        val img = image { x, y -> if (y in 30 until 40) photo() else lightUi(x) }

        assertNull(ScreenshotCropDetector.detect(img))
    }

    @Test
    fun `rejects data that does not match dimensions`() {
        org.junit.jupiter.api.assertThrows<IllegalArgumentException> { LumaImage(2, 2, IntArray(3)) }
    }
}
