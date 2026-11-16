package dev.stpark.namchin.core.composition

import dev.stpark.namchin.core.guidance.Guidance
import dev.stpark.namchin.core.model.Joint
import dev.stpark.namchin.core.model.PersonPose
import dev.stpark.namchin.core.model.Point
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * 폰 높이·앞뒤 기울기 코칭 (기류: 전신은 폰을 세워 수평으로, 앉은 사람은 살짝 위에서).
 * - 서 있는 전신을 폰을 숙여 내려다보면 다리가 짧아 보인다.
 * - 앉은 사람을 아래에서 올려다보면 턱·코가 커 보인다.
 * 서 있는 사람을 약간 낮은 데서 올려 찍는 건 다리가 길어 보이는 방식이라 말하지 않는다.
 */
object CameraAngle {
    private const val MIN_CONFIDENCE = 0.5f

    /** 카메라가 이만큼 아래를 보면 안내, 이 아래로 돌아오면 해제 */
    private const val DOWN_ENTER_DEG = 20f
    private const val DOWN_EXIT_DEG = 10f

    /** 카메라가 이만큼 위를 보면 안내 (음수), 이 위로 돌아오면 해제 */
    private const val UP_ENTER_DEG = -10f
    private const val UP_EXIT_DEG = -5f

    /** 허벅지가 수평에서 이 이내면 앉은 자세 */
    private const val SEATED_THIGH_DEG = 45.0

    /** 서 있는 다리: 허벅지가 수평에서 이 이상 (거의 세로) */
    private const val STANDING_THIGH_DEG = 60.0

    /** 서 있는 다리의 정강이/허벅지 길이 비율. 카메라를 보고 앉으면 허벅지가 짧게 찍혀 벗어난다 */
    private const val LEG_RATIO_MIN = 0.6f
    private const val LEG_RATIO_MAX = 1.6f

    /** @param pitchDegrees 카메라가 아래를 보면 양수 (DeviceTilt.pitchDegrees). 모르면 null */
    fun cue(pose: PersonPose, pitchDegrees: Float?, previous: Collection<Guidance>): Guidance? {
        val pitch = pitchDegrees ?: return null
        val seated = seated(pose)
        val downLimit = if (Guidance.PhoneTiltedDown in previous) DOWN_EXIT_DEG else DOWN_ENTER_DEG
        val upLimit = if (Guidance.ShootFromAbove in previous) UP_EXIT_DEG else UP_ENTER_DEG
        return when {
            !seated && standingFullBody(pose) && pitch > downLimit -> Guidance.PhoneTiltedDown
            seated && pitch < upLimit -> Guidance.ShootFromAbove
            else -> null
        }
    }

    private fun legs() = listOf(
        Triple(Joint.LEFT_HIP, Joint.LEFT_KNEE, Joint.LEFT_ANKLE),
        Triple(Joint.RIGHT_HIP, Joint.RIGHT_KNEE, Joint.RIGHT_ANKLE),
    )

    /** 다리 하나라도 골반→무릎→발목이 화면 안에 곧게 서 있다 (세로 허벅지, 자연스러운 정강이 길이). */
    private fun standingFullBody(pose: PersonPose) = legs().any { (h, k, a) ->
        val hip = pose.reliable(h, MIN_CONFIDENCE) ?: return@any false
        val knee = pose.reliable(k, MIN_CONFIDENCE) ?: return@any false
        val ankle = pose.reliable(a, MIN_CONFIDENCE) ?: return@any false
        val thigh = length(pose, hip, knee)
        hip.y < knee.y && knee.y < ankle.y && ankle.y <= 1f && thigh > 0f &&
            thighDegrees(pose, hip, knee) >= STANDING_THIGH_DEG &&
            length(pose, knee, ankle) / thigh in LEG_RATIO_MIN..LEG_RATIO_MAX
    }

    /** 두 허벅지가 모두 보이고 수평에 가깝다 (의자·바닥에 앉음). 한쪽만 보이면 발을 계단에 올린 것일 수 있어 아니라고 본다. */
    fun seated(pose: PersonPose): Boolean {
        val angles = legs().mapNotNull { (h, k, _) ->
            val hip = pose.reliable(h, MIN_CONFIDENCE) ?: return@mapNotNull null
            val knee = pose.reliable(k, MIN_CONFIDENCE) ?: return@mapNotNull null
            thighDegrees(pose, hip, knee)
        }
        return angles.size == 2 && angles.all { it < SEATED_THIGH_DEG }
    }

    /** 허벅지가 수평에서 몇 도 (실제 화면 비율로). */
    private fun thighDegrees(pose: PersonPose, hip: Point, knee: Point) =
        Math.toDegrees(atan2(abs(knee.y - hip.y).toDouble(), abs(knee.x - hip.x).toDouble() * pose.aspectRatio))

    private fun length(pose: PersonPose, a: Point, b: Point) = hypot((b.x - a.x) * pose.aspectRatio, b.y - a.y)
}
