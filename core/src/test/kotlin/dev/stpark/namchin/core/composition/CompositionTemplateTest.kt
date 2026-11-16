package dev.stpark.namchin.core.composition

import dev.stpark.namchin.core.composition.CompositionTemplate.DIAGONAL_SPACE
import dev.stpark.namchin.core.composition.CompositionTemplate.FULL_BODY
import dev.stpark.namchin.core.composition.CompositionTemplate.GOLDEN_SPIRAL
import dev.stpark.namchin.core.composition.CompositionTemplate.HALF_BODY
import dev.stpark.namchin.core.composition.CompositionTemplate.HORIZON_T
import dev.stpark.namchin.core.composition.CompositionTemplate.LEADING_LINES
import dev.stpark.namchin.core.composition.CompositionTemplate.SIDE_THIRD
import dev.stpark.namchin.core.guidance.Guidance
import dev.stpark.namchin.core.guidance.Guidance.Side
import dev.stpark.namchin.core.model.PersonPose
import dev.stpark.namchin.core.testutil.STANDING
import dev.stpark.namchin.core.testutil.inFrame
import dev.stpark.namchin.core.testutil.pose
import dev.stpark.namchin.core.testutil.transformed
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class CompositionTemplateTest {

    /** STANDING(어깨 0.30, 골반 0.55, 발목 0.90)을 키우고 옮겨 각 틀의 자리에 맞춘 인물 */
    private val fullBody = pose(STANDING.transformed(scale = 1.17f, dx = -0.085f, dy = -0.10f)).inFrame()
    private val halfBody = pose(STANDING.transformed(scale = 2f, dx = -0.5f, dy = -0.18f)).inFrame()
    private fun small(x: Float) = pose(STANDING.transformed(scale = 0.6f, dx = x - 0.3f, dy = 0.358f)).inFrame()

    /** 어깨 중점 (x, y), 어깨~골반 길이 torso인 정면 인물 (STANDING: 어깨 (0.5, 0.3), 몸통 0.25) */
    private fun at(x: Float, y: Float, torso: Float): PersonPose {
        val scale = torso / 0.25f
        return pose(STANDING.transformed(scale, dx = x - 0.5f * scale, dy = y - 0.3f * scale)).inFrame()
    }

    private fun end(arc: Arc): Pair<Float, Float> {
        val angle = Math.toRadians((arc.startDegrees + arc.sweepDegrees).toDouble())
        val cx = (arc.left + arc.right) / 2
        val cy = (arc.top + arc.bottom) / 2
        return (cx + (arc.right - arc.left) / 2 * cos(angle).toFloat()) to (cy + (arc.bottom - arc.top) / 2 * sin(angle).toFloat())
    }

    private fun start(arc: Arc): Pair<Float, Float> = end(arc.copy(sweepDegrees = 0f))

    private fun near(a: Pair<Float, Float>, b: Pair<Float, Float>, tolerance: Float) =
        abs(a.first - b.first) <= tolerance && abs(a.second - b.second) <= tolerance

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

    @Test
    fun `side third puts her body middle on the lower third line`() {
        val guide = TemplateGuide.of(SIDE_THIRD, Side.LEFT)

        assertEquals(2f / 3, (guide.headTop + guide.bottom) / 2, 0.01f)
        assertTrue(guide.lines.any { it.y1 == it.y2 && it.y1 == 2f / 3 }, "교차점이 보이도록 가로 1/3 선")
    }

    @Test
    fun `golden spiral fits a small person on the spiral eye`() {
        val eye = GoldenSpiral.EYE
        val result = TemplateCoach.analyze(GOLDEN_SPIRAL, at(eye, eye - 0.1f, torso = 0.1f), previousSide = Side.RIGHT)

        assertEquals(Side.RIGHT, result.side)
        assertTrue(result.fits, "${result.cues}")
    }

    @Test
    fun `golden spiral flips to the left eye when she stands on the left`() {
        val eye = 1f - GoldenSpiral.EYE
        val result = TemplateCoach.analyze(GOLDEN_SPIRAL, at(eye, GoldenSpiral.EYE - 0.1f, torso = 0.1f))

        assertEquals(Side.LEFT, result.side)
        assertTrue(result.fits, "${result.cues}")
    }

    @Test
    fun `golden spiral is drawn from the frame corner and winds into her spot`() {
        listOf(Side.RIGHT, Side.LEFT, null).forEach { side ->
            val guide = TemplateGuide.of(GOLDEN_SPIRAL, side)
            val target = GOLDEN_SPIRAL.target(side)
            val hip = target.anchorX to target.anchorY + target.torso!!

            assertTrue(guide.arcs.size >= 6, "$side")
            val first = start(guide.arcs.first())
            assertTrue(near(first, (if (side == Side.RIGHT) 1f else 0f) to 0f, 1e-3f), "$side 시작 $first")
            assertEquals(target.anchorX, guide.centerX, "$side 나선과 인물 윤곽이 같은 쪽")
            guide.arcs.zipWithNext().forEach { (a, b) -> assertTrue(near(end(a), start(b), 1e-3f), "$side 이어짐") }
            assertTrue(near(end(guide.arcs.last()), hip, 0.03f), "$side 끝 ${end(guide.arcs.last())}")
        }
    }

    @Test
    fun `diagonal template wants her small on the slope with sky above`() {
        val target = DIAGONAL_SPACE.target(Side.LEFT)
        val result = TemplateCoach.analyze(DIAGONAL_SPACE, at(target.anchorX, target.anchorY, target.torso!!))

        assertEquals(Side.LEFT, result.side)
        assertTrue(result.fits, "${result.cues}")
        assertTrue(target.anchorY > 0.5f, "위쪽 절반은 하늘(여백)")
        assertTrue(Guidance.MoveBack in TemplateCoach.analyze(DIAGONAL_SPACE, at(0.25f, 0.4f, torso = 0.2f)).cues)
    }

    @Test
    fun `diagonal line runs under her feet and slopes down away from her`() {
        listOf(Side.LEFT, Side.RIGHT).forEach { side ->
            val target = DIAGONAL_SPACE.target(side)
            val feet = target.anchorY + target.torso!! * CompositionTemplate.FEET_PER_TORSO
            val line = TemplateGuide.of(DIAGONAL_SPACE, side).lines.single()
            val yAtHer = line.y1 + (line.y2 - line.y1) * (target.anchorX - line.x1) / (line.x2 - line.x1)

            assertEquals(feet, yAtHer, 0.02f, "$side")
            val farSide = if (side == Side.LEFT) line.y2 else line.y1
            assertTrue(farSide > feet + 0.05f, "$side 반대쪽으로 내려간다")
        }
    }

    @Test
    fun `leading lines meet where she stands in the middle`() {
        val target = LEADING_LINES.target(null)
        val feet = target.anchorY + target.torso!! * CompositionTemplate.FEET_PER_TORSO
        val lines = TemplateGuide.of(LEADING_LINES, null).lines

        assertEquals(0.5f, target.anchorX)
        assertEquals(2, lines.size)
        lines.forEach { assertEquals(1f, maxOf(it.y1, it.y2), "화면 아래 모서리에서 시작") }
        lines.forEach { assertEquals(feet, minOf(it.y1, it.y2), 0.02f, "발끝 높이에서 만남") }
        assertTrue(TemplateCoach.analyze(LEADING_LINES, at(0.5f, target.anchorY, target.torso!!)).fits)
        assertTrue(Guidance.PanRight in TemplateCoach.analyze(LEADING_LINES, at(0.65f, target.anchorY, target.torso!!)).cues)
    }

    @Test
    fun `small person templates never ask for feet at the bottom`() {
        listOf(GOLDEN_SPIRAL, DIAGONAL_SPACE, LEADING_LINES).forEach { template ->
            val t = template.target(Side.LEFT)
            val result = TemplateCoach.analyze(template, at(t.anchorX, t.anchorY, t.torso!!), previousSide = Side.LEFT)
            assertFalse(Guidance.FeetToBottom in result.cues, "$template ${result.cues}")
            assertFalse(Guidance.TooMuchHeadroom in result.cues, "$template ${result.cues}")
        }
    }
}
