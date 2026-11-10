package dev.stpark.namchin.capture

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BurstTest {

    /** 결과를 순서대로 돌려주는 가짜 촬영 (null = 실패). 호출 횟수를 센다. */
    private class FakeShots(private val results: List<String?>) {
        var calls = 0
        val take: ((String?) -> Unit) -> Unit = { done -> done(results[calls++]) }
    }

    @Test
    fun `returns every saved shot in order when all succeed`() {
        val shots = FakeShots(listOf("a", "b", "c"))
        var saved: List<String>? = null

        Burst.run(count = 3, isActive = { true }, takeOne = shots.take) { saved = it }

        assertEquals(3, shots.calls)
        assertEquals(listOf("a", "b", "c"), saved)
    }

    @Test
    fun `stops at the first failure and keeps what was saved`() {
        val shots = FakeShots(listOf("a", null, "c"))
        var saved: List<String>? = null

        Burst.run(count = 3, isActive = { true }, takeOne = shots.take) { saved = it }

        assertEquals(2, shots.calls)
        assertEquals(listOf("a"), saved)
    }

    @Test
    fun `stops when the screen is no longer active`() {
        val shots = FakeShots(listOf("a", "b", "c"))
        var active = true
        var saved: List<String>? = null

        Burst.run(count = 3, isActive = { active }, takeOne = { done -> shots.take { active = false; done(it) } }) {
            saved = it
        }

        assertEquals(1, shots.calls)
        assertEquals(listOf("a"), saved)
    }
}
