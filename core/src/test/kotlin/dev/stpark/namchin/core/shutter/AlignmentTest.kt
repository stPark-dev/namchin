package dev.stpark.namchin.core.shutter

import dev.stpark.namchin.core.guidance.Guidance
import dev.stpark.namchin.core.model.BodyPart
import dev.stpark.namchin.core.pose.PoseMatch
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AlignmentTest {

    private fun match(score: Float, worst: BodyPart = BodyPart.LEFT_FOREARM) =
        PoseMatch(score, mirrored = false, partScores = mapOf(worst to score, BodyPart.LEFT_THIGH to 1f))

    @Test
    fun `aligned when framing has no cues and pose is above threshold`() {
        val result = Alignment.evaluate(emptyList(), match(0.9f), poseEnabled = true)

        assertTrue(result.aligned)
        assertTrue(result.cues.isEmpty())
    }

    @Test
    fun `poor pose adds a cue for the worst part`() {
        val result = Alignment.evaluate(emptyList(), match(0.5f), poseEnabled = true)

        assertFalse(result.aligned)
        assertEquals(listOf(Guidance.Pose(BodyPart.LEFT_FOREARM)), result.cues)
    }

    @Test
    fun `framing cues block alignment even with a good pose`() {
        val result = Alignment.evaluate(listOf(Guidance.MoveBack), match(0.95f), poseEnabled = true)

        assertFalse(result.aligned)
        assertEquals(listOf(Guidance.MoveBack), result.cues)
    }

    @Test
    fun `unknown pose is never aligned`() {
        val result = Alignment.evaluate(emptyList(), null, poseEnabled = true)

        assertFalse(result.aligned)
        assertTrue(result.cues.isEmpty())
    }

    @Test
    fun `with pose disabled only framing matters`() {
        val result = Alignment.evaluate(emptyList(), match(0.1f), poseEnabled = false)

        assertTrue(result.aligned)
        assertTrue(result.cues.isEmpty())
    }
}
