package dev.stpark.namchin.core.guidance

data class GuidanceState(
    val lastAnnounced: Guidance? = null,
    val lastAnnouncedAtMs: Long? = null,
)

/**
 * @property display 화면에 보여줄 안내 (항상 최우선 1개)
 * @property announce 이번에 음성·진동으로 낼 안내 (반복 방지로 null일 수 있음)
 */
data class GuidanceDecision(
    val display: Guidance?,
    val announce: Guidance?,
    val state: GuidanceState,
)

/** 한 번에 하나만, 같은 말 반복 없이 안내를 고른다. */
object GuidancePrioritizer {
    /** 서로 다른 안내 사이 최소 간격. */
    const val MIN_GAP_MS = 1_500L

    /** 같은 안내를 다시 말하기까지의 간격. */
    const val REPEAT_COOLDOWN_MS = 3_000L

    fun decide(cues: List<Guidance>, state: GuidanceState, nowMs: Long): GuidanceDecision {
        val top = cues.minByOrNull { it.priority }
        val announce = top?.takeIf { canAnnounce(it, state, nowMs) }
        val nextState = if (announce != null) GuidanceState(announce, nowMs) else state
        return GuidanceDecision(display = top, announce = announce, state = nextState)
    }

    private fun canAnnounce(cue: Guidance, state: GuidanceState, nowMs: Long): Boolean {
        val lastAt = state.lastAnnouncedAtMs ?: return true
        val elapsed = nowMs - lastAt
        val gap = if (cue == state.lastAnnounced) REPEAT_COOLDOWN_MS else MIN_GAP_MS
        return elapsed >= gap
    }
}
