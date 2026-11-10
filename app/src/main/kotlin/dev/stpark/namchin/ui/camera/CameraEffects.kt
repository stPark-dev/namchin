package dev.stpark.namchin.ui.camera

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import dev.stpark.namchin.camera.CameraViewModel
import dev.stpark.namchin.camera.Feedback
import dev.stpark.namchin.camera.GuideVoice
import dev.stpark.namchin.camera.GuidanceText
import dev.stpark.namchin.camera.Haptics
import dev.stpark.namchin.camera.angularSpeed
import dev.stpark.namchin.camera.tiltDegrees
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 화면이 앞에 있는지 (촬영 콜백 스레드에서도 읽을 수 있게 AtomicBoolean).
 * 화면을 떠나거나 백그라운드로 가면 false → 진행 중인 연속 촬영을 멈춘다.
 */
@Composable
internal fun rememberScreenActive(): AtomicBoolean {
    val lifecycleOwner = LocalLifecycleOwner.current
    val active = remember { AtomicBoolean(true) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> active.set(true)
                Lifecycle.Event.ON_STOP -> active.set(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            active.set(false)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    return active
}

/** 화면이 보일 때만 자이로·기울기 센서를 읽는다. */
@Composable
internal fun SensorEffect(vm: CameraViewModel, context: Context = LocalContext.current) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch { angularSpeed(context).collect(vm::onGyro) }
            tiltDegrees(context).collect(vm::onTilt)
        }
    }
}

/**
 * 안내 피드백: 음성·진동·자동 셔터.
 * @param canFire 지금 자동 셔터가 찍어도 되는지 (카드 목록·결과 화면 등이 떠 있으면 false)
 */
@Composable
internal fun FeedbackEffect(vm: CameraViewModel, voiceOn: Boolean, canFire: Boolean, fire: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val voice = remember { GuideVoice(context) }
    val haptics = remember { Haptics(context) }
    DisposableEffect(voice) { onDispose { voice.close() } }
    LaunchedEffect(voiceOn) { if (!voiceOn) voice.stop() }
    DisposableEffect(lifecycleOwner, voice) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) voice.stop() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val currentFire by rememberUpdatedState(fire)
    val currentCanFire by rememberUpdatedState(canFire)
    LaunchedEffect(vm) {
        vm.feedback.collect { event ->
            when (event) {
                is Feedback.Speak -> voice.speak(GuidanceText.text(context, event.cue))
                Feedback.Tick -> haptics.tick()
                Feedback.AlignedBuzz -> haptics.aligned()
                Feedback.Fire -> if (!currentCanFire) vm.onFireRejected() else currentFire()
            }
        }
    }
}
