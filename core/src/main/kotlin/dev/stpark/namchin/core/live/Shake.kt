package dev.stpark.namchin.core.live

import kotlin.math.exp

/**
 * 자이로 각속도(rad/s) → 손떨림 판정.
 * 손떨림은 0.01~0.1 rad/s 수준(1/15초 노출에서 0.1이면 약 10px 번짐).
 * 크게 돌리는 건 구도를 잡는 의도적 동작이라 세지 않는다.
 * 평활은 샘플 수가 아니라 실제 경과 시간 기준(센서 주기가 기기마다 달라도 같게).
 */
object Shake {
    const val ENTER = 0.08f
    const val EXIT = 0.04f

    /** 이보다 빠르면 의도적으로 폰을 돌리는 중 → 무시. */
    const val PAN = 0.6f
    private const val TAU_MS = 150.0
    private const val FIRST_DT_MS = 20L

    data class State(val ema: Float = 0f, val shaky: Boolean = false, val lastMs: Long? = null)

    fun step(state: State, angularSpeed: Float, nowMs: Long): State {
        if (angularSpeed > PAN) return state.copy(lastMs = nowMs)
        val dt = state.lastMs?.let { (nowMs - it).coerceAtLeast(0) } ?: FIRST_DT_MS
        val alpha = (1 - exp(-dt / TAU_MS)).toFloat()
        val ema = state.ema + alpha * (angularSpeed - state.ema)
        val shaky = if (state.shaky) ema > EXIT else ema > ENTER
        return State(ema, shaky, nowMs)
    }
}
