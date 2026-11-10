package dev.stpark.namchin.core.live

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ShakeTest {

    /** 20ms 간격 센서 샘플. */
    private fun run(vararg speeds: Float, from: Shake.State = Shake.State(), stepMs: Long = 20): Shake.State {
        var t = (from.lastMs ?: 0L)
        return speeds.fold(from) { s, w -> t += stepMs; Shake.step(s, w, t) }
    }

    private fun repeat(n: Int, w: Float) = FloatArray(n) { w }

    @Test
    fun `steady hands are not shaky`() {
        assertFalse(run(*repeat(50, 0.02f)).shaky)
    }

    @Test
    fun `sustained hand tremor is shaky`() {
        // 0.1 rad/s 수준의 떨림이면 1/15초 노출에서 약 10px 번진다
        assertTrue(run(*repeat(50, 0.12f)).shaky)
    }

    @Test
    fun `deliberate panning is not counted as shake`() {
        assertFalse(run(*repeat(50, 1.0f)).shaky)
    }

    @Test
    fun `a single twitch is smoothed out`() {
        assertFalse(run(*(repeat(20, 0.02f) + floatArrayOf(0.4f) + repeat(5, 0.02f))).shaky)
    }

    @Test
    fun `the same duration of tremor counts the same regardless of sensor rate`() {
        // 300ms 동안의 떨림: 20ms 간격 15개 = 5ms 간격 60개
        assertTrue(run(*repeat(15, 0.12f), stepMs = 20).shaky == run(*repeat(60, 0.12f), stepMs = 5).shaky)
    }

    @Test
    fun `shaky state clears only once clearly steady`() {
        val shaky = run(*repeat(50, 0.12f))

        assertTrue(run(*repeat(50, 0.06f), from = shaky).shaky)
        assertFalse(run(*repeat(50, 0.01f), from = shaky).shaky)
    }
}
