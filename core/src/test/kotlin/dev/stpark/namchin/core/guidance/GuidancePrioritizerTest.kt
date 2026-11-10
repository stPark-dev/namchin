package dev.stpark.namchin.core.guidance

import dev.stpark.namchin.core.guidance.GuidancePrioritizer.MIN_GAP_MS
import dev.stpark.namchin.core.guidance.GuidancePrioritizer.REPEAT_COOLDOWN_MS
import dev.stpark.namchin.core.guidance.GuidancePrioritizer.STABLE_MS
import dev.stpark.namchin.core.model.BodyPart
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class GuidancePrioritizerTest {

    /** (시각, 큐 목록) 순서대로 넣고 각 결정을 돌려준다. */
    private fun run(vararg frames: Pair<Long, List<Guidance>>): List<GuidanceDecision> {
        var state = GuidanceState()
        return frames.map { (now, cues) ->
            GuidancePrioritizer.decide(cues, state, now).also { state = it.state }
        }
    }

    private val back = listOf(Guidance.MoveBack)
    private val pan = listOf(Guidance.PanLeft)

    @Test
    fun `no cues means nothing to show or say`() {
        val d = run(0L to emptyList()).single()

        assertNull(d.display)
        assertNull(d.announce)
    }

    @Test
    fun `highest priority cue is displayed - person, level, distance, vertical, horizontal, pose`() {
        val cues = listOf(
            Guidance.Pose(BodyPart.LEFT_FOREARM),
            Guidance.PanLeft,
            Guidance.TiltUp,
            Guidance.MoveBack,
            Guidance.Level(Rotation.CLOCKWISE),
            Guidance.NoPerson,
        )
        val expected = listOf(
            Guidance.NoPerson,
            Guidance.Level(Rotation.CLOCKWISE),
            Guidance.MoveBack,
            Guidance.TiltUp,
            Guidance.PanLeft,
            Guidance.Pose(BodyPart.LEFT_FOREARM),
        )

        expected.forEachIndexed { i, cue ->
            assertEquals(cue, run(0L to cues.dropLast(i)).single().display)
        }
    }

    @Test
    fun `cue is announced only after staying on top for STABLE_MS`() {
        val d = run(0L to back, STABLE_MS - 1 to back, STABLE_MS to back)

        assertEquals(listOf(null, null, Guidance.MoveBack), d.map { it.announce })
    }

    @Test
    fun `a one-frame blip resets stability and is never announced`() {
        val d = run(0L to back, 100L to pan, 200L to back, STABLE_MS to back, 200 + STABLE_MS to back)

        assertEquals(listOf(null, null, null, null, Guidance.MoveBack), d.map { it.announce })
    }

    @Test
    fun `same cue is not repeated within cooldown`() {
        val t = STABLE_MS
        val d = run(0L to back, t to back, t + REPEAT_COOLDOWN_MS - 1 to back, t + REPEAT_COOLDOWN_MS to back)

        assertEquals(listOf(null, Guidance.MoveBack, null, Guidance.MoveBack), d.map { it.announce })
        assertEquals(Guidance.MoveBack, d[2].display)
    }

    @Test
    fun `different cue waits for the minimum gap`() {
        val t = STABLE_MS
        val d = run(0L to back, t to back, t + 1 to pan, t + MIN_GAP_MS - 1 to pan, t + MIN_GAP_MS to pan)

        assertEquals(listOf(null, Guidance.MoveBack, null, null, Guidance.PanLeft), d.map { it.announce })
    }
}
