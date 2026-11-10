package dev.stpark.namchin.core.shutter

sealed interface ShutterState {
    data object Idle : ShutterState
    data class Aligning(val sinceMs: Long) : ShutterState
    data class Cooldown(val untilMs: Long) : ShutterState
}

data class ShutterStep(val state: ShutterState, val fire: Boolean)

/** 일치 상태가 HOLD_MS 동안 유지되면 한 번 발사하고 COOLDOWN_MS 동안 쉰다. */
object AutoShutter {
    const val HOLD_MS = 500L
    const val COOLDOWN_MS = 2_500L

    fun step(state: ShutterState, aligned: Boolean, enabled: Boolean, nowMs: Long): ShutterStep {
        if (!enabled) return ShutterStep(ShutterState.Idle, fire = false)
        return when (state) {
            is ShutterState.Cooldown ->
                if (nowMs < state.untilMs) ShutterStep(state, fire = false)
                else step(ShutterState.Idle, aligned, enabled, nowMs)
            ShutterState.Idle ->
                ShutterStep(if (aligned) ShutterState.Aligning(nowMs) else ShutterState.Idle, fire = false)
            is ShutterState.Aligning -> when {
                !aligned -> ShutterStep(ShutterState.Idle, fire = false)
                nowMs - state.sinceMs >= HOLD_MS ->
                    ShutterStep(ShutterState.Cooldown(nowMs + COOLDOWN_MS), fire = true)
                else -> ShutterStep(state, fire = false)
            }
        }
    }
}
