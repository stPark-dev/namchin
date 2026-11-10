package dev.stpark.namchin.core.shutter

import dev.stpark.namchin.core.shutter.AutoShutter.COOLDOWN_MS
import dev.stpark.namchin.core.shutter.AutoShutter.HOLD_MS
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AutoShutterTest {

    private fun run(vararg inputs: Pair<Long, Boolean>, enabled: Boolean = true): List<ShutterStep> {
        var state: ShutterState = ShutterState.Idle
        return inputs.map { (now, aligned) ->
            AutoShutter.step(state, aligned, enabled, now).also { state = it.state }
        }
    }

    @Test
    fun `stays idle while not aligned`() {
        val steps = run(0L to false, 1000L to false)

        assertTrue(steps.all { it.state == ShutterState.Idle && !it.fire })
    }

    @Test
    fun `fires after alignment holds for HOLD_MS`() {
        val steps = run(0L to true, HOLD_MS - 1 to true, HOLD_MS to true)

        assertEquals(listOf(false, false, true), steps.map { it.fire })
        assertEquals(ShutterState.Cooldown(untilMs = HOLD_MS + COOLDOWN_MS), steps.last().state)
    }

    @Test
    fun `losing alignment resets the hold timer`() {
        val steps = run(0L to true, 300L to false, 400L to true, 400L + HOLD_MS - 1 to true)

        assertFalse(steps.any { it.fire })
    }

    @Test
    fun `does not fire again during cooldown`() {
        val fireAt = HOLD_MS
        val steps = run(0L to true, fireAt to true, fireAt + 100 to true, fireAt + COOLDOWN_MS - 1 to true)

        assertEquals(1, steps.count { it.fire })
    }

    @Test
    fun `can fire again after cooldown and a new hold`() {
        val fireAt = HOLD_MS
        val afterCooldown = fireAt + COOLDOWN_MS
        val steps = run(
            0L to true,
            fireAt to true,
            afterCooldown to true,
            afterCooldown + HOLD_MS to true,
        )

        assertEquals(listOf(false, true, false, true), steps.map { it.fire })
    }

    @Test
    fun `disabled shutter never fires and resets to idle`() {
        val steps = run(0L to true, HOLD_MS to true, enabled = false)

        assertTrue(steps.all { it.state == ShutterState.Idle && !it.fire })
    }
}
