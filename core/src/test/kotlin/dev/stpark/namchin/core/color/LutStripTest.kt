package dev.stpark.namchin.core.color

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

class LutStripTest {

    private fun argb(r: Int, g: Int, b: Int) = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    private fun level(i: Int) = Math.round(i * 255f / (LutStrip.SIZE - 1))

    @Test
    fun `strip is SIZE tiles of SIZE by SIZE laid side by side`() {
        val strip = LutStrip.of(FilterRecipe()::apply)

        assertEquals(LutStrip.SIZE * LutStrip.SIZE, strip.width)
        assertEquals(LutStrip.SIZE, strip.height)
        assertEquals(strip.width * strip.height, strip.pixels.size)
    }

    @Test
    fun `blue picks the tile, red goes across and green goes down`() {
        val n = LutStrip.SIZE
        val strip = LutStrip.of(FilterRecipe()::apply)
        val (r, g, b) = Triple(5, 20, 30)

        val pixel = strip.pixels[g * strip.width + b * n + r]

        assertEquals(argb(level(r), level(g), level(b)), pixel)
    }

    @Test
    fun `strip carries the filter colours`() {
        val n = LutStrip.SIZE
        val recipe = Filter.FILM.recipe
        val strip = LutStrip.of(recipe::apply)
        val (r, g, b) = Triple(n - 1, 0, n / 2)
        val expected = recipe.apply(argb(level(r), level(g), level(b)))
        val actual = strip.pixels[g * strip.width + b * n + r]

        assertTrue(abs((expected shr 16 and 0xFF) - (actual shr 16 and 0xFF)) <= 1)
        assertEquals(0xFF, actual ushr 24)
    }
}
