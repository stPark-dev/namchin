package dev.stpark.namchin.core.live

import dev.stpark.namchin.core.live.LowLight.HOLD_MS
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LowLightTest {

    /** (시각, 밝기) 순서대로 넣고 마지막 판정. */
    private fun run(vararg frames: Pair<Long, Float>, from: LowLight.State = LowLight.State()): LowLight.State =
        frames.fold(from) { s, (now, luma) -> LowLight.step(s, luma, now) }

    @Test
    fun `bright scene is not low light`() {
        assertFalse(run(0L to 120f, 500L to 120f, 2000L to 120f).lowLight)
    }

    @Test
    fun `dark scene becomes low light only after it stays dark`() {
        val brief = run(0L to 20f, 300L to 20f)
        val held = run(0L to 20f, 300L to 20f, HOLD_MS + 100 to 20f, HOLD_MS + 400 to 20f)

        assertFalse(brief.lowLight)
        assertTrue(held.lowLight)
    }

    @Test
    fun `a passing shadow does not switch modes`() {
        assertFalse(run(0L to 120f, 100L to 10f, 200L to 120f, 300L to 120f, 1500L to 120f).lowLight)
    }

    @Test
    fun `leaving low light needs clearly brighter light for a while`() {
        val night = run(0L to 20f, HOLD_MS to 20f, 2 * HOLD_MS to 20f)
        assertTrue(night.lowLight)

        // 기준 바로 위(경계)에서는 유지, 확실히 밝아진 채로 HOLD_MS가 지나야 해제
        assertTrue(run(3000L to 45f, 3000L + 2 * HOLD_MS to 45f, from = night).lowLight)
        assertFalse(run(3000L to 120f, 3200L to 120f, 3000L + 3 * HOLD_MS to 120f, 3000L + 4 * HOLD_MS to 120f, from = night).lowLight)
    }
}
