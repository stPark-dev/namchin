package dev.stpark.namchin.camera

import dev.stpark.namchin.R
import dev.stpark.namchin.core.guidance.Guidance
import dev.stpark.namchin.core.guidance.Rotation
import dev.stpark.namchin.core.model.BodyPart
import dev.stpark.namchin.core.pose.PoseDirection.Direction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class GuidanceTextTest {

    private val framingCues: List<Guidance> = listOf(
        Guidance.NoPerson,
        Guidance.HoldSteady,
        Guidance.Level(Rotation.CLOCKWISE),
        Guidance.Level(Rotation.COUNTERCLOCKWISE),
        Guidance.MoveCloser,
        Guidance.MoveBack,
        Guidance.TiltUp,
        Guidance.TiltDown,
        Guidance.PanLeft,
        Guidance.PanRight,
        Guidance.HeadCut,
        Guidance.FeetToBottom,
        Guidance.TooMuchHeadroom,
    ) + Guidance.CutPoint.entries.map { Guidance.CutAtJoint(it) } +
        Guidance.Side.entries.map { Guidance.GazeSpace(it) }

    private val poseCues: List<Guidance> =
        BodyPart.entries.flatMap { part -> (Direction.entries + null).map { Guidance.Pose(part, it) } }

    @Test
    fun `every framing cue has its own message`() {
        val specs = framingCues.map { GuidanceText.spec(it) }

        assertEquals(specs.size, specs.toSet().size)
    }

    @Test
    fun `pose cue with a direction combines body part and movement`() {
        val spec = GuidanceText.spec(Guidance.Pose(BodyPart.RIGHT_UPPER_ARM, Direction.RAISE))

        assertEquals(R.string.guide_pose_directed, spec.res)
        assertEquals(listOf(R.string.part_right_arm, R.string.move_raise), spec.args)
    }

    @Test
    fun `pose cue without a direction falls back to naming the part`() {
        assertEquals(GuidanceText.TextSpec(R.string.guide_pose_left_forearm), GuidanceText.spec(Guidance.Pose(BodyPart.LEFT_FOREARM)))
    }

    @Test
    fun `different directions on the same part read differently`() {
        val raise = GuidanceText.spec(Guidance.Pose(BodyPart.LEFT_THIGH, Direction.RAISE))
        val bend = GuidanceText.spec(Guidance.Pose(BodyPart.LEFT_THIGH, Direction.BEND))

        assertNotEquals(raise, bend)
    }

    @Test
    fun `every cue has a reason`() {
        (framingCues + poseCues).forEach { GuidanceText.reason(it) }
    }

    @Test
    fun `reasons explain the rule, not repeat the instruction`() {
        assertNotEquals(GuidanceText.spec(Guidance.FeetToBottom).res, GuidanceText.reason(Guidance.FeetToBottom))
    }
}
