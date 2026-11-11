package dev.stpark.namchin.core.composition

import dev.stpark.namchin.core.composition.CompositionTemplate.FULL_BODY
import dev.stpark.namchin.core.composition.CompositionTemplate.HALF_BODY
import dev.stpark.namchin.core.composition.CompositionTemplate.HORIZON_T
import dev.stpark.namchin.core.composition.CompositionTemplate.SIDE_THIRD
import dev.stpark.namchin.core.guidance.Guidance
import dev.stpark.namchin.core.guidance.Guidance.Side
import dev.stpark.namchin.core.testutil.STANDING
import dev.stpark.namchin.core.testutil.inFrame
import dev.stpark.namchin.core.testutil.pose
import dev.stpark.namchin.core.testutil.transformed
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CompositionTemplateTest {

    /** STANDING(어깨 0.30, 골반 0.55, 발목 0.90)을 키우고 옮겨 각 틀의 자리에 맞춘 인물 */
    private val fullBody = pose(STANDING.transformed(scale = 1.17f, dx = -0.085f, dy = -0.10f)).inFrame()
    private val halfBody = pose(STANDING.transformed(scale = 2f, dx = -0.5f, dy = -0.18f)).inFrame()
    private fun small(x: Float) = pose(STANDING.transformed(scale = 0.6f, dx = x - 0.3f, dy = 0.31f)).inFrame()

    @Test
    fun `person on the full body spot gets nothing to fix`() {
        val result = TemplateCoach.analyze(FULL_BODY, fullBody)

        assertTrue(result.cues.isEmpty(), "${result.cues}")
        assertTrue(result.fits)
    }

    @Test
    fun `too small for full body asks to come closer`() {
        val result = TemplateCoach.analyze(FULL_BODY, small(0.5f))

        assertTrue(Guidance.MoveCloser in result.cues)
        assertFalse(result.fits)
    }

    @Test
    fun `half body spot fits a waist up person`() {
        val result = TemplateCoach.analyze(HALF_BODY, halfBody)

        assertTrue(result.cues.isEmpty(), "${result.cues}")
    }

    @Test
    fun `side third picks the nearer third and pans her onto it`() {
        val result = TemplateCoach.analyze(SIDE_THIRD, small(0.45f))

        assertEquals(Side.LEFT, result.side)
        // 인물이 왼쪽 1/3보다 오른쪽에 있음 → 폰을 오른쪽으로 돌려야 인물이 왼쪽으로 온다
        assertTrue(Guidance.PanRight in result.cues)
    }

    @Test
    fun `side choice does not flip around the middle`() {
        val result = TemplateCoach.analyze(SIDE_THIRD, small(0.56f), previousSide = Side.LEFT)

        assertEquals(Side.LEFT, result.side)
        assertEquals(Side.RIGHT, TemplateCoach.analyze(SIDE_THIRD, small(0.7f), previousSide = Side.LEFT).side)
    }

    @Test
    fun `small person on a third fits without asking for feet at the bottom`() {
        val result = TemplateCoach.analyze(SIDE_THIRD, small(1f / 3), previousSide = Side.LEFT)

        assertFalse(Guidance.FeetToBottom in result.cues)
        assertTrue(result.fits, "${result.cues}")
    }

    @Test
    fun `horizon template centers her`() {
        val offCenter = pose(STANDING.transformed(scale = 0.55f, dx = 0.025f, dy = 0.385f)).inFrame()

        assertTrue(Guidance.PanLeft in TemplateCoach.analyze(HORIZON_T, offCenter).cues)
    }

    @Test
    fun `no person means nothing to coach`() {
        val result = TemplateCoach.analyze(FULL_BODY, null)

        assertTrue(result.cues.isEmpty())
        assertFalse(result.fits)
    }

    @Test
    fun `guides draw their helper lines`() {
        assertTrue(TemplateGuide.of(HORIZON_T, null).lines.any { it.y1 == it.y2 }, "수평선")
        assertTrue(TemplateGuide.of(SIDE_THIRD, Side.RIGHT).lines.any { it.x1 == 2f / 3 && it.x2 == 2f / 3 })
        assertTrue(TemplateGuide.of(FULL_BODY, null).lines.any { it.y1 == it.y2 && it.y1 > 0.9f }, "발끝 선")
    }

    @Test
    fun `guide silhouette sits on the target spot`() {
        val guide = TemplateGuide.of(SIDE_THIRD, Side.LEFT)

        assertEquals(1f / 3, guide.centerX, 1e-4f)
        assertTrue(guide.headTop < guide.shoulderY && guide.shoulderY < guide.hipY && guide.hipY < guide.bottom)
        assertTrue(guide.headTop + guide.headRadius * 2 < guide.shoulderY, "머리가 어깨와 겹치지 않음")
    }

    @Test
    fun `full body with feet cut off is not a fit even when the size looks right`() {
        val feetCut = pose(STANDING.transformed(scale = 1.344f, dx = -0.172f, dy = -0.093f)).inFrame()

        val result = TemplateCoach.analyze(FULL_BODY, feetCut)

        assertFalse(result.fits, "${result.cues}")
        assertTrue(Guidance.TiltDown in result.cues, "${result.cues}")
    }

    @Test
    fun `half body silhouette closes at the waist without squashed legs`() {
        val guide = TemplateGuide.of(HALF_BODY, null)

        assertEquals(guide.hipY, guide.bottom, 1e-4f)
    }
}
