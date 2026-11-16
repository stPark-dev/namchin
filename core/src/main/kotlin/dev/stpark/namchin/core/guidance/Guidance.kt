package dev.stpark.namchin.core.guidance

import dev.stpark.namchin.core.model.BodyPart
import dev.stpark.namchin.core.pose.PoseDirection

enum class Rotation { CLOCKWISE, COUNTERCLOCKWISE }

/** 촬영 중 안내. priority가 낮을수록 먼저 안내한다. */
sealed class Guidance(val priority: Int) {
    /** 어두운 곳에서 손이 떨림 — 사진이 흔들리기 쉬우니 가장 먼저 안내. */
    data object HoldSteady : Guidance(0)

    /** 인물(어깨·골반)이 화면에서 인식되지 않음. */
    data object NoPerson : Guidance(0)
    data class Level(val rotation: Rotation) : Guidance(1)
    data object MoveCloser : Guidance(2)
    data object MoveBack : Guidance(2)

    /** 확대 촬영 설정인데 배율이 낮음 — 물러나서 ratio배로 당겨 찍기. 구도 전체가 바뀌므로 구도 안내보다 먼저. */
    data class TeleZoom(val ratio: Float) : Guidance(1)
    data object TiltUp : Guidance(3)
    data object TiltDown : Guidance(3)
    data object PanLeft : Guidance(4)
    data object PanRight : Guidance(4)
    /** @property direction 어떻게 고칠지 (관절이 안 보이면 null → 부위만 안내) */
    data class Pose(val part: BodyPart, val direction: PoseDirection.Direction? = null) : Guidance(5)

    // 기본 모드 구도 코칭
    enum class CutPoint { ANKLE, KNEE, WRIST }
    enum class Side { LEFT, RIGHT }

    /** 화면 가장자리가 관절을 지나감 (어색하게 잘림). */
    data class CutAtJoint(val point: CutPoint) : Guidance(2)

    /** 정수리가 화면 밖. */
    data object HeadCut : Guidance(2)

    /** 전신인데 발끝이 화면 아래에서 멂 — 폰을 낮추고 발끝을 아래에 맞추기. */
    data object FeetToBottom : Guidance(3)

    /** 인물이 아래를 채우는데 머리 위가 많이 빔. */
    data object TooMuchHeadroom : Guidance(3)

    /** 서 있는 전신인데 폰을 숙여 내려다봄 — 다리가 짧아 보임. 폰을 세워서. */
    data object PhoneTiltedDown : Guidance(3)

    /** 앉은 사람을 아래에서 올려다봄 — 폰을 얼굴보다 살짝 위로. */
    data object ShootFromAbove : Guidance(3)

    /** 인물이 side 쪽을 보는데 그쪽 여백이 부족함. */
    data class GazeSpace(val side: Side) : Guidance(4)

    // 한 번만 하는 한마디 (다른 안내가 없을 때만)

    /** 얼굴이 화면보다 한참 어두움 (역광·그늘) — 빛 쪽으로 돌려 보기. */
    data object FaceInShadow : Guidance(6)

    /** 두 팔을 곧게 내린 차렷 자세 — 손을 얼굴·허리로 가져가 삼각형 만들기. */
    data object TrianglePose : Guidance(7)
}
