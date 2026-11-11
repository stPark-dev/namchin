package dev.stpark.namchin.core.live

import dev.stpark.namchin.core.guidance.Guidance
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class TeleShotTest {

    @Test
    fun `wide lens on a person asks to step back and zoom`() {
        assertEquals(Guidance.TeleZoom(2f), TeleShot.cue(zoomRatio = 1f, target = 2f, personVisible = true))
    }

    @Test
    fun `nothing to say once zoomed close to the target`() {
        assertNull(TeleShot.cue(zoomRatio = 2f, target = 2f, personVisible = true))
        assertNull(TeleShot.cue(zoomRatio = 1.7f, target = 2f, personVisible = true))
        assertNull(TeleShot.cue(zoomRatio = 3f, target = 2f, personVisible = true))
    }

    @Test
    fun `off when the setting is 1x or nobody is in frame`() {
        assertNull(TeleShot.cue(zoomRatio = 1f, target = 1f, personVisible = true))
        assertNull(TeleShot.cue(zoomRatio = 1f, target = 3f, personVisible = false))
    }

    @Test
    fun `distance cues are dropped while the zoom is wrong`() {
        val cues = listOf(Guidance.MoveCloser, Guidance.PanLeft, Guidance.TeleZoom(3f))

        assertEquals(listOf(Guidance.PanLeft, Guidance.TeleZoom(3f)), TeleShot.withoutDistance(cues))
    }

    @Test
    fun `distance cues stay when zoom is fine`() {
        val cues = listOf(Guidance.MoveBack, Guidance.PanLeft)

        assertEquals(cues, TeleShot.withoutDistance(cues))
    }

    @Test
    fun `a deliberate ultra wide shot is left alone`() {
        assertNull(TeleShot.cue(zoomRatio = 0.6f, target = 2f, personVisible = true))
    }
}
