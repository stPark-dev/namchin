package dev.stpark.namchin.core.shutter

import dev.stpark.namchin.core.guidance.Guidance
import dev.stpark.namchin.core.model.BodyPart
import dev.stpark.namchin.core.pose.PoseMatch
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AlignmentTest {

    /** 평균 avg, 가장 나쁜 마디(LEFT_FOREARM) 점수 worst인 매치. */
    private fun match(avg: Float, worst: Float = avg) = PoseMatch(
        score = avg,
        mirrored = false,
        partScores = mapOf(BodyPart.LEFT_FOREARM to worst, BodyPart.LEFT_THIGH to 1f),
    )

    @Test
    fun `aligned when framing is fine and every part is close enough`() {
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
    fun `one badly wrong limb blocks alignment even with a high average`() {
        val result = Alignment.evaluate(emptyList(), match(avg = 0.85f, worst = 0f), poseEnabled = true)

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
    fun `undetected person asks to get the person in frame`() {
        val result = Alignment.evaluate(null, null, poseEnabled = true)

        assertFalse(result.aligned)
        assertEquals(listOf(Guidance.NoPerson), result.cues)
    }

    @Test
    fun `without a comparable pose only framing matters`() {
        val result = Alignment.evaluate(emptyList(), null, poseEnabled = true)

        assertTrue(result.aligned)
        assertTrue(result.cues.isEmpty())
    }

    @Test
    fun `with pose disabled only framing matters`() {
        val result = Alignment.evaluate(emptyList(), match(0.1f), poseEnabled = false)

        assertTrue(result.aligned)
        assertTrue(result.cues.isEmpty())
    }
}
