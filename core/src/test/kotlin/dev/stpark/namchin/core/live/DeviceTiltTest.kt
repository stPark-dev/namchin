package dev.stpark.namchin.core.live

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import kotlin.math.cos
import kotlin.math.sin

class DeviceTiltTest {

    private val g = 9.81f

    @Test
    fun `upright portrait phone has zero roll`() {
        assertEquals(0f, DeviceTilt.rollDegrees(gx = 0f, gy = g)!!, 1e-3f)
    }

    @Test
    fun `phone rotated clockwise reports positive roll`() {
        val theta = Math.toRadians(10.0)
        // 시계 방향으로 돌리면 화면 기준 위쪽(중력 반대)이 왼쪽(-x)으로 기운다
        val roll = DeviceTilt.rollDegrees(gx = (-g * sin(theta)).toFloat(), gy = (g * cos(theta)).toFloat())

        assertEquals(10f, roll!!, 1e-3f)
    }

    @Test
    fun `phone rotated counterclockwise reports negative roll`() {
        val theta = Math.toRadians(-7.0)
        val roll = DeviceTilt.rollDegrees(gx = (-g * sin(theta)).toFloat(), gy = (g * cos(theta)).toFloat())

        assertEquals(-7f, roll!!, 1e-3f)
    }

    @Test
    fun `phone lying nearly flat has no meaningful roll`() {
        assertNull(DeviceTilt.rollDegrees(gx = 0.5f, gy = 0.5f))
    }

    @Test
    fun `upright phone has zero pitch`() {
        assertEquals(0f, DeviceTilt.pitchDegrees(gx = 0f, gy = g, gz = 0f), 1e-3f)
    }

    @Test
    fun `camera pointing down reports positive pitch`() {
        val theta = Math.toRadians(25.0)
        // 폰 윗부분을 앞으로 숙이면 화면이 하늘을 향해 중력이 +z로 옮겨 간다
        val pitch = DeviceTilt.pitchDegrees(gx = 0f, gy = (g * cos(theta)).toFloat(), gz = (g * sin(theta)).toFloat())

        assertEquals(25f, pitch, 1e-3f)
    }

    @Test
    fun `camera pointing up reports negative pitch even when rolled`() {
        val theta = Math.toRadians(-15.0)
        val inPlane = g * cos(theta)
        val pitch = DeviceTilt.pitchDegrees(
            gx = (inPlane * sin(Math.toRadians(20.0))).toFloat(),
            gy = (inPlane * cos(Math.toRadians(20.0))).toFloat(),
            gz = (g * sin(theta)).toFloat(),
        )

        assertEquals(-15f, pitch, 1e-3f)
    }

    // --- 화면이 뒤집혔을 때 (폰을 거꾸로 들고 찍기) ---

    @Test
    fun `roll is measured against the screen, so an upside down phone held level reads zero`() {
        // 거꾸로 든 폰: 중력이 -y → 기기 기준 180°, 화면도 180° 돌았으니 화면 기준 0°
        val deviceRoll = DeviceTilt.rollDegrees(gx = 0f, gy = -g)!!

        assertEquals(0f, DeviceTilt.onScreen(deviceRoll, displayRotationDegrees = 180), 1e-3f)
    }

    @Test
    fun `a clockwise turn stays clockwise on a flipped screen`() {
        // 화면 기준 시계 방향 10°: 기기 기준으로는 190° (= -170°)
        assertEquals(10f, DeviceTilt.onScreen(-170f, displayRotationDegrees = 180), 1e-3f)
        assertEquals(-10f, DeviceTilt.onScreen(170f, displayRotationDegrees = 180), 1e-3f)
    }

    @Test
    fun `an upright screen leaves roll unchanged`() {
        assertEquals(7f, DeviceTilt.onScreen(7f, displayRotationDegrees = 0), 1e-3f)
    }

    @Test
    fun `turning the phone past 135 degrees means it is held upside down`() {
        assertEquals(true, DeviceTilt.upsideDown(deviceRoll = 170f, wasUpsideDown = false))
        assertEquals(true, DeviceTilt.upsideDown(deviceRoll = -140f, wasUpsideDown = false))
        assertEquals(false, DeviceTilt.upsideDown(deviceRoll = 120f, wasUpsideDown = false))
    }

    @Test
    fun `upside down holds until the phone is clearly back upright`() {
        assertEquals(true, DeviceTilt.upsideDown(deviceRoll = 100f, wasUpsideDown = true))
        assertEquals(false, DeviceTilt.upsideDown(deviceRoll = 80f, wasUpsideDown = true))
    }

    @Test
    fun `unknown roll keeps the last judgement`() {
        assertEquals(true, DeviceTilt.upsideDown(deviceRoll = null, wasUpsideDown = true))
    }
}
