package dev.stpark.namchin.core.tagging

/**
 * 상세 화면 태그 칩 규칙.
 * own = 이 사진의 태그(자동 판단 또는 사용자가 확정한 것), learned = 비슷한 사진에게서 배운 제안.
 * 칩 하나를 누르면 그 칩만 반영하고 검토하지 않은 제안은 확정하지 않는다 (잘못된 제안이 번지지 않게).
 */
object TagEdit {
    enum class ChipState { ON, SUGGESTED, OFF }

    fun states(own: Set<Tag>, learned: Set<Tag>): Map<Tag, ChipState> =
        TagVocabulary.CATEGORIES.flatMap { it.tags }.associateWith { tag ->
            when (tag) {
                in own -> ChipState.ON
                in learned -> ChipState.SUGGESTED
                else -> ChipState.OFF
            }
        }

    /** 누른 칩만 켜고 끈 결과 (확정할 태그). */
    fun toggle(own: Set<Tag>, tag: Tag): Set<Tag> = if (tag in own) own - tag else own + tag

    /** "이대로 확정": 내 태그 + 제안 전부. */
    fun confirmAll(own: Set<Tag>, learned: Set<Tag>): Set<Tag> = own + learned
}
