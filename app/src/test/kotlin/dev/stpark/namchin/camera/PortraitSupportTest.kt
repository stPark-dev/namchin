package dev.stpark.namchin.camera

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PortraitSupportTest {

    @Test
    fun `offered whenever the phone has bokeh`() {
        assertTrue(PortraitSupport.offered(bokeh = true))
        assertFalse(PortraitSupport.offered(bokeh = false))
    }

    @Test
    fun `used only when asked for and offered`() {
        assertTrue(PortraitSupport.use(requested = true, offered = true))
        assertFalse(PortraitSupport.use(requested = true, offered = false))
        assertFalse(PortraitSupport.use(requested = false, offered = true))
    }

    @Test
    fun `the shutter first switches to the bokeh session, then shoots there`() {
        assertTrue(PortraitSupport.switchBeforeShot(portraitMode = true, sessionActive = false))
        assertFalse(PortraitSupport.switchBeforeShot(portraitMode = true, sessionActive = true))
        assertFalse(PortraitSupport.switchBeforeShot(portraitMode = false, sessionActive = false))
    }
}
