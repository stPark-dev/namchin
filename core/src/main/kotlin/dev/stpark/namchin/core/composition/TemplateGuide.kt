package dev.stpark.namchin.core.composition

import dev.stpark.namchin.core.guidance.Guidance.Side

/** 0..1 촬영 영역 위의 선분. */
data class Line(val x1: Float, val y1: Float, val x2: Float, val y2: Float)

/**
 * 화면에 그릴 구도 틀: 인물 자리 윤곽(머리 + 어깨·허리·두 다리)과 보조선.
 * y와 x는 촬영 영역 기준(0..1). 반지름·반폭은 영역 높이 단위 (그릴 때 가로 비율 보정).
 */
data class TemplateGuide(
    val centerX: Float,
    val headTop: Float,
    val headRadius: Float,
    val shoulderY: Float,
    val hipY: Float,
    val bottom: Float,
    val shoulderHalfWidth: Float,
    val hipHalfWidth: Float,
    val lines: List<Line>,
    val arcs: List<Arc> = emptyList(),
) {
    companion object {
        private const val HEAD_TOP_FACTOR = 0.7f
        private const val HEAD_RADIUS_PER_NECK = 0.5f
        private const val ANKLE_PER_TORSO = CompositionTemplate.FEET_PER_TORSO
        private const val SHOULDER_HALF_PER_TORSO = 0.3f
        private const val HIP_HALF_PER_TORSO = 0.22f

        fun of(template: CompositionTemplate, side: Side?): TemplateGuide {
            val t = template.target(side)
            val torso = t.torso ?: 0f
            val neck = t.neck ?: 0f
            val nose = t.anchorY - neck
            val headTop = nose - HEAD_TOP_FACTOR * neck
            val lines = when (template) {
                CompositionTemplate.FULL_BODY -> listOf(horizontal(CompositionTemplate.FEET_Y))
                CompositionTemplate.HALF_BODY -> emptyList()
                CompositionTemplate.SIDE_THIRD -> listOf(Line(t.anchorX, 0f, t.anchorX, 1f), horizontal(2f / 3))
                CompositionTemplate.HORIZON_T -> listOf(horizontal(CompositionTemplate.HORIZON_Y), Line(0.5f, 0f, 0.5f, 1f))
                CompositionTemplate.GOLDEN_SPIRAL -> emptyList()
                CompositionTemplate.DIAGONAL_SPACE -> listOf(slope(side))
                CompositionTemplate.LEADING_LINES -> converging()
                CompositionTemplate.CENTER -> listOf(horizontal(1f / 3), vertical(1f / 3), vertical(2f / 3))
                CompositionTemplate.FACE_THIRD -> listOf(horizontal(1f / 3), vertical(t.anchorX))
                CompositionTemplate.BOTTOM_CENTER ->
                    listOf(horizontal(2f / 3), Line(1f / 3, 2f / 3, 1f / 3, 1f), Line(2f / 3, 2f / 3, 2f / 3, 1f))
            }
            return TemplateGuide(
                centerX = t.anchorX,
                headTop = headTop,
                headRadius = neck * HEAD_RADIUS_PER_NECK,
                shoulderY = t.anchorY,
                hipY = t.anchorY + torso,
                // 발이 화면 밖인 틀(상반신)은 다리 없이 허리에서 닫는다
                bottom = (t.anchorY + torso * ANKLE_PER_TORSO).let { if (it > 1f) minOf(t.anchorY + torso, 1f) else it },
                shoulderHalfWidth = torso * SHOULDER_HALF_PER_TORSO,
                hipHalfWidth = torso * HIP_HALF_PER_TORSO,
                lines = lines,
                arcs = if (template == CompositionTemplate.GOLDEN_SPIRAL) GoldenSpiral.arcs(side) else emptyList(),
            )
        }

        private fun horizontal(y: Float) = Line(0f, y, 1f, y)

        private fun vertical(x: Float) = Line(x, 0f, x, 1f)

        /** 인물 발밑을 지나 반대쪽으로 내려가는 비탈 */
        private const val SLOPE_DROP = 0.2f

        private fun slope(side: Side?): Line {
            val feet = CompositionTemplate.SMALL_FEET_Y
            val herX = CompositionTemplate.DIAGONAL_X
            val nearEdge = feet - SLOPE_DROP * herX
            val farEdge = nearEdge + SLOPE_DROP
            return if (side == Side.RIGHT) Line(0f, farEdge, 1f, nearEdge) else Line(0f, nearEdge, 1f, farEdge)
        }

        /** 화면 아래 두 모서리에서 좁아지며 인물 발끝 양옆에 닿는 길 */
        private const val PATH_HALF_WIDTH = 0.1f

        private fun converging(): List<Line> {
            val feet = CompositionTemplate.SMALL_FEET_Y
            return listOf(Line(0f, 1f, 0.5f - PATH_HALF_WIDTH, feet), Line(1f, 1f, 0.5f + PATH_HALF_WIDTH, feet))
        }
    }
}
