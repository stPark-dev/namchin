package dev.stpark.namchin.camera

import dev.stpark.namchin.core.guidance.Guidance
import dev.stpark.namchin.core.guidance.Rotation
import dev.stpark.namchin.core.model.BodyPart
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GuidanceTextTest {

    private val allCues: List<Guidance> = listOf(
        Guidance.NoPerson,
        Guidance.Level(Rotation.CLOCKWISE),
        Guidance.Level(Rotation.COUNTERCLOCKWISE),
        Guidance.MoveCloser,
        Guidance.MoveBack,
        Guidance.TiltUp,
        Guidance.TiltDown,
        Guidance.PanLeft,
        Guidance.PanRight,
    ) + BodyPart.entries.map { Guidance.Pose(it) }

    @Test
    fun `every cue has its own message`() {
        val ids = allCues.map { GuidanceText.of(it) }

        assertEquals(ids.size, ids.toSet().size)
    }
}
