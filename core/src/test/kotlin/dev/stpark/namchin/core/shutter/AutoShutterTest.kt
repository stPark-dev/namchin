package dev.stpark.namchin.core.shutter

import dev.stpark.namchin.core.shutter.AutoShutter.COOLDOWN_MS
import dev.stpark.namchin.core.shutter.AutoShutter.GRACE_MS
import dev.stpark.namchin.core.shutter.AutoShutter.HOLD_MS
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AutoShutterTest {

    private data class Frame(val now: Long, val aligned: Boolean, val enabled: Boolean = true)

    private fun run(vararg frames: Frame, from: ShutterState = ShutterState.Idle): List<ShutterStep> {
        var state = from
        return frames.map { f ->
            AutoShutter.step(state, f.aligned, f.enabled, f.now).also { state = it.state }
        }
    }

    private fun on(now: Long) = Frame(now, true)
    private fun off(now: Long) = Frame(now, false)

    @Test
    fun `stays idle while not aligned`() {
        assertTrue(run(off(0), off(1000)).all { it.state == ShutterState.Idle && !it.fire })
    }

    @Test
    fun `fires after alignment holds for HOLD_MS`() {
        val steps = run(on(0), on(HOLD_MS - 1), on(HOLD_MS))

        assertEquals(listOf(false, false, true), steps.map { it.fire })
        assertEquals(ShutterState.Cooldown(untilMs = HOLD_MS + COOLDOWN_MS), steps.last().state)
    }

    @Test
    fun `a brief dropout within GRACE_MS does not reset the hold`() {
        val steps = run(on(0), on(100), off(100 + GRACE_MS), on(HOLD_MS))

        assertTrue(steps.last().fire)
    }

    @Test
    fun `a longer dropout resets the hold`() {
        val steps = run(on(0), off(100), off(GRACE_MS + 1), on(400), on(400 + HOLD_MS - 1))

        assertFalse(steps.any { it.fire })
    }

    @Test
    fun `does not fire again during cooldown`() {
        val steps = run(on(0), on(HOLD_MS), on(HOLD_MS + 100), on(HOLD_MS + COOLDOWN_MS - 1))

        assertEquals(1, steps.count { it.fire })
    }

    @Test
    fun `after cooldown the pose must be released before firing again`() {
        val c = HOLD_MS + COOLDOWN_MS
        val held = run(on(0), on(HOLD_MS), on(c), on(c + HOLD_MS), on(c + 3 * HOLD_MS))
        assertEquals(1, held.count { it.fire })

        val released = run(
            on(0), on(HOLD_MS), on(c),
            off(c + 100), off(c + 100 + GRACE_MS),
            on(c + 400), on(c + 400 + HOLD_MS),
        )
        assertEquals(listOf(false, true, false, false, false, false, true), released.map { it.fire })
    }

    @Test
    fun `a single misaligned frame does not count as a release`() {
        val c = HOLD_MS + COOLDOWN_MS
        val steps = run(on(0), on(HOLD_MS), on(c), off(c + 50), on(c + 100), on(c + 100 + HOLD_MS))

        assertEquals(1, steps.count { it.fire })
    }

    @Test
    fun `disabled shutter never fires`() {
        val steps = run(Frame(0, true, enabled = false), Frame(HOLD_MS, true, enabled = false))

        assertTrue(steps.all { it.state == ShutterState.Idle && !it.fire })
    }

    @Test
    fun `toggling off and on keeps the cooldown`() {
        val cooling = ShutterState.Cooldown(untilMs = 1000)
        val steps = run(Frame(100, true, enabled = false), on(200), on(800), from = cooling)

        assertTrue(steps.all { it.state == cooling && !it.fire })
    }

    @Test
    fun `a manual shot starts a cooldown`() {
        assertEquals(ShutterState.Cooldown(500 + COOLDOWN_MS), AutoShutter.afterManualShot(nowMs = 500))
    }

    @Test
    fun `pausing does not forget that the pose must be released first`() {
        val waiting = ShutterState.AwaitingRelease()

        val steps = run(Frame(0, true, enabled = false), on(100), on(100 + HOLD_MS), from = waiting)

        assertTrue(steps.none { it.fire })
    }
}
