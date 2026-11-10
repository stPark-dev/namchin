package dev.stpark.namchin.core.framing

import dev.stpark.namchin.core.guidance.Guidance
import dev.stpark.namchin.core.guidance.Rotation
import kotlin.math.abs

/** 레퍼런스와 현재 구도를 비교해 필요한 안내를 모두 돌려준다. 순서는 의미 없음. */
object FramingAnalyzer {
    const val LEVEL_TOLERANCE_DEG = 3f
    const val SIZE_TOLERANCE = 0.10f
    const val POSITION_TOLERANCE = 0.05f

    /**
     * @param rollDegrees 폰이 시계 방향으로 기울어진 각도 (양수 = 시계 방향)
     */
    fun analyze(reference: PersonFrame, current: PersonFrame, rollDegrees: Float): List<Guidance> =
        listOfNotNull(
            levelCue(rollDegrees),
            distanceCue(reference, current),
            verticalCue(reference, current),
            horizontalCue(reference, current),
        )

    private fun levelCue(roll: Float): Guidance? = when {
        roll > LEVEL_TOLERANCE_DEG -> Guidance.Level(Rotation.COUNTERCLOCKWISE)
        roll < -LEVEL_TOLERANCE_DEG -> Guidance.Level(Rotation.CLOCKWISE)
        else -> null
    }

    private fun distanceCue(reference: PersonFrame, current: PersonFrame): Guidance? {
        val ratio = current.height / reference.height
        return when {
            ratio > 1 + SIZE_TOLERANCE -> Guidance.MoveBack
            ratio < 1 - SIZE_TOLERANCE -> Guidance.MoveCloser
            else -> null
        }
    }

    /** 인물이 화면에서 너무 위에 있으면 폰을 위로 기울여야 인물이 아래로 내려온다. */
    private fun verticalCue(reference: PersonFrame, current: PersonFrame): Guidance? {
        val offset = current.centerY - reference.centerY
        return when {
            abs(offset) <= POSITION_TOLERANCE -> null
            offset < 0 -> Guidance.TiltUp
            else -> Guidance.TiltDown
        }
    }

    /** 인물이 화면 왼쪽에 치우치면 폰을 왼쪽으로 돌려야 인물이 오른쪽으로 온다. */
    private fun horizontalCue(reference: PersonFrame, current: PersonFrame): Guidance? {
        val offset = current.centerX - reference.centerX
        return when {
            abs(offset) <= POSITION_TOLERANCE -> null
            offset < 0 -> Guidance.PanLeft
            else -> Guidance.PanRight
        }
    }
}
