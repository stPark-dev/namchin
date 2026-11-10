package dev.stpark.namchin.core.live

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ShakeTest {

    private fun run(vararg speeds: Float, from: Shake.State = Shake.State()) =
        speeds.fold(from) { s, w -> Shake.step(s, w) }

    @Test
    fun `steady hands are not shaky`() {
        assertFalse(run(0.02f, 0.05f, 0.03f, 0.04f).shaky)
    }

    @Test
    fun `sustained rotation is shaky`() {
        assertTrue(run(*FloatArray(10) { 0.8f }).shaky)
    }

    @Test
    fun `a single twitch is smoothed out`() {
        assertFalse(run(0.02f, 1.5f, 0.02f, 0.02f, 0.02f).shaky)
    }

    @Test
    fun `shaky state clears only once clearly steady`() {
        val shaky = run(*FloatArray(10) { 0.8f })

        assertTrue(run(*FloatArray(10) { 0.3f }, from = shaky).shaky)
        assertFalse(run(*FloatArray(20) { 0.05f }, from = shaky).shaky)
    }
}
