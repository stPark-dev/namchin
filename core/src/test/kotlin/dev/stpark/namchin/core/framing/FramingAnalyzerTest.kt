package dev.stpark.namchin.core.framing

import dev.stpark.namchin.core.guidance.Guidance
import dev.stpark.namchin.core.guidance.Rotation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FramingAnalyzerTest {

    private val reference = PersonFrame(top = 0.2f, bottom = 0.8f, centerX = 0.5f)

    private fun analyze(current: PersonFrame, roll: Float = 0f) =
        FramingAnalyzer.analyze(reference, current, rollDegrees = roll)

    @Test
    fun `matching frame and level phone gives no cues`() {
        assertTrue(analyze(reference).isEmpty())
    }

    @Test
    fun `small differences within tolerance give no cues`() {
        val close = PersonFrame(top = 0.21f, bottom = 0.82f, centerX = 0.52f)

        assertTrue(analyze(close, roll = 2f).isEmpty())
    }

    @Test
    fun `phone rotated clockwise asks to rotate counterclockwise`() {
        assertEquals(listOf(Guidance.Level(Rotation.COUNTERCLOCKWISE)), analyze(reference, roll = 6f))
        assertEquals(listOf(Guidance.Level(Rotation.CLOCKWISE)), analyze(reference, roll = -6f))
    }

    @Test
    fun `person too big asks to move back`() {
        val big = PersonFrame(top = 0.05f, bottom = 0.95f, centerX = 0.5f)

        assertEquals(listOf(Guidance.MoveBack), analyze(big))
    }

    @Test
    fun `person too small asks to move closer`() {
        val small = PersonFrame(top = 0.35f, bottom = 0.65f, centerX = 0.5f)

        assertEquals(listOf(Guidance.MoveCloser), analyze(small))
    }

    @Test
    fun `person too high in frame asks to tilt up`() {
        val high = PersonFrame(top = 0.1f, bottom = 0.7f, centerX = 0.5f)

        assertEquals(listOf(Guidance.TiltUp), analyze(high))
    }

    @Test
    fun `person too low in frame asks to tilt down`() {
        val low = PersonFrame(top = 0.3f, bottom = 0.9f, centerX = 0.5f)

        assertEquals(listOf(Guidance.TiltDown), analyze(low))
    }

    @Test
    fun `person too far left asks to pan left`() {
        val left = PersonFrame(top = 0.2f, bottom = 0.8f, centerX = 0.3f)

        assertEquals(listOf(Guidance.PanLeft), analyze(left))
    }

    @Test
    fun `person too far right asks to pan right`() {
        val right = PersonFrame(top = 0.2f, bottom = 0.8f, centerX = 0.7f)

        assertEquals(listOf(Guidance.PanRight), analyze(right))
    }

    @Test
    fun `multiple problems produce multiple cues`() {
        val off = PersonFrame(top = 0.35f, bottom = 0.65f, centerX = 0.3f)

        val cues = analyze(off, roll = 10f)

        assertEquals(
            setOf(Guidance.Level(Rotation.COUNTERCLOCKWISE), Guidance.MoveCloser, Guidance.PanLeft),
            cues.toSet(),
        )
    }
}
