package dev.stpark.namchin.core.live

import dev.stpark.namchin.core.live.LowLight.HOLD_MS
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LowLightTest {

    private data class F(val now: Long, val luma: Float, val exposure: Float? = null)

    private fun run(vararg frames: F, from: LowLight.State = LowLight.State()): LowLight.State =
        frames.fold(from) { s, f -> LowLight.step(s, f.luma, f.exposure, f.now) }

    /** start부터 간격 step으로 n프레임 같은 값. */
    private fun frames(start: Long, n: Int, luma: Float, exposure: Float? = null, step: Long = 100) =
        Array(n) { F(start + it * step, luma, exposure) }

    @Test
    fun `bright scene is not low light`() {
        assertFalse(run(*frames(0, 30, 120f)).lowLight)
    }

    @Test
    fun `dark scene becomes low light only after it stays dark`() {
        assertFalse(run(*frames(0, 5, 20f)).lowLight)
        assertTrue(run(*frames(0, 15, 20f)).lowLight)
    }

    @Test
    fun `a dip below the threshold that recovers before HOLD_MS does not switch`() {
        val dip = frames(0, 5, 120f) + frames(500, 5, 0f) + frames(1000, 20, 120f)

        assertFalse(run(*dip).lowLight)
    }

    @Test
    fun `between the enter and exit levels the current mode is kept`() {
        val night = run(*frames(0, 20, 20f))
        val day = run(*frames(0, 20, 120f))

        assertTrue(run(*frames(3000, 30, 45f), from = night).lowLight)
        assertFalse(run(*frames(3000, 30, 45f), from = day).lowLight)
    }

    @Test
    fun `clearly brighter for a while leaves low light`() {
        val night = run(*frames(0, 20, 20f))

        assertFalse(run(*frames(3000, 30, 120f), from = night).lowLight)
    }

    @Test
    fun `high ISO times exposure means low light even when auto exposure keeps the picture mid-grey`() {
        // 어두운 술집: 화면 평균은 70이지만 카메라가 ISO 3200 · 1/15초로 버티는 중
        assertTrue(run(*frames(0, 20, 70f, exposure = 3200f / 15f)).lowLight)
        // 밝은 실내: ISO 400 · 1/60초
        assertFalse(run(*frames(0, 20, 70f, exposure = 400f / 60f)).lowLight)
    }

    @Test
    fun `a pause in frames restarts the hold instead of switching at once`() {
        val pending = run(*frames(0, 5, 0f)) // 어두워지기 시작 (아직 전환 전)

        val afterGap = run(F(10_000, 0f), from = pending)

        assertFalse(afterGap.lowLight)
    }
}
