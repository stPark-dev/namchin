package dev.stpark.namchin.core.live

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.math.cos
import kotlin.math.sin

class DeviceTiltTest {

    private val g = 9.81f

    @Test
    fun `upright portrait phone has zero roll`() {
        assertEquals(0f, DeviceTilt.rollDegrees(gx = 0f, gy = g), 1e-3f)
    }

    @Test
    fun `phone rotated clockwise reports positive roll`() {
        val theta = Math.toRadians(10.0)
        // 시계 방향으로 돌리면 화면 기준 위쪽(중력 반대)이 왼쪽(-x)으로 기운다
        val roll = DeviceTilt.rollDegrees(gx = (-g * sin(theta)).toFloat(), gy = (g * cos(theta)).toFloat())

        assertEquals(10f, roll, 1e-3f)
    }

    @Test
    fun `phone rotated counterclockwise reports negative roll`() {
        val theta = Math.toRadians(-7.0)
        val roll = DeviceTilt.rollDegrees(gx = (-g * sin(theta)).toFloat(), gy = (g * cos(theta)).toFloat())

        assertEquals(-7f, roll, 1e-3f)
    }
}
