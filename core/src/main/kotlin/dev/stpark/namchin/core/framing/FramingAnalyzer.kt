package dev.stpark.namchin.core.framing

import dev.stpark.namchin.core.guidance.Guidance
import dev.stpark.namchin.core.guidance.Rotation

/**
 * 레퍼런스와 현재 구도를 비교해 필요한 안내를 모두 돌려준다. 순서는 의미 없음.
 * 직전 프레임에 나온 안내는 허용 오차의 EXIT_FACTOR 배 안으로 들어와야 사라진다 (깜빡임 방지).
 */
object FramingAnalyzer {
    const val LEVEL_TOLERANCE_DEG = 3f
    const val SIZE_TOLERANCE = 0.10f
    const val POSITION_TOLERANCE = 0.05f
    const val EXIT_FACTOR = 0.6f

    /**
     * @param rollDegrees 폰이 시계 방향으로 기울어진 각도 (양수 = 시계 방향)
     * @param previous 직전 프레임의 안내
     */
    fun analyze(
        reference: PersonFrame,
        current: PersonFrame,
        rollDegrees: Float,
        previous: Collection<Guidance> = emptyList(),
    ): List<Guidance> {
        fun pick(value: Float, tolerance: Float, ifPositive: Guidance, ifNegative: Guidance): Guidance? {
            val candidate = if (value > 0) ifPositive else ifNegative
            val limit = if (candidate in previous) tolerance * EXIT_FACTOR else tolerance
            return candidate.takeIf { kotlin.math.abs(value) > limit }
        }
        return listOfNotNull(
            pick(
                rollDegrees, LEVEL_TOLERANCE_DEG,
                Guidance.Level(Rotation.COUNTERCLOCKWISE), Guidance.Level(Rotation.CLOCKWISE),
            ),
            pick(current.torso / reference.torso - 1, SIZE_TOLERANCE, Guidance.MoveBack, Guidance.MoveCloser),
            // 인물이 화면 위쪽(음수)에 있으면 폰을 위로 기울여야 인물이 내려온다.
            pick(current.anchorY - reference.anchorY, POSITION_TOLERANCE, Guidance.TiltDown, Guidance.TiltUp),
            // 인물이 화면 왼쪽(음수)에 있으면 폰을 왼쪽으로 돌려야 인물이 오른쪽으로 온다.
            pick(current.anchorX - reference.anchorX, POSITION_TOLERANCE, Guidance.PanRight, Guidance.PanLeft),
        )
    }
}
