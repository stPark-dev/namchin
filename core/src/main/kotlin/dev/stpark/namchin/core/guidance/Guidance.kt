package dev.stpark.namchin.core.guidance

import dev.stpark.namchin.core.model.BodyPart

enum class Rotation { CLOCKWISE, COUNTERCLOCKWISE }

/** 촬영 중 안내. priority가 낮을수록 먼저 안내한다. */
sealed class Guidance(val priority: Int) {
    /** 인물(어깨·골반)이 화면에서 인식되지 않음. */
    data object NoPerson : Guidance(0)
    data class Level(val rotation: Rotation) : Guidance(1)
    data object MoveCloser : Guidance(2)
    data object MoveBack : Guidance(2)
    data object TiltUp : Guidance(3)
    data object TiltDown : Guidance(3)
    data object PanLeft : Guidance(4)
    data object PanRight : Guidance(4)
    data class Pose(val part: BodyPart) : Guidance(5)
}
