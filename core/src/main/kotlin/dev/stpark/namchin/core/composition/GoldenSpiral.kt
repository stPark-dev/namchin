package dev.stpark.namchin.core.composition

import dev.stpark.namchin.core.guidance.Guidance.Side

/**
 * 타원의 1/4 호. 좌표는 촬영 영역 기준(0..1).
 * 각도는 도 단위, 0 = 오른쪽(3시), 시계 방향(+y 아래)이 양수 — Compose drawArc와 같다.
 * 호는 startDegrees에서 시작해 sweepDegrees만큼(음수면 반시계) 그린다.
 */
data class Arc(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val startDegrees: Float,
    val sweepDegrees: Float,
)

/**
 * 황금 나선: 화면을 위 → 왼쪽 → 아래 → 오른쪽 순으로 0.618씩 잘라 내며 1/4 호를 잇는다.
 * 오른쪽(RIGHT)은 오른쪽 위 모서리에서 시작해 오른쪽 아래 눈(EYE, EYE)으로 감긴다. 왼쪽은 좌우를 뒤집는다.
 */
object GoldenSpiral {
    private const val PHI_CUT = 0.618034f

    /** 나선이 감겨 들어가는 점 (가로·세로 모두): 0.618 / (1 - 0.382²) */
    const val EYE = PHI_CUT / (1f - (1f - PHI_CUT) * (1f - PHI_CUT))

    private const val STEPS = 10

    /** 고른 쪽이 없으면 다른 틀처럼 왼쪽 */
    fun arcs(side: Side?): List<Arc> {
        val right = buildRight()
        return if (side == Side.RIGHT) right else right.map(::mirror)
    }

    private fun buildRight(): List<Arc> {
        var l = 0f
        var t = 0f
        var r = 1f
        var b = 1f
        return List(STEPS) { i ->
            val w = r - l
            val h = b - t
            when (i % 4) {
                0 -> { // 위를 잘라 냄: 오른쪽 위 → 왼쪽 아래 (중심은 조각의 오른쪽 아래)
                    val cut = t + h * PHI_CUT
                    Arc(l, t, r + w, cut + (cut - t), 270f, -90f).also { t = cut }
                }
                1 -> { // 왼쪽을 잘라 냄: 왼쪽 위 → 오른쪽 아래 (중심은 조각의 오른쪽 위)
                    val cut = l + w * PHI_CUT
                    Arc(l, t - h, cut + (cut - l), b, 180f, -90f).also { l = cut }
                }
                2 -> { // 아래를 잘라 냄: 왼쪽 아래 → 오른쪽 위 (중심은 조각의 왼쪽 위)
                    val cut = b - h * PHI_CUT
                    Arc(l - w, cut - (b - cut), r, b, 90f, -90f).also { b = cut }
                }
                else -> { // 오른쪽을 잘라 냄: 오른쪽 아래 → 왼쪽 위 (중심은 조각의 왼쪽 아래)
                    val cut = r - w * PHI_CUT
                    Arc(cut - (r - cut), t, r, b + h, 0f, -90f).also { r = cut }
                }
            }
        }
    }

    /** 좌우 뒤집기: 각도 θ → 180 - θ, 도는 방향도 반대 */
    private fun mirror(arc: Arc): Arc = arc.copy(
        left = 1f - arc.right,
        right = 1f - arc.left,
        startDegrees = 180f - arc.startDegrees,
        sweepDegrees = -arc.sweepDegrees,
    )
}
