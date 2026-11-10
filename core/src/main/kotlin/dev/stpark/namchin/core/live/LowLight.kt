package dev.stpark.namchin.core.live

/**
 * 프레임 평균 밝기(0..255) → 저조도(야간) 판정.
 * 그림자 한 번에 바뀌지 않도록 평활 + 진입/해제 기준 분리 + HOLD_MS 유지 조건.
 */
object LowLight {
    const val ENTER_LUMA = 35f
    const val EXIT_LUMA = 55f
    const val HOLD_MS = 1_000L
    private const val SMOOTHING = 0.3f

    data class State(val ema: Float? = null, val lowLight: Boolean = false, val pendingSinceMs: Long? = null)

    fun step(state: State, luma: Float, nowMs: Long): State {
        val ema = state.ema?.let { it + SMOOTHING * (luma - it) } ?: luma
        val want = if (state.lowLight) ema <= EXIT_LUMA else ema < ENTER_LUMA
        return when {
            want == state.lowLight -> State(ema, state.lowLight, null)
            state.pendingSinceMs == null -> State(ema, state.lowLight, nowMs)
            nowMs - state.pendingSinceMs >= HOLD_MS -> State(ema, want, null)
            else -> state.copy(ema = ema)
        }
    }
}
