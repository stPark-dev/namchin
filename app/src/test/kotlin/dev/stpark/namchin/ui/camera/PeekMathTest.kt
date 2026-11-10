package dev.stpark.namchin.ui.camera

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PeekMathTest {

    @Test
    fun `progress follows how far the card was dragged up`() {
        assertEquals(0f, PeekMath.progress(dragUpPx = -50f, distancePx = 400f))
        assertEquals(0.5f, PeekMath.progress(dragUpPx = 200f, distancePx = 400f))
        assertEquals(1f, PeekMath.progress(dragUpPx = 900f, distancePx = 400f))
    }

    @Test
    fun `from the hand, a short drag falls back and a longer one opens`() {
        assertEquals(0f, PeekMath.settle(progress = 0.2f, wasOpen = false))
        assertEquals(1f, PeekMath.settle(progress = 0.4f, wasOpen = false))
    }

    @Test
    fun `when open, a small pull down stays open and a longer one closes`() {
        assertEquals(1f, PeekMath.settle(progress = 0.8f, wasOpen = true))
        assertEquals(0f, PeekMath.settle(progress = 0.6f, wasOpen = true))
    }

    @Test
    fun `enlarged card fits the screen and keeps the reference ratio`() {
        val portrait = PeekMath.target(screenWidth = 1000f, screenHeight = 2000f, aspect = 0.75f)
        assertEquals(800f, portrait.width, 0.01f)
        assertEquals(800f / 0.75f, portrait.height, 0.01f)
        assertEquals(500f, portrait.left + portrait.width / 2, 0.01f)

        // 아주 긴 사진은 높이 제한에 맞춰 줄어든다
        val tall = PeekMath.target(screenWidth = 1000f, screenHeight = 2000f, aspect = 0.4f)
        assertTrue(tall.height <= 2000f * PeekMath.MAX_HEIGHT_RATIO + 0.01f)
        assertEquals(0.4f, tall.width / tall.height, 0.001f)
    }

    @Test
    fun `bounds interpolate from the hand to the target`() {
        val from = PeekMath.Bounds(900f, 1800f, 100f, 130f)
        val to = PeekMath.Bounds(100f, 400f, 800f, 1000f)

        assertEquals(from, PeekMath.lerp(from, to, 0f))
        assertEquals(to, PeekMath.lerp(from, to, 1f))
        assertEquals(PeekMath.Bounds(500f, 1100f, 450f, 565f), PeekMath.lerp(from, to, 0.5f))
    }

    @Test
    fun `release without an active drag changes nothing`() {
        // 카드가 손패에서 빠지며 끝난 제스처가 취소돼도 닫히는 중인 화면을 되돌리지 않는다
        assertEquals(null, PeekMath.releaseTarget(progress = 1f, open = false, dragging = false))
        assertEquals(1f, PeekMath.releaseTarget(progress = 0.5f, open = false, dragging = true))
    }

    @Test
    fun `tapping a card opens it fully`() {
        assertEquals(1f, PeekMath.TAP_TARGET)
    }
}
