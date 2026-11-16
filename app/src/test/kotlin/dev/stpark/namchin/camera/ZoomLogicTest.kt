package dev.stpark.namchin.camera

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ZoomLogicTest {

    @Test
    fun `presets outside the camera range are hidden`() {
        assertEquals(listOf(0.6f, 1f, 2f, 3f, 10f), ZoomLogic.presets(min = 0.6f, max = 100f))
        assertEquals(listOf(1f, 2f, 3f), ZoomLogic.presets(min = 1f, max = 8f))
    }

    @Test
    fun `pinch multiplies the ratio and stays in range`() {
        assertEquals(2f, ZoomLogic.pinch(current = 1f, scale = 2f, min = 0.6f, max = 10f))
        assertEquals(10f, ZoomLogic.pinch(current = 8f, scale = 2f, min = 0.6f, max = 10f))
        assertEquals(0.6f, ZoomLogic.pinch(current = 1f, scale = 0.1f, min = 0.6f, max = 10f))
    }

    @Test
    fun `selected preset is the closest one to the current ratio`() {
        val presets = listOf(0.6f, 1f, 3f, 10f)

        assertEquals(1f, ZoomLogic.selected(1.2f, presets))
        assertEquals(3f, ZoomLogic.selected(2.6f, presets))
        assertEquals(null, ZoomLogic.selected(5f, presets))
    }

    @Test
    fun `label drops trailing zero`() {
        assertEquals("1×", ZoomLogic.label(1f))
        assertEquals("0.6×", ZoomLogic.label(0.6f))
        assertEquals("2.4×", ZoomLogic.label(2.43f))
        assertEquals("2", ZoomLogic.number(2f))
    }

    @Test
    fun `tele portrait choices are the 1x, 2x and 3x buttons`() {
        assertEquals(listOf(1f, 2f, 3f), ZoomLogic.TELE_CHOICES)
    }

    // --- 인물사진 모드: 삼성 인물사진처럼 1·2·3배만 ---

    @Test
    fun `portrait mode offers only 1x 2x 3x`() {
        assertEquals(listOf(1f, 2f, 3f), ZoomLogic.presets(0.6f, 10f, portrait = true))
    }

    @Test
    fun `portrait mode snaps any ratio to the nearest of 1x 2x 3x`() {
        assertEquals(1f, ZoomLogic.portraitZoom(0.6f))
        assertEquals(1f, ZoomLogic.portraitZoom(1.4f))
        assertEquals(2f, ZoomLogic.portraitZoom(1.6f))
        assertEquals(3f, ZoomLogic.portraitZoom(10f))
    }

    @Test
    fun `normal mode keeps every lens`() {
        assertEquals(ZoomLogic.presets(0.6f, 10f), ZoomLogic.presets(0.6f, 10f, portrait = false))
    }
}
