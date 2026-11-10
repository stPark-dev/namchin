package dev.stpark.namchin.core.composition

import dev.stpark.namchin.core.guidance.Guidance
import dev.stpark.namchin.core.guidance.Guidance.CutPoint
import dev.stpark.namchin.core.guidance.Guidance.Side
import dev.stpark.namchin.core.model.Joint
import dev.stpark.namchin.core.model.Point
import dev.stpark.namchin.core.testutil.STANDING
import dev.stpark.namchin.core.testutil.pose
import dev.stpark.namchin.core.testutil.transformed
import dev.stpark.namchin.core.testutil.with
import dev.stpark.namchin.core.testutil.withConfidence
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CompositionAnalyzerTest {

    private fun analyze(points: Map<Joint, Point>, previous: List<Guidance> = emptyList()) =
        CompositionAnalyzer.analyze(pose(points), previous)

    @Test
    fun `well framed full body facing front needs nothing`() {
        assertTrue(analyze(STANDING).isEmpty())
    }

    @Test
    fun `full body with feet floating high asks to put feet at the bottom`() {
        val small = STANDING.transformed(scale = 0.8f, dx = 0.1f, dy = 0f) // 발목 y = 0.68

        assertEquals(listOf(Guidance.FeetToBottom), analyze(small))
    }

    @Test
    fun `feet cue persists until clearly fixed`() {
        val almost = STANDING.with(Joint.LEFT_ANKLE to Point(0.53f, 0.86f), Joint.RIGHT_ANKLE to Point(0.47f, 0.86f))

        assertTrue(analyze(almost).isEmpty())
        assertEquals(listOf(Guidance.FeetToBottom), analyze(almost, previous = listOf(Guidance.FeetToBottom)))
    }

    @Test
    fun `head above the top edge is a head cut`() {
        val high = STANDING.transformed(scale = 1f, dx = 0f, dy = -0.2f) // 코 y = 0.0

        assertTrue(Guidance.HeadCut in analyze(high))
    }

    @Test
    fun `big subject with lots of empty space above asks to tilt down`() {
        val low = mapOf(
            Joint.NOSE to Point(0.5f, 0.40f),
            Joint.LEFT_SHOULDER to Point(0.6f, 0.50f),
            Joint.RIGHT_SHOULDER to Point(0.4f, 0.50f),
            Joint.LEFT_HIP to Point(0.56f, 0.88f),
            Joint.RIGHT_HIP to Point(0.44f, 0.88f),
        )

        assertEquals(listOf(Guidance.TooMuchHeadroom), analyze(low))
    }

    @Test
    fun `frame edge through the ankles is a cut at the ankle`() {
        val shifted = STANDING.transformed(scale = 1f, dx = 0f, dy = 0.15f) // 발목 y = 1.0

        assertTrue(Guidance.CutAtJoint(CutPoint.ANKLE) in analyze(shifted))
    }

    @Test
    fun `frame edge through the knees is a cut at the knee`() {
        val p = pose(STANDING.transformed(scale = 1f, dx = 0f, dy = 0.3f)) // 무릎 1.0, 발목 1.15
            .withConfidence(Joint.LEFT_ANKLE, 0f)
            .withConfidence(Joint.RIGHT_ANKLE, 0f)

        val cues = CompositionAnalyzer.analyze(p, emptyList())

        assertTrue(Guidance.CutAtJoint(CutPoint.KNEE) in cues)
        assertFalse(Guidance.CutAtJoint(CutPoint.ANKLE) in cues)
    }

    @Test
    fun `hand at the side edge is a cut at the wrist`() {
        val reaching = STANDING.with(Joint.LEFT_ELBOW to Point(0.85f, 0.35f), Joint.LEFT_WRIST to Point(0.99f, 0.35f))

        assertTrue(Guidance.CutAtJoint(CutPoint.WRIST) in analyze(reaching))
    }

    @Test
    fun `looking right while centered asks for space on the right`() {
        val lookingRight = STANDING.with(Joint.NOSE to Point(0.56f, 0.20f))

        assertEquals(listOf(Guidance.GazeSpace(Side.RIGHT)), analyze(lookingRight))
    }

    @Test
    fun `looking right from the left third is already good`() {
        val placed = STANDING.with(Joint.NOSE to Point(0.56f, 0.20f)).transformed(scale = 1f, dx = -0.17f, dy = 0f)

        assertTrue(analyze(placed).isEmpty())
    }

    @Test
    fun `looking left while centered asks for space on the left`() {
        val lookingLeft = STANDING.with(Joint.NOSE to Point(0.44f, 0.20f))

        assertEquals(listOf(Guidance.GazeSpace(Side.LEFT)), analyze(lookingLeft))
    }

    @Test
    fun `without shoulders nothing is judged`() {
        val p = pose(STANDING.transformed(0.8f, 0.1f, 0f))
            .withConfidence(Joint.LEFT_SHOULDER, 0f)

        assertTrue(CompositionAnalyzer.analyze(p, emptyList()).isEmpty())
    }
}
