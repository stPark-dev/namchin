package dev.stpark.namchin.core.crop

/** 축소된 흑백(휘도 0..255) 이미지. */
class LumaImage(val width: Int, val height: Int, val data: IntArray) {
    init {
        require(data.size == width * height) { "data 크기(${data.size})가 ${width}x$height 와 다름" }
    }

    operator fun get(x: Int, y: Int): Int = data[y * width + x]
}

/** right/bottom은 미포함(exclusive). */
data class CropRect(val left: Int, val top: Int, val right: Int, val bottom: Int)

/**
 * 인스타 캡처에서 사진 영역을 찾는다.
 * 앱 UI는 흰색(라이트) 또는 검은색(다크) 배경이 대부분인 줄이라는 점을 이용한다.
 */
object ScreenshotCropDetector {
    const val UI_RATIO = 0.7f
    const val NEAR_WHITE = 245
    const val NEAR_BLACK = 10
    const val MIN_SIZE_RATIO = 0.2f
    const val GAP_TOLERANCE_RATIO = 0.02f

    fun detect(image: LumaImage): CropRect? {
        val rowIsPhoto = BooleanArray(image.height) { y ->
            !isUi(IntArray(image.width) { x -> image[x, y] })
        }
        val rows = longestRun(rowIsPhoto, gapFor(image.height)) ?: return null
        if (rows.count() < image.height * MIN_SIZE_RATIO) return null

        val colIsPhoto = BooleanArray(image.width) { x ->
            !isUi(IntArray(rows.count()) { i -> image[x, rows.first + i] })
        }
        val cols = longestRun(colIsPhoto, gapFor(image.width)) ?: return null
        if (cols.count() < image.width * MIN_SIZE_RATIO) return null

        return CropRect(cols.first, rows.first, cols.last + 1, rows.last + 1)
    }

    private fun isUi(line: IntArray): Boolean {
        val white = line.count { it >= NEAR_WHITE }
        val black = line.count { it <= NEAR_BLACK }
        return maxOf(white, black) >= line.size * UI_RATIO
    }

    private fun gapFor(length: Int): Int = maxOf(1, (length * GAP_TOLERANCE_RATIO).toInt())

    /** true 구간 중 가장 긴 것. gap 이하의 false 틈은 이어 붙인다. */
    private fun longestRun(flags: BooleanArray, gap: Int): IntRange? {
        val segments = mutableListOf<IntRange>()
        var start = -1
        for (i in 0..flags.size) {
            val on = i < flags.size && flags[i]
            if (on && start < 0) start = i
            if (!on && start >= 0) {
                segments += start until i
                start = -1
            }
        }
        val merged = segments.fold(emptyList<IntRange>()) { acc, seg ->
            val last = acc.lastOrNull()
            if (last != null && seg.first - last.last - 1 <= gap) acc.dropLast(1) + listOf(last.first..seg.last)
            else acc + listOf(seg)
        }
        return merged.maxByOrNull { it.count() }
    }
}
