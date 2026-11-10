package dev.stpark.namchin.capture

/** 연속 촬영 순서. 한 장이라도 실패하거나 화면이 비활성이면 멈춘다 (빈 셔터음 방지). */
object Burst {
    /**
     * @param takeOne 한 장 찍고 성공 여부를 콜백으로 알린다
     * @param onDone 저장된 장 수
     */
    fun run(count: Int, isActive: () -> Boolean, takeOne: ((Boolean) -> Unit) -> Unit, onDone: (Int) -> Unit) {
        fun shot(remaining: Int, saved: Int) {
            if (remaining == 0 || !isActive()) return onDone(saved)
            takeOne { ok -> if (ok) shot(remaining - 1, saved + 1) else onDone(saved) }
        }
        shot(count, 0)
    }
}
