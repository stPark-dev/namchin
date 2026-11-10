package dev.stpark.namchin.ui.camera

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CardFanTest {

    @Test
    fun `shows at most three cards`() {
        assertEquals(0, CardFan.layout(0).size)
        assertEquals(2, CardFan.layout(2).size)
        assertEquals(3, CardFan.layout(10).size)
    }

    @Test
    fun `front card is the last one and the most upright`() {
        val fan = CardFan.layout(3)

        assertEquals(0f, fan.last().rotationDeg)
        assertTrue(fan.zipWithNext().all { (a, b) -> a.rotationDeg < b.rotationDeg })
    }

    @Test
    fun `cards behind sit further left so each peeks out`() {
        val fan = CardFan.layout(3)

        assertTrue(fan.zipWithNext().all { (a, b) -> a.offsetXDp < b.offsetXDp })
    }
}
