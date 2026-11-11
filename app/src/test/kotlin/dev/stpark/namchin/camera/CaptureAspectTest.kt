package dev.stpark.namchin.camera

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CaptureAspectTest {

    @Test
    fun `reference ratio wins`() {
        assertEquals(CaptureAspect(1080, 1350), CaptureAspect.of(referenceWidth = 1080, referenceHeight = 1350, instaRatio = false))
        assertEquals(CaptureAspect(3, 2), CaptureAspect.of(referenceWidth = 3, referenceHeight = 2, instaRatio = true))
    }

    @Test
    fun `without a reference it is 3 to 4, or 4 to 5 for instagram`() {
        assertEquals(CaptureAspect(3, 4), CaptureAspect.of(null, null, instaRatio = false))
        assertEquals(CaptureAspect(4, 5), CaptureAspect.of(null, null, instaRatio = true))
    }

    @Test
    fun `ratio is width over height`() {
        assertEquals(0.8f, CaptureAspect(4, 5).ratio)
    }
}
