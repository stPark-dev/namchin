package dev.stpark.namchin.core.color

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

class FilterRecipeTest {

    private fun argb(r: Int, g: Int, b: Int) = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    private fun r(c: Int) = (c shr 16) and 0xFF
    private fun g(c: Int) = (c shr 8) and 0xFF
    private fun b(c: Int) = c and 0xFF
    private fun distance(x: Int, y: Int) = abs(r(x) - r(y)) + abs(g(x) - g(y)) + abs(b(x) - b(y))

    private val samples = listOf(argb(0, 0, 0), argb(255, 255, 255), argb(128, 128, 128), argb(200, 120, 40), argb(30, 90, 210))
    private val skin = argb(224, 172, 140)
    private val sky = argb(120, 170, 230)

    @Test
    fun `empty recipe changes nothing`() {
        samples.forEach { c -> assertTrue(distance(FilterRecipe().apply(c), c) <= 1, "$c") }
    }

    @Test
    fun `alpha is kept`() {
        val translucent = (0x80 shl 24) or 0x336699
        assertEquals(0x80, FilterRecipe(warmth = 0.5f).apply(translucent) ushr 24)
    }

    @Test
    fun `warmth pushes grey toward red and away from blue`() {
        val out = FilterRecipe(warmth = 0.5f).apply(argb(128, 128, 128))
        assertTrue(r(out) > 128 && b(out) < 128)
        val cool = FilterRecipe(warmth = -0.5f).apply(argb(128, 128, 128))
        assertTrue(b(cool) > 128 && r(cool) < 128)
    }

    @Test
    fun `tint pushes toward magenta or green`() {
        assertTrue(g(FilterRecipe(tint = 0.5f).apply(argb(128, 128, 128))) < 128)
        assertTrue(g(FilterRecipe(tint = -0.5f).apply(argb(128, 128, 128))) > 128)
    }

    @Test
    fun `full desaturation gives grey`() {
        val out = FilterRecipe(saturation = -1f).apply(argb(200, 120, 40))
        assertTrue(abs(r(out) - g(out)) <= 1 && abs(g(out) - b(out)) <= 1)
    }

    @Test
    fun `more saturation spreads the channels`() {
        val c = argb(160, 120, 100)
        val out = FilterRecipe(saturation = 0.5f).apply(c)
        assertTrue(r(out) - b(out) > r(c) - b(c))
    }

    @Test
    fun `contrast darkens shadows and brightens highlights`() {
        val recipe = FilterRecipe(contrast = 0.5f)
        assertTrue(r(recipe.apply(argb(60, 60, 60))) < 60)
        assertTrue(r(recipe.apply(argb(200, 200, 200))) > 200)
    }

    @Test
    fun `exposure brightens without wrapping white`() {
        val recipe = FilterRecipe(exposure = 0.5f)
        assertTrue(r(recipe.apply(argb(100, 100, 100))) > 100)
        assertEquals(argb(255, 255, 255), recipe.apply(argb(255, 255, 255)))
    }

    @Test
    fun `fade lifts black for a film look`() {
        assertTrue(r(FilterRecipe(fade = 0.5f).apply(argb(0, 0, 0))) > 10)
    }

    @Test
    fun `split toning tints shadows and highlights separately`() {
        val recipe = FilterRecipe(shadowTint = Tint(0f, 0.1f, 0.15f), highlightTint = Tint(0.15f, 0.05f, 0f))
        val dark = recipe.apply(argb(40, 40, 40))
        val light = recipe.apply(argb(210, 210, 210))
        assertTrue(b(dark) > r(dark), "그림자는 청록")
        assertTrue(r(light) > b(light), "밝은 곳은 주황")
    }

    @Test
    fun `skin protection keeps skin closer to the original than the sky`() {
        val strong = FilterRecipe(warmth = -0.6f, saturation = 0.4f, skinProtect = true)
        val unprotected = strong.copy(skinProtect = false)

        assertTrue(distance(strong.apply(skin), skin) < distance(unprotected.apply(skin), skin))
        assertEquals(unprotected.apply(sky), strong.apply(sky))
    }

    @Test
    fun `every filter has its own look`() {
        val photo = listOf(skin, sky, argb(90, 120, 60), argb(40, 40, 50), argb(230, 220, 200))
        val looks = Filter.entries.map { f -> photo.map(f.recipe::apply) }

        assertEquals(looks.size, looks.toSet().size)
        Filter.entries.forEach { f -> assertNotEquals(photo, photo.map(f.recipe::apply), f.name) }
    }

    @Test
    fun `mono filter is black and white`() {
        val out = Filter.MONO.recipe.apply(argb(200, 120, 40))
        assertTrue(abs(r(out) - g(out)) <= 1 && abs(g(out) - b(out)) <= 1)
    }

    @Test
    fun `filter table matches the recipe`() {
        val lut = Filter.FILM.lut()
        samples.forEach { c -> assertTrue(distance(lut.apply(c), Filter.FILM.recipe.apply(c)) <= 3, "$c") }
    }

    @Test
    fun `skin weight fades out smoothly in the dark instead of cutting off`() {
        // 같은 색상·채도의 피부를 아주 조금씩 어둡게 하면서 갑자기 튀는 곳이 없어야 한다
        var previous = FilterRecipe.skinWeight(0.4f, 0.31f, 0.26f)
        var scale = 1f
        while (scale > 0.2f) {
            scale -= 0.01f
            val w = FilterRecipe.skinWeight(0.4f * scale, 0.31f * scale, 0.26f * scale)
            assertTrue(abs(w - previous) < 0.1f, "scale=$scale $previous -> $w")
            previous = w
        }
    }

    @Test
    fun `skin protection keeps the brightening of the filter`() {
        val recipe = FilterRecipe(exposure = 0.4f, warmth = -0.5f, skinProtect = true)
        fun luma(c: Int) = 0.299f * r(c) + 0.587f * g(c) + 0.114f * b(c)

        val midSkin = argb(170, 125, 100) // 밝게 해도 255에 닿지 않는 피부
        val protected = recipe.apply(midSkin)
        val plain = recipe.copy(skinProtect = false).apply(midSkin)

        assertEquals(luma(plain), luma(protected), 2f)
        assertTrue(distance(protected, midSkin) < distance(plain, midSkin))
    }
}
