package dev.stpark.namchin.core.guidance

import dev.stpark.namchin.core.guidance.GuidancePrioritizer.MIN_GAP_MS
import dev.stpark.namchin.core.guidance.GuidancePrioritizer.REPEAT_COOLDOWN_MS
import dev.stpark.namchin.core.model.BodyPart
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class GuidancePrioritizerTest {

    private val start = GuidanceState()

    @Test
    fun `no cues means nothing to show or say`() {
        val decision = GuidancePrioritizer.decide(emptyList(), start, nowMs = 0)

        assertNull(decision.display)
        assertNull(decision.announce)
    }

    @Test
    fun `highest priority cue wins - level, distance, vertical, horizontal, pose`() {
        val cues = listOf(
            Guidance.Pose(BodyPart.LEFT_FOREARM),
            Guidance.PanLeft,
            Guidance.TiltUp,
            Guidance.MoveBack,
            Guidance.Level(Rotation.CLOCKWISE),
        )

        assertEquals(Guidance.Level(Rotation.CLOCKWISE), GuidancePrioritizer.decide(cues, start, 0).display)
        assertEquals(Guidance.MoveBack, GuidancePrioritizer.decide(cues.dropLast(1), start, 0).display)
        assertEquals(Guidance.TiltUp, GuidancePrioritizer.decide(cues.take(3), start, 0).display)
        assertEquals(Guidance.PanLeft, GuidancePrioritizer.decide(cues.take(2), start, 0).display)
    }

    @Test
    fun `first cue is announced immediately`() {
        val decision = GuidancePrioritizer.decide(listOf(Guidance.MoveBack), start, nowMs = 0)

        assertEquals(Guidance.MoveBack, decision.announce)
    }

    @Test
    fun `same cue is not repeated within cooldown`() {
        val first = GuidancePrioritizer.decide(listOf(Guidance.MoveBack), start, nowMs = 0)

        val soon = GuidancePrioritizer.decide(listOf(Guidance.MoveBack), first.state, REPEAT_COOLDOWN_MS - 1)
        val later = GuidancePrioritizer.decide(listOf(Guidance.MoveBack), first.state, REPEAT_COOLDOWN_MS)

        assertEquals(Guidance.MoveBack, soon.display)
        assertNull(soon.announce)
        assertEquals(Guidance.MoveBack, later.announce)
    }

    @Test
    fun `different cue waits for the minimum gap`() {
        val first = GuidancePrioritizer.decide(listOf(Guidance.MoveBack), start, nowMs = 0)

        val tooSoon = GuidancePrioritizer.decide(listOf(Guidance.PanLeft), first.state, MIN_GAP_MS - 1)
        val ok = GuidancePrioritizer.decide(listOf(Guidance.PanLeft), first.state, MIN_GAP_MS)

        assertNull(tooSoon.announce)
        assertEquals(Guidance.PanLeft, ok.announce)
    }

    @Test
    fun `state is unchanged when nothing is announced`() {
        val first = GuidancePrioritizer.decide(listOf(Guidance.MoveBack), start, nowMs = 0)

        val quiet = GuidancePrioritizer.decide(listOf(Guidance.MoveBack), first.state, 100)

        assertEquals(first.state, quiet.state)
    }
}
