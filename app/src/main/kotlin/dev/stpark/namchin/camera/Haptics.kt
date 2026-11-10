package dev.stpark.namchin.camera

import android.content.Context
import android.os.VibrationEffect
import android.os.VibratorManager

/** 안내는 짧은 틱, 구도가 맞는 순간은 두 번. */
class Haptics(context: Context) {
    private val vibrator = context.getSystemService(VibratorManager::class.java)?.defaultVibrator

    fun tick() = play(VibrationEffect.EFFECT_TICK)

    fun aligned() = play(VibrationEffect.EFFECT_DOUBLE_CLICK)

    private fun play(effect: Int) {
        vibrator?.takeIf { it.hasVibrator() }?.vibrate(VibrationEffect.createPredefined(effect))
    }
}
