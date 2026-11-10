package dev.stpark.namchin.core.live

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
}
