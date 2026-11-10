package dev.stpark.namchin.camera

/** 카드 손패 순서: 장면이 비슷한 순서(order)대로, 순서에 없는 카드는 원래 순서로 뒤에. */
object CardOrder {
    fun <T> arrange(all: List<T>, order: List<Long>?, id: (T) -> Long): List<T> {
        if (order == null) return all
        val byId = all.associateBy(id)
        val ordered = order.toSet()
        return order.mapNotNull { byId[it] } + all.filter { id(it) !in ordered }
    }
}
