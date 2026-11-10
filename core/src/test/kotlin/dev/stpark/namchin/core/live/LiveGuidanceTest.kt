package dev.stpark.namchin.core.live

import dev.stpark.namchin.core.guidance.Guidance
import dev.stpark.namchin.core.guidance.GuidancePrioritizer.STABLE_MS
import dev.stpark.namchin.core.guidance.Rotation
import dev.stpark.namchin.core.model.Joint
import dev.stpark.namchin.core.shutter.AutoShutter
import dev.stpark.namchin.core.model.PersonPose
import dev.stpark.namchin.core.model.Point
import dev.stpark.namchin.core.testutil.STANDING
import dev.stpark.namchin.core.testutil.mirrored
import dev.stpark.namchin.core.testutil.pose
import dev.stpark.namchin.core.testutil.transformed
import dev.stpark.namchin.core.testutil.with
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LiveGuidanceTest {

    private val rightArmRaised = STANDING.with(
        Joint.RIGHT_ELBOW to Point(0.35f, 0.30f),
        Joint.RIGHT_WRIST to Point(0.25f, 0.30f),
    )
    private val reference = pose(rightArmRaised)

    private fun step(
        current: PersonPose?,
        reference: PersonPose = this.reference,
        state: LiveState = LiveState(),
        roll: Float? = 0f,
        now: Long = 0,
        poseEnabled: Boolean = true,
        autoShutter: Boolean = false,
    ) = LiveGuidance.step(state, LiveInput(reference, current, roll, poseEnabled, now, autoShutter))

    @Test
    fun `matching person is aligned with nothing to say`() {
        val (_, out) = step(pose(rightArmRaised))

        assertTrue(out.aligned)
        assertNull(out.display)
        assertEquals(1f, out.match!!.score, 1e-4f)
    }

    @Test
    fun `missing person asks to get her in frame`() {
        val (_, out) = step(current = null)

        assertFalse(out.aligned)
        assertEquals(Guidance.NoPerson, out.display)
    }

    @Test
    fun `framing cue is displayed at once and announced after it is stable`() {
        val tooSmall = pose(rightArmRaised.transformed(scale = 0.5f, dx = 0.25f, dy = 0.15f))

        val (s1, first) = step(tooSmall, now = 0)
        val (_, later) = step(tooSmall, state = s1, now = STABLE_MS)

        assertEquals(Guidance.MoveCloser, first.display)
        assertNull(first.announce)
        assertEquals(Guidance.MoveCloser, later.announce)
    }

    @Test
    fun `wrong arm gives a pose cue`() {
        val (_, out) = step(pose(STANDING))

        assertFalse(out.aligned)
        assertTrue(out.display is Guidance.Pose)
    }

    @Test
    fun `mirror choice is remembered for the next frame`() {
        val (state, out) = step(pose(rightArmRaised.mirrored()))

        assertTrue(out.match!!.mirrored)
        assertTrue(state.preferMirrored)
    }

    @Test
    fun `previous cues feed hysteresis`() {
        val (s1, first) = step(pose(rightArmRaised), roll = 5f)
        val (_, second) = step(pose(rightArmRaised), state = s1, roll = 2.5f)

        assertEquals(Guidance.Level(Rotation.COUNTERCLOCKWISE), first.display)
        assertEquals(Guidance.Level(Rotation.COUNTERCLOCKWISE), second.display)
    }

    @Test
    fun `pose can be disabled for night mode`() {
        val (_, out) = step(pose(STANDING), poseEnabled = false)

        assertTrue(out.aligned)
        assertNull(out.match)
    }

    @Test
    fun `level is checked even when no person is in frame`() {
        val (_, out) = step(current = null, roll = 8f)

        assertEquals(Guidance.NoPerson, out.display)
        assertTrue(Guidance.Level(Rotation.COUNTERCLOCKWISE) in out.cues)
    }

    @Test
    fun `unknown roll gives no level cue`() {
        val (_, out) = step(pose(rightArmRaised), roll = null)

        assertTrue(out.aligned)
        assertTrue(out.cues.isEmpty())
    }

    @Test
    fun `reference without frame still checks level`() {
        val noFrameRef = pose(rightArmRaised.filterKeys { "SHOULDER" !in it.name && it != Joint.NOSE })

        val (_, out) = step(pose(rightArmRaised), reference = noFrameRef, roll = 8f)

        assertEquals(Guidance.Level(Rotation.COUNTERCLOCKWISE), out.display)
    }

    @Test
    fun `reference with neither frame nor comparable pose is never aligned`() {
        val legsOnlyRef = pose(rightArmRaised.filterKeys { "HIP" in it.name || "KNEE" in it.name })

        val (_, out) = step(pose(rightArmRaised), reference = legsOnlyRef)

        assertFalse(out.aligned)
    }

    @Test
    fun `basic mode with a level phone has nothing to say`() {
        val (_, out) = LiveGuidance.stepBasic(LiveState(), pose(STANDING), rollDegrees = 1f, nowMs = 0)

        assertNull(out.display)
        assertFalse(out.aligned)
        assertNull(out.match)
    }

    @Test
    fun `basic mode coaches level and announces once stable`() {
        val (s1, first) = LiveGuidance.stepBasic(LiveState(), pose(STANDING), rollDegrees = 6f, nowMs = 0)
        val (_, later) = LiveGuidance.stepBasic(s1, pose(STANDING), rollDegrees = 6f, nowMs = STABLE_MS)

        assertEquals(Guidance.Level(Rotation.COUNTERCLOCKWISE), first.display)
        assertEquals(Guidance.Level(Rotation.COUNTERCLOCKWISE), later.announce)
    }

    @Test
    fun `basic mode does not nag when nobody is in frame`() {
        val (_, out) = LiveGuidance.stepBasic(LiveState(), current = null, rollDegrees = 0f, nowMs = 0)

        assertTrue(out.cues.isEmpty())
    }

    @Test
    fun `basic mode adds composition coaching after level`() {
        val small = pose(STANDING.transformed(scale = 0.8f, dx = 0.1f, dy = 0f))

        val (_, out) = LiveGuidance.stepBasic(LiveState(), small, rollDegrees = 6f, nowMs = 0)

        assertEquals(Guidance.Level(Rotation.COUNTERCLOCKWISE), out.display)
        assertTrue(Guidance.FeetToBottom in out.cues)
    }

    @Test
    fun `auto shutter fires once alignment holds`() {
        val matching = pose(rightArmRaised)

        val (s1, first) = step(matching, now = 0, autoShutter = true)
        val (_, held) = step(matching, state = s1, now = AutoShutter.HOLD_MS, autoShutter = true)

        assertFalse(first.fire)
        assertTrue(held.fire)
    }

    @Test
    fun `auto shutter stays quiet when switched off`() {
        val matching = pose(rightArmRaised)

        val (s1, _) = step(matching, now = 0, autoShutter = false)
        val (_, held) = step(matching, state = s1, now = AutoShutter.HOLD_MS, autoShutter = false)

        assertFalse(held.fire)
    }

    @Test
    fun `a manual shot starts the shutter cooldown`() {
        val matching = pose(rightArmRaised)
        val cooled = LiveGuidance.afterManualShot(LiveState(), nowMs = 0)

        val (s1, _) = step(matching, state = cooled, now = 10, autoShutter = true)
        val (_, held) = step(matching, state = s1, now = 10 + AutoShutter.HOLD_MS, autoShutter = true)

        assertFalse(held.fire)
    }

    @Test
    fun `basic mode never fires`() {
        val (_, out) = LiveGuidance.stepBasic(LiveState(), pose(STANDING), rollDegrees = 0f, nowMs = 0)

        assertFalse(out.fire)
    }

    @Test
    fun `a fire that could not be taken is rearmed`() {
        val matching = pose(rightArmRaised)
        val (s1, _) = step(matching, now = 0, autoShutter = true)
        val (s2, fired) = step(matching, state = s1, now = AutoShutter.HOLD_MS, autoShutter = true)
        assertTrue(fired.fire)

        val rearmed = LiveGuidance.cancelFire(s2)
        val (s3, _) = step(matching, state = rearmed, now = AutoShutter.HOLD_MS + 10, autoShutter = true)
        val (_, again) = step(matching, state = s3, now = 2 * AutoShutter.HOLD_MS + 10, autoShutter = true)

        assertTrue(again.fire)
    }

    @Test
    fun `in low light pose and auto shutter are off`() {
        // 레퍼런스와 다른 포즈여도 야간엔 포즈 안내·자동 셔터가 없다
        val night = { s: LiveState, now: Long ->
            LiveGuidance.step(s, LiveInput(reference, pose(STANDING), 0f, poseEnabled = true, nowMs = now, autoShutter = true, lowLight = true))
        }

        val (s1, first) = night(LiveState(), 0)
        val (_, held) = night(s1, AutoShutter.HOLD_MS)

        assertNull(first.match)
        assertFalse(held.fire)
        // 포즈를 비교하지 않으므로 "맞음"(초록·진동·'그대로 유지')도 내지 않는다
        assertFalse(held.aligned)
        assertFalse(first.cues.any { it is Guidance.Pose })
    }

    @Test
    fun `shaking in low light asks to hold steady first`() {
        val (_, out) = LiveGuidance.step(
            LiveState(),
            LiveInput(reference, pose(rightArmRaised), 6f, poseEnabled = true, nowMs = 0, lowLight = true, shaky = true),
        )

        assertEquals(Guidance.HoldSteady, out.display)
    }

    @Test
    fun `basic mode also warns about shaking in low light`() {
        val (_, out) = LiveGuidance.stepBasic(LiveState(), pose(STANDING), 0f, 0, lowLight = true, shaky = true)

        assertEquals(Guidance.HoldSteady, out.display)
    }

    @Test
    fun `shaking in good light is not nagged about`() {
        val (_, out) = LiveGuidance.stepBasic(LiveState(), pose(STANDING), 0f, 0, lowLight = false, shaky = true)

        assertFalse(Guidance.HoldSteady in out.cues)
    }

    @Test
    fun `pose cue says which way to move`() {
        val (_, out) = step(pose(STANDING)) // 레퍼런스는 오른팔 수평, 지금은 내림

        val cue = out.display as Guidance.Pose
        assertEquals(dev.stpark.namchin.core.pose.PoseDirection.Direction.RAISE, cue.direction)
    }
}
