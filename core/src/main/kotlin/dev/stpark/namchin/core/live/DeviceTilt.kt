package dev.stpark.namchin.core.live

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

object DeviceTilt {
    private const val GRAVITY = 9.81f

    /** 화면 평면 위 중력 성분이 이보다 작으면(폰을 거의 눕힘) 좌우 기울기가 의미 없다. */
    const val MIN_IN_PLANE_RATIO = 0.3f

    /**
     * 세로로 든 폰의 좌우 기울기. 시계 방향이 양수. 폰이 거의 누워 있으면 null.
     * @param gx 중력 센서 x값, @param gy y값 (기기 좌표계)
     */
    fun rollDegrees(gx: Float, gy: Float): Float? {
        if (hypot(gx, gy) < GRAVITY * MIN_IN_PLANE_RATIO) return null
        return Math.toDegrees(atan2(-gx.toDouble(), gy.toDouble())).toFloat()
    }

    /**
     * 세로로 든 폰의 앞뒤 기울기. 카메라가 아래(땅)를 보면 양수, 위(하늘)를 보면 음수. 좌우 기울기와 상관없다.
     * @param gz 중력 센서 z값 (화면이 하늘을 보면 +)
     */
    /** 이만큼 넘게 돌리면 거꾸로 든 것, 이 아래로 돌아오면 바로 든 것 */
    const val UPSIDE_DOWN_ENTER_DEG = 135f
    const val UPSIDE_DOWN_EXIT_DEG = 90f

    /**
     * 폰을 거꾸로 들었나 (렌즈를 바닥에 붙이는 로우앵글 촬영). 이 폰은 앱 화면을 180° 돌려 주지 않아 직접 판단한다.
     * @param deviceRoll 기기 기준 좌우 기울기 (rollDegrees), 모르면 null → 이전 판단 유지
     */
    fun upsideDown(deviceRoll: Float?, wasUpsideDown: Boolean): Boolean {
        val roll = deviceRoll?.let { abs(it) } ?: return wasUpsideDown
        return if (wasUpsideDown) roll >= UPSIDE_DOWN_EXIT_DEG else roll > UPSIDE_DOWN_ENTER_DEG
    }

    /**
     * 기기 기준 좌우 기울기를 화면 기준으로. 폰을 거꾸로 들어 화면이 180° 돌면 기기 기준 180°가 화면 기준 0°.
     * @param displayRotationDegrees 화면이 돌아간 각도 (0, 90, 180, 270)
     * @return -180 초과 180 이하
     */
    fun onScreen(deviceRoll: Float, displayRotationDegrees: Int): Float {
        val r = (deviceRoll + displayRotationDegrees) % 360f
        return when {
            r > 180f -> r - 360f
            r <= -180f -> r + 360f
            else -> r
        }
    }

    fun pitchDegrees(gx: Float, gy: Float, gz: Float): Float =
        Math.toDegrees(atan2(gz.toDouble(), hypot(gx, gy).toDouble())).toFloat()
}
