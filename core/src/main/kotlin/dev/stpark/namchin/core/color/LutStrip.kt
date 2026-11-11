package dev.stpark.namchin.core.color

import kotlin.math.roundToInt

/**
 * 화면 미리보기(GPU)용 작은 색 변환표를 한 장의 그림으로 편 것.
 * 파랑 칸(tile)마다 SIZE×SIZE 정사각형: 가로 = 빨강, 세로 = 초록. 칸들을 가로로 이어 붙인다.
 * 사진 저장용 ColorLut(65³)보다 작아도 화면에선 차이가 안 보인다.
 */
class LutStrip private constructor(val width: Int, val height: Int, val pixels: IntArray) {
    companion object {
        const val SIZE = 33
        private const val ALPHA = 0xFF shl 24

        fun of(transform: (Int) -> Int): LutStrip {
            val width = SIZE * SIZE
            val pixels = IntArray(width * SIZE)
            fun level(i: Int) = (i * 255f / (SIZE - 1)).roundToInt()
            for (b in 0 until SIZE) for (g in 0 until SIZE) for (r in 0 until SIZE) {
                val out = transform(ALPHA or (level(r) shl 16) or (level(g) shl 8) or level(b))
                pixels[g * width + b * SIZE + r] = ALPHA or (out and 0xFFFFFF)
            }
            return LutStrip(width, SIZE, pixels)
        }
    }
}
