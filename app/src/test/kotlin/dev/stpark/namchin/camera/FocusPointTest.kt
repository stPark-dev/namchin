package dev.stpark.namchin.camera

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FocusPointTest {

    @Test
    fun `a tap maps to the same spot on the preview when held upright`() {
        assertEquals(FocusPoint(0.25f, 0.75f), FocusPoint.fromTap(x = 100f, y = 600f, width = 400f, height = 800f, upsideDown = false))
    }

    @Test
    fun `a tap maps to the opposite spot on the preview when held upside down`() {
        // 화면 전체를 180° 돌려 그리고 미리보기만 되돌렸으므로, 미리보기 기준으로는 반대 자리
        assertEquals(FocusPoint(0.75f, 0.25f), FocusPoint.fromTap(x = 100f, y = 600f, width = 400f, height = 800f, upsideDown = true))
    }

    @Test
    fun `taps outside the preview are clamped to its edge`() {
        assertEquals(FocusPoint(1f, 0f), FocusPoint.fromTap(x = 500f, y = -10f, width = 400f, height = 800f, upsideDown = false))
    }
}
