package dev.stpark.namchin.core.shutter

sealed interface ShutterState {
    data object Idle : ShutterState
    data class Aligning(val sinceMs: Long, val lastAlignedMs: Long) : ShutterState
    data class Cooldown(val untilMs: Long) : ShutterState

    /** 발사 후 같은 포즈가 계속 유지되는 중. 한 번 풀려야 다시 발사할 수 있다. */
    data class AwaitingRelease(val misalignedSinceMs: Long? = null) : ShutterState
}

data class ShutterStep(val state: ShutterState, val fire: Boolean)

/**
 * 일치 상태가 HOLD_MS 동안 유지되면 한 번 발사하고 COOLDOWN_MS 동안 쉰다.
 * GRACE_MS 이하의 짧은 끊김(인식 노이즈)은 무시한다.
 */
object AutoShutter {
    const val HOLD_MS = 500L
    const val GRACE_MS = 150L
    const val COOLDOWN_MS = 2_500L

    fun afterManualShot(nowMs: Long): ShutterState = ShutterState.Cooldown(nowMs + COOLDOWN_MS)

    fun step(state: ShutterState, aligned: Boolean, enabled: Boolean, nowMs: Long): ShutterStep {
        if (!enabled) return ShutterStep(state as? ShutterState.Cooldown ?: ShutterState.Idle, fire = false)
        val next = when (state) {
            ShutterState.Idle ->
                if (aligned) ShutterState.Aligning(nowMs, nowMs) else state
            is ShutterState.Aligning -> when {
                aligned && nowMs - state.sinceMs >= HOLD_MS ->
                    return ShutterStep(ShutterState.Cooldown(nowMs + COOLDOWN_MS), fire = true)
                aligned -> state.copy(lastAlignedMs = nowMs)
                nowMs - state.lastAlignedMs > GRACE_MS -> ShutterState.Idle
                else -> state
            }
            is ShutterState.Cooldown ->
                if (nowMs < state.untilMs) state
                else return step(ShutterState.AwaitingRelease(), aligned, enabled = true, nowMs)
            is ShutterState.AwaitingRelease -> when {
                aligned -> ShutterState.AwaitingRelease()
                state.misalignedSinceMs == null -> ShutterState.AwaitingRelease(nowMs)
                nowMs - state.misalignedSinceMs >= GRACE_MS -> ShutterState.Idle
                else -> state
            }
        }
        return ShutterStep(next, fire = false)
    }
}
