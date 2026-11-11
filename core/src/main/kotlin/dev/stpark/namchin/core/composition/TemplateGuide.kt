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
) {
    companion object {
        private const val HEAD_TOP_FACTOR = 0.7f
        private const val HEAD_RADIUS_PER_NECK = 0.5f
        private const val ANKLE_PER_TORSO = 2.4f
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
                CompositionTemplate.SIDE_THIRD -> listOf(Line(t.anchorX, 0f, t.anchorX, 1f))
                CompositionTemplate.HORIZON_T -> listOf(horizontal(CompositionTemplate.HORIZON_Y), Line(0.5f, 0f, 0.5f, 1f))
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
            )
        }

        private fun horizontal(y: Float) = Line(0f, y, 1f, y)
    }
}
