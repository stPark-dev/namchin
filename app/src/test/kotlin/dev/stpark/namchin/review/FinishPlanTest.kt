package dev.stpark.namchin.review

import dev.stpark.namchin.core.color.Filter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FinishPlanTest {
    private val shots = listOf("a", "b", "c")
    private val film = Look.Preset(Filter.FILM)

    @Test
    fun `keeping all without a filter changes nothing`() {
        assertEquals(FinishPlan(delete = emptyList(), recolor = emptyList()), FinishPlan.of(shots, keepId = null, look = null))
    }

    @Test
    fun `keeping all with a filter recolours every photo`() {
        assertEquals(FinishPlan(delete = emptyList(), recolor = shots), FinishPlan.of(shots, keepId = null, look = film))
    }

    @Test
    fun `keeping one deletes the rest and recolours only the kept one`() {
        assertEquals(FinishPlan(delete = listOf("a", "c"), recolor = listOf("b")), FinishPlan.of(shots, keepId = "b", look = film))
    }

    @Test
    fun `keeping one without a filter only deletes the rest`() {
        assertEquals(FinishPlan(delete = listOf("a", "c"), recolor = emptyList()), FinishPlan.of(shots, keepId = "b", look = null))
    }

    @Test
    fun `nothing to do means the plan is empty`() {
        assertEquals(true, FinishPlan.of(shots, keepId = null, look = null).isEmpty)
        assertEquals(false, FinishPlan.of(shots, keepId = null, look = film).isEmpty)
    }
}
