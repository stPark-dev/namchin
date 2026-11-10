package dev.stpark.namchin.core.live

/**
 * 저조도(야간) 판정.
 * 자동 노출이 화면을 중간 밝기로 끌어올리므로 화면 밝기만으로는 약하다 →
 * 카메라가 실제로 쓰는 감도×노출시간(ISO·초)이 크면 어둡다고 본다. 둘 중 하나라도 어두우면 야간.
 * 그림자 한 번에 바뀌지 않게: 평활 + 진입/해제 기준 분리 + HOLD_MS 유지 조건.
 */
object LowLight {
    const val ENTER_LUMA = 35f
    const val EXIT_LUMA = 55f

    /** ISO × 노출 시간(초). 밝은 실내 ISO 400·1/60 ≈ 7, 어두운 술집 ISO 3200·1/15 ≈ 213. */
    const val ENTER_EXPOSURE = 40f
    const val EXIT_EXPOSURE = 20f
    const val HOLD_MS = 1_000L

    /** 프레임이 이보다 오래 끊겼다 다시 오면(화면 이탈 등) 유지 시간을 처음부터 센다. */
    const val MAX_GAP_MS = 500L
    private const val SMOOTHING = 0.3f

    data class State(
        val ema: Float? = null,
        val lowLight: Boolean = false,
        val pendingSinceMs: Long? = null,
        val lastFrameMs: Long? = null,
    )

    /** @param exposure ISO × 노출 시간(초). 모르면 null (밝기만으로 판단) */
    fun step(state: State, luma: Float, exposure: Float?, nowMs: Long): State {
        val gap = state.lastFrameMs?.let { nowMs - it > MAX_GAP_MS } ?: false
        val pendingSince = if (gap) null else state.pendingSinceMs
        val ema = state.ema?.let { it + SMOOTHING * (luma - it) } ?: luma
        val darkLuma = if (state.lowLight) ema <= EXIT_LUMA else ema < ENTER_LUMA
        val darkExposure = exposure?.let { if (state.lowLight) it >= EXIT_EXPOSURE else it > ENTER_EXPOSURE } ?: false
        val want = darkLuma || darkExposure
        return when {
            want == state.lowLight -> State(ema, state.lowLight, null, nowMs)
            pendingSince == null -> State(ema, state.lowLight, nowMs, nowMs)
            nowMs - pendingSince >= HOLD_MS -> State(ema, want, null, nowMs)
            else -> State(ema, state.lowLight, pendingSince, nowMs)
        }
    }
}
