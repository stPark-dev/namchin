package dev.stpark.namchin.core.live

/** 자이로 각속도 크기(rad/s) → 손떨림 판정. 한 번 움찔한 건 평활로 무시. */
object Shake {
    const val ENTER = 0.5f
    const val EXIT = 0.15f
    private const val SMOOTHING = 0.3f

    data class State(val ema: Float = 0f, val shaky: Boolean = false)

    fun step(state: State, angularSpeed: Float): State {
        val ema = state.ema + SMOOTHING * (angularSpeed - state.ema)
        val shaky = if (state.shaky) ema > EXIT else ema > ENTER
        return State(ema, shaky)
    }
}
