package dev.stpark.namchin.core.color

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.random.Random

class ColorTransferTest {

    private fun argb(r: Int, g: Int, b: Int) = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    private fun r(c: Int) = (c shr 16) and 0xFF
    private fun g(c: Int) = (c shr 8) and 0xFF
    private fun b(c: Int) = c and 0xFF

    /** 기준색 주변으로 흩어진 사진 흉내 */
    private fun image(r: Int, g: Int, b: Int, spread: Int = 30, n: Int = 4000, seed: Int = 1): IntArray {
        val rnd = Random(seed)
        fun v(base: Int) = (base + rnd.nextInt(-spread, spread + 1)).coerceIn(0, 255)
        return IntArray(n) { argb(v(r), v(g), v(b)) }
    }

    @Test
    fun `lab conversion round trips`() {
        listOf(argb(0, 0, 0), argb(255, 255, 255), argb(200, 120, 40), argb(30, 90, 210)).forEach { c ->
            val back = ColorLab.toArgb(ColorLab.fromArgb(c))
            assertTrue(abs(r(back) - r(c)) <= 1 && abs(g(back) - g(c)) <= 1 && abs(b(back) - b(c)) <= 1, "$c -> $back")
        }
    }

    @Test
    fun `same colours in and out means no change`() {
        val photo = image(120, 110, 100)
        val transfer = ColorTransfer(source = ColorStats.of(photo), reference = ColorStats.of(photo))

        photo.take(50).forEach { c ->
            val out = transfer.apply(c)
            assertTrue(abs(r(out) - r(c)) <= 1 && abs(b(out) - b(c)) <= 1)
        }
    }

    @Test
    fun `a neutral photo takes on the warm look of the reference`() {
        val neutral = image(128, 128, 128)
        val warm = image(200, 140, 80)
        val transfer = ColorTransfer(ColorStats.of(neutral), ColorStats.of(warm))

        val result = ColorStats.of(IntArray(neutral.size) { transfer.apply(neutral[it]) })
        val target = ColorStats.of(warm)

        assertEquals(target.mean[1], result.mean[1], 2.0) // a* (붉은기)
        assertEquals(target.mean[2], result.mean[2], 2.0) // b* (노란기)
    }

    @Test
    fun `strength zero changes nothing and half goes halfway`() {
        val neutral = image(128, 128, 128)
        val warm = image(200, 140, 80)
        val none = ColorTransfer(ColorStats.of(neutral), ColorStats.of(warm), strength = 0f)
        val half = ColorTransfer(ColorStats.of(neutral), ColorStats.of(warm), strength = 0.5f)
        val c = argb(128, 128, 128)

        assertEquals(c, none.apply(c))
        // 색은 Lab 공간에서 섞으므로 "절반"도 Lab 값으로 확인 (RGB는 감마 때문에 선형이 아님)
        val full = ColorLab.fromArgb(ColorTransfer(ColorStats.of(neutral), ColorStats.of(warm)).apply(c))
        val mid = ColorLab.fromArgb(half.apply(c))
        val orig = ColorLab.fromArgb(c)
        for (ch in 0..2) assertEquals((orig[ch] + full[ch]) / 2, mid[ch], 1.0)
    }

    @Test
    fun `contrast changes are limited so skin tones do not blow out`() {
        val flat = image(128, 128, 128, spread = 2)
        val wild = image(128, 128, 128, spread = 120, seed = 2)

        val transfer = ColorTransfer(ColorStats.of(flat), ColorStats.of(wild))

        assertTrue(transfer.scale.all { it <= ColorTransfer.MAX_SCALE })
    }

    @Test
    fun `lookup table matches the direct transform closely`() {
        val transfer = ColorTransfer(ColorStats.of(image(110, 120, 140)), ColorStats.of(image(200, 150, 90)))
        val lut = ColorLut.build(transfer::apply)
        val rnd = Random(7)
        var err = 0
        repeat(500) {
            val c = argb(rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256))
            val d = transfer.apply(c)
            val l = lut.apply(c)
            err = maxOf(err, abs(r(d) - r(l)), abs(g(d) - g(l)), abs(b(d) - b(l)))
        }
        assertTrue(err <= 4, "max error $err")
    }

    @Test
    fun `alpha is preserved`() {
        val transfer = ColorTransfer(ColorStats.of(image(100, 100, 100)), ColorStats.of(image(150, 120, 90)))
        val translucent = (0x80 shl 24) or (100 shl 16) or (100 shl 8) or 100

        assertEquals(0x80, (transfer.apply(translucent) ushr 24) and 0xFF)
    }

    @Test
    fun `lookup table strength mixes original and full look`() {
        val transfer = ColorTransfer(ColorStats.of(image(110, 120, 140)), ColorStats.of(image(200, 150, 90)))
        val lut = ColorLut.build(transfer::apply)
        val c = argb(100, 120, 140)
        val full = lut.apply(c)

        assertEquals(c, lut.apply(c, strength = 0f))
        assertEquals(full, lut.apply(c, strength = 1f))
        assertTrue(abs(r(lut.apply(c, 0.5f)) - (r(c) + r(full)) / 2) <= 1)
    }

    @Test
    fun `keeping lightness changes the colour but not the brightness`() {
        // 밝은 사진에 어둡고 푸른 레퍼런스: 색(푸른 기)만 가져오고 밝기는 그대로
        val photo = image(200, 190, 180)
        val darkBlue = image(40, 60, 110)
        val transfer = ColorTransfer(ColorStats.of(photo), ColorStats.of(darkBlue), keepLightness = true)

        photo.take(50).forEach { c ->
            val before = ColorLab.fromArgb(c)
            val after = ColorLab.fromArgb(transfer.apply(c))
            // 8비트 RGB로 되돌릴 때 반올림·색 범위 밖 잘림만큼은 흔들린다 (사람 눈엔 같은 밝기)
            assertEquals(before[0], after[0], 2.0, "L")
            assertTrue(after[2] < before[2] - 5, "더 푸르게 (b 감소): ${before[2]} -> ${after[2]}")
        }
    }

    @Test
    fun `without keeping lightness a dark reference darkens the photo`() {
        val photo = image(200, 190, 180)
        val transfer = ColorTransfer(ColorStats.of(photo), ColorStats.of(image(40, 60, 110)))

        val c = photo.first()
        assertTrue(ColorLab.fromArgb(transfer.apply(c))[0] < ColorLab.fromArgb(c)[0] - 20)
    }
}
