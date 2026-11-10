package dev.stpark.namchin.camera

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ShutterGateTest {

    @Test
    fun `basic mode can shoot once the camera is ready`() {
        assertTrue(ShutterGate.canShoot(selectedId = null, activeId = null, cameraReady = true, capturing = false))
        assertFalse(ShutterGate.canShoot(selectedId = null, activeId = null, cameraReady = false, capturing = false))
    }

    @Test
    fun `cannot shoot while the selected reference is not active yet`() {
        assertFalse(ShutterGate.canShoot(selectedId = 7, activeId = null, cameraReady = true, capturing = false))
        assertFalse(ShutterGate.canShoot(selectedId = 7, activeId = 3, cameraReady = true, capturing = false))
        assertTrue(ShutterGate.canShoot(selectedId = 7, activeId = 7, cameraReady = true, capturing = false))
    }

    @Test
    fun `cannot shoot after clearing until the old reference is gone`() {
        assertFalse(ShutterGate.canShoot(selectedId = null, activeId = 7, cameraReady = true, capturing = false))
    }

    @Test
    fun `cannot shoot while a capture is in flight`() {
        assertFalse(ShutterGate.canShoot(selectedId = null, activeId = null, cameraReady = true, capturing = true))
    }
}
