package dev.stpark.namchin.core.live

/**
 * @property since 조건이 시작된 시각
 * @property clearSince 조건이 사라진 시각 (잠깐 끊긴 건지 보려고)
 * @property spent 이번 조건에서 이미 한 번 보여 줌
 */
data class HintTimer(val since: Long? = null, val clearSince: Long? = null, val spent: Boolean = false)

/**
 * 한 번만 하는 한마디 (포즈·빛 팁). 고칠지 말지는 둘의 선택이라 계속 재촉하지 않는다:
 * 조건이 DELAY_MS 이어지면 SHOW_MS 동안만 보여 주고, 조건이 CLEAR_MS 넘게 풀렸다가 다시 생기면 또 한 번.
 */
object OneShotHint {
    const val DELAY_MS = 1_500L
    /** 같은 안내 재발화 간격(STABLE_MS + REPEAT_COOLDOWN_MS)보다 짧아야 한 번만 말한다 */
    const val SHOW_MS = 3_000L
    const val CLEAR_MS = 1_000L

    /** @return 다음 상태와 이번 프레임에 보여 줄지 */
    fun step(timer: HintTimer, condition: Boolean, nowMs: Long): Pair<HintTimer, Boolean> {
        val since = timer.since
        val clearing = when {
            condition -> null
            since == null -> return HintTimer() to false
            else -> timer.clearSince ?: nowMs
        }
        if (clearing != null && nowMs - clearing >= CLEAR_MS) return HintTimer() to false
        val start = since ?: nowMs
        val elapsed = nowMs - start
        // 조건이 풀린 동안엔 이미 보이던 것만 이어 보여 준다 (풀린 뒤에 처음 뜨지 않게)
        val wasShowing = clearing != null && clearing - start >= DELAY_MS
        val show = !timer.spent && elapsed in DELAY_MS until DELAY_MS + SHOW_MS && (condition || wasShowing)
        return HintTimer(start, clearing, timer.spent || elapsed >= DELAY_MS + SHOW_MS) to show
    }
}
