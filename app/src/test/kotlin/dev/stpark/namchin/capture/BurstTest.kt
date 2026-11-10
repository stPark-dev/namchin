package dev.stpark.namchin.capture

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BurstTest {

    /** 결과를 순서대로 돌려주는 가짜 촬영. 호출 횟수를 센다. */
    private class FakeShots(private val results: List<Boolean>) {
        var calls = 0
        val take: ((Boolean) -> Unit) -> Unit = { done -> done(results[calls++]) }
    }

    @Test
    fun `takes every shot when all succeed`() {
        val shots = FakeShots(listOf(true, true, true))
        var saved = -1

        Burst.run(count = 3, isActive = { true }, takeOne = shots.take) { saved = it }

        assertEquals(3, shots.calls)
        assertEquals(3, saved)
    }

    @Test
    fun `stops at the first failure`() {
        val shots = FakeShots(listOf(true, false, true))
        var saved = -1

        Burst.run(count = 3, isActive = { true }, takeOne = shots.take) { saved = it }

        assertEquals(2, shots.calls)
        assertEquals(1, saved)
    }

    @Test
    fun `stops when the screen is no longer active`() {
        val shots = FakeShots(listOf(true, true, true))
        var active = true
        var saved = -1

        Burst.run(count = 3, isActive = { active }, takeOne = { done -> shots.take { active = false; done(it) } }) { saved = it }

        assertEquals(1, shots.calls)
        assertEquals(1, saved)
    }
}
