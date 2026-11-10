package dev.stpark.namchin.core.guidance

import dev.stpark.namchin.core.model.BodyPart

enum class Rotation { CLOCKWISE, COUNTERCLOCKWISE }

/** 촬영 중 안내. priority가 낮을수록 먼저 안내한다. */
sealed class Guidance(val priority: Int) {
    data class Level(val rotation: Rotation) : Guidance(0)
    data object MoveCloser : Guidance(1)
    data object MoveBack : Guidance(1)
    data object TiltUp : Guidance(2)
    data object TiltDown : Guidance(2)
    data object PanLeft : Guidance(3)
    data object PanRight : Guidance(3)
    data class Pose(val part: BodyPart) : Guidance(4)
}
