package dev.stpark.namchin.capture

/** 연속 촬영 순서. 한 장이라도 실패하거나 화면이 비활성이면 멈춘다 (빈 셔터음 방지). */
object Burst {
    /**
     * @param takeOne 한 장 찍고 결과(실패면 null)를 콜백으로 알린다
     * @param onDone 저장된 결과들 (찍은 순서)
     */
    fun <T : Any> run(count: Int, isActive: () -> Boolean, takeOne: ((T?) -> Unit) -> Unit, onDone: (List<T>) -> Unit) {
        fun shot(remaining: Int, saved: List<T>) {
            if (remaining == 0 || !isActive()) return onDone(saved)
            takeOne { result -> if (result != null) shot(remaining - 1, saved + result) else onDone(saved) }
        }
        shot(count, emptyList())
    }
}
