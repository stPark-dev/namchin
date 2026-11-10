package dev.stpark.namchin.core.guidance

/**
 * @property candidate 현재 최우선 안내와 그것이 처음 최우선이 된 시각
 */
data class GuidanceState(
    val lastAnnounced: Guidance? = null,
    val lastAnnouncedAtMs: Long? = null,
    val candidate: Guidance? = null,
    val candidateSinceMs: Long = 0,
)

/**
 * @property display 화면에 보여줄 안내 (항상 최우선 1개)
 * @property announce 이번에 음성·진동으로 낼 안내 (안정화·반복 방지로 대부분 null)
 */
data class GuidanceDecision(
    val display: Guidance?,
    val announce: Guidance?,
    val state: GuidanceState,
)

/** 한 번에 하나만, 잠깐 튄 값은 무시하고, 같은 말 반복 없이 안내를 고른다. */
object GuidancePrioritizer {
    /** 최우선 안내가 이 시간 동안 유지돼야 말한다. */
    const val STABLE_MS = 400L

    /** 서로 다른 안내 사이 최소 간격. */
    const val MIN_GAP_MS = 1_500L

    /** 같은 안내를 다시 말하기까지의 간격. */
    const val REPEAT_COOLDOWN_MS = 3_000L

    fun decide(cues: List<Guidance>, state: GuidanceState, nowMs: Long): GuidanceDecision {
        val top = cues.minByOrNull { it.priority }
        val tracked = if (top == state.candidate) state else state.copy(candidate = top, candidateSinceMs = nowMs)
        val announce = top?.takeIf {
            nowMs - tracked.candidateSinceMs >= STABLE_MS && gapElapsed(it, tracked, nowMs)
        }
        val next = if (announce != null) tracked.copy(lastAnnounced = announce, lastAnnouncedAtMs = nowMs) else tracked
        return GuidanceDecision(display = top, announce = announce, state = next)
    }

    private fun gapElapsed(cue: Guidance, state: GuidanceState, nowMs: Long): Boolean {
        val lastAt = state.lastAnnouncedAtMs ?: return true
        val gap = if (cue == state.lastAnnounced) REPEAT_COOLDOWN_MS else MIN_GAP_MS
        return nowMs - lastAt >= gap
    }
}
