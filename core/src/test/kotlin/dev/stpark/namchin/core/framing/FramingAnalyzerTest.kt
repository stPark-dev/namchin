package dev.stpark.namchin.core.framing

import dev.stpark.namchin.core.guidance.Guidance
import dev.stpark.namchin.core.guidance.Rotation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FramingAnalyzerTest {

    private val reference = PersonFrame(anchorX = 0.5f, anchorY = 0.3f, torso = 0.25f)

    private fun frame(x: Float = 0.5f, y: Float = 0.3f, torso: Float = 0.25f) = PersonFrame(x, y, torso)

    private fun analyze(current: PersonFrame, roll: Float = 0f, previous: List<Guidance> = emptyList()) =
        FramingAnalyzer.analyze(reference, current, rollDegrees = roll, previous = previous)

    @Test
    fun `matching frame and level phone gives no cues`() {
        assertTrue(analyze(reference).isEmpty())
    }

    @Test
    fun `small differences within tolerance give no cues`() {
        assertTrue(analyze(frame(x = 0.53f, y = 0.27f, torso = 0.26f), roll = 2f).isEmpty())
    }

    @Test
    fun `phone rotated clockwise asks to rotate counterclockwise`() {
        assertEquals(listOf(Guidance.Level(Rotation.COUNTERCLOCKWISE)), analyze(reference, roll = 6f))
        assertEquals(listOf(Guidance.Level(Rotation.CLOCKWISE)), analyze(reference, roll = -6f))
    }

    @Test
    fun `person too big asks to move back, too small to move closer`() {
        assertEquals(listOf(Guidance.MoveBack), analyze(frame(torso = 0.35f)))
        assertEquals(listOf(Guidance.MoveCloser), analyze(frame(torso = 0.18f)))
    }

    @Test
    fun `person too high asks to tilt up, too low to tilt down`() {
        assertEquals(listOf(Guidance.TiltUp), analyze(frame(y = 0.2f)))
        assertEquals(listOf(Guidance.TiltDown), analyze(frame(y = 0.4f)))
    }

    @Test
    fun `person too far left asks to pan left, too far right to pan right`() {
        assertEquals(listOf(Guidance.PanLeft), analyze(frame(x = 0.3f)))
        assertEquals(listOf(Guidance.PanRight), analyze(frame(x = 0.7f)))
    }

    @Test
    fun `multiple problems produce multiple cues`() {
        val cues = analyze(frame(x = 0.3f, torso = 0.15f), roll = 10f)

        assertEquals(
            setOf(Guidance.Level(Rotation.COUNTERCLOCKWISE), Guidance.MoveCloser, Guidance.PanLeft),
            cues.toSet(),
        )
    }

    @Test
    fun `an active cue persists until well inside tolerance (hysteresis)`() {
        val level = Guidance.Level(Rotation.COUNTERCLOCKWISE)

        assertTrue(analyze(reference, roll = 2.5f).isEmpty())
        assertEquals(listOf(level), analyze(reference, roll = 2.5f, previous = listOf(level)))
        assertTrue(analyze(reference, roll = 1.5f, previous = listOf(level)).isEmpty())

        assertEquals(listOf(Guidance.MoveBack), analyze(frame(torso = 0.27f), previous = listOf(Guidance.MoveBack)))
        assertTrue(analyze(frame(torso = 0.27f)).isEmpty())

        assertEquals(listOf(Guidance.PanLeft), analyze(frame(x = 0.46f), previous = listOf(Guidance.PanLeft)))
        assertTrue(analyze(frame(x = 0.46f)).isEmpty())
    }

    @Test
    fun `hysteresis only applies to the same direction`() {
        assertTrue(analyze(frame(x = 0.46f), previous = listOf(Guidance.PanRight)).isEmpty())
    }
}
