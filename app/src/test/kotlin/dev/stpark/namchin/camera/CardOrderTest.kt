package dev.stpark.namchin.camera

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CardOrderTest {

    private data class Card(val id: Long)

    private val all = listOf(Card(1), Card(2), Card(3), Card(4))

    private fun arrange(order: List<Long>?) = CardOrder.arrange(all, order) { it.id }

    @Test
    fun `without a scene order the list stays as is`() {
        assertEquals(all, arrange(null))
    }

    @Test
    fun `cards follow the scene order`() {
        assertEquals(listOf(Card(3), Card(1), Card(4), Card(2)), arrange(listOf(3, 1, 4, 2)))
    }

    @Test
    fun `cards missing from the order go last in their original order`() {
        // 새로 추가돼 아직 점수가 없는 레퍼런스
        assertEquals(listOf(Card(4), Card(1), Card(2), Card(3)), arrange(listOf(4)))
    }

    @Test
    fun `deleted ids in the order are skipped`() {
        assertEquals(listOf(Card(2), Card(1), Card(3), Card(4)), arrange(listOf(9, 2)))
    }
}
