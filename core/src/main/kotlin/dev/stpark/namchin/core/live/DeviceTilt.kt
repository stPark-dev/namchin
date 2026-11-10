package dev.stpark.namchin.core.live

import kotlin.math.atan2

object DeviceTilt {
    /**
     * 세로로 든 폰의 좌우 기울기. 시계 방향이 양수.
     * @param gx 중력(가속도) 센서 x값, @param gy y값 (기기 좌표계)
     */
    fun rollDegrees(gx: Float, gy: Float): Float = Math.toDegrees(atan2(-gx.toDouble(), gy.toDouble())).toFloat()
}
