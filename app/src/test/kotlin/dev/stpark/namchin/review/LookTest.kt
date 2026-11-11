package dev.stpark.namchin.review

import dev.stpark.namchin.core.color.Filter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class LookTest {

    @Test
    fun `a preset filter table is shared by every photo`() {
        assertEquals(Look.Preset(Filter.FILM).tableKey("a"), Look.Preset(Filter.FILM).tableKey("b"))
        assertNotEquals(Look.Preset(Filter.FILM).tableKey("a"), Look.Preset(Filter.MONO).tableKey("a"))
    }

    @Test
    fun `a reference colour table depends on the photo and the lightness option`() {
        assertNotEquals(Look.Reference(false).tableKey("a"), Look.Reference(false).tableKey("b"))
        assertNotEquals(Look.Reference(false).tableKey("a"), Look.Reference(true).tableKey("a"))
        assertEquals(Look.Reference(true).tableKey("a"), Look.Reference(true).tableKey("a"))
    }

    @Test
    fun `only the reference look needs a reference photo`() {
        assertEquals(true, Look.Reference(false).needsReference)
        assertEquals(false, Look.Preset(Filter.CLEAR).needsReference)
    }
}
