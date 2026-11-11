package dev.stpark.namchin.review

/**
 * 결과 화면을 마칠 때 할 일: 지울 사진과, 필터를 입혀 원본 자리에 덮어쓸 사진.
 * 필터를 골랐으면 남는 사진에는 필터 입힌 것만 남긴다.
 */
data class FinishPlan(val delete: List<String>, val recolor: List<String>) {
    val isEmpty: Boolean get() = delete.isEmpty() && recolor.isEmpty()

    companion object {
        /** @param keepId 이 한 장만 남긴다. null이면 모두 남긴다 */
        fun of(shotIds: List<String>, keepId: String?, look: Look?): FinishPlan {
            val kept = if (keepId == null) shotIds else shotIds.filter { it == keepId }
            return FinishPlan(
                delete = shotIds - kept.toSet(),
                recolor = if (look == null) emptyList() else kept,
            )
        }
    }
}
