package dev.stpark.namchin.core.live

import dev.stpark.namchin.core.live.OneShotHint.CLEAR_MS
import dev.stpark.namchin.core.live.OneShotHint.DELAY_MS
import dev.stpark.namchin.core.live.OneShotHint.SHOW_MS
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OneShotHintTest {

    /** 조건을 times 시각마다 넣어 보고 마지막 표시 여부. */
    private fun run(vararg frames: Pair<Long, Boolean>): Pair<HintTimer, Boolean> =
        frames.fold(HintTimer() to false) { (timer, _), (now, condition) -> OneShotHint.step(timer, condition, now) }

    @Test
    fun `a brief condition is not shown`() {
        assertFalse(run(0L to true, DELAY_MS - 1 to true).second)
    }

    @Test
    fun `a lasting condition is shown after the delay`() {
        assertTrue(run(0L to true, DELAY_MS to true).second)
    }

    @Test
    fun `the hint is shown only once for a while`() {
        assertFalse(run(0L to true, DELAY_MS to true, DELAY_MS + SHOW_MS to true).second)
        assertFalse(run(0L to true, DELAY_MS + SHOW_MS to true, 60_000L to true).second)
    }

    @Test
    fun `a short dropout does not restart or hide the hint`() {
        val (_, shown) = run(0L to true, DELAY_MS to true, DELAY_MS + 100 to false)

        assertTrue(shown)
    }

    @Test
    fun `after the condition clears the hint can come back`() {
        val spent = DELAY_MS + SHOW_MS
        val back = spent + CLEAR_MS + 10

        assertTrue(run(0L to true, spent to true, spent + 10 to false, back to false, back + 1 to true, back + 1 + DELAY_MS to true).second)
    }

    @Test
    fun `a condition that cleared before the delay is never shown`() {
        val (_, shown) = run(0L to true, DELAY_MS - 100 to true, DELAY_MS - 50 to false, DELAY_MS + 100 to false)

        assertFalse(shown)
    }
}
