package dev.stpark.namchin.camera

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.sqrt

private const val NANOS_PER_MS = 1_000_000L

/** 자이로 표본: 폰이 도는 속도(rad/s)와 센서 시각(ms). */
data class GyroSample(val speed: Float, val timeMs: Long)

/** 자이로 → 손떨림 판정용 표본. 센서가 없으면 아무것도 내보내지 않는다. */
fun angularSpeed(context: Context): Flow<GyroSample> = callbackFlow {
    val manager = context.getSystemService(SensorManager::class.java)
    val sensor = manager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    if (manager == null || sensor == null) {
        awaitClose()
        return@callbackFlow
    }
    val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val (x, y, z) = event.values
            trySend(GyroSample(sqrt(x * x + y * y + z * z), event.timestamp / NANOS_PER_MS))
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
    }
    manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
    awaitClose { manager.unregisterListener(listener) }
}
