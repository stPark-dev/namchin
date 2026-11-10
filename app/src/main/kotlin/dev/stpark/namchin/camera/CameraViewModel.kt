package dev.stpark.namchin.camera

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import dev.stpark.namchin.core.live.CropRegion
import dev.stpark.namchin.core.live.LiveGuidance
import dev.stpark.namchin.core.live.LiveInput
import dev.stpark.namchin.core.live.LiveOutput
import dev.stpark.namchin.core.live.LiveState
import dev.stpark.namchin.core.live.Region
import dev.stpark.namchin.core.model.PersonPose
import dev.stpark.namchin.data.Reference
import dev.stpark.namchin.ml.MlKitPoseMapper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * @property region 분석 프레임 안에서 레퍼런스 비율로 실제 찍힐 영역
 * @property current region 기준 좌표로 바꾼 현재 포즈
 */
data class CameraUiState(
    val reference: Reference? = null,
    val region: Region? = null,
    val current: PersonPose? = null,
    val output: LiveOutput? = null,
    val rollDegrees: Float? = null,
    val analysisMs: Long = 0,
    val fps: Float = 0f,
    val settings: FeedbackSettings = FeedbackSettings(),
)

class CameraViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(CameraUiState())
    val state: StateFlow<CameraUiState> = _state.asStateFlow()
    private var live = LiveState()
    private var lastFrameAt = 0L
    private var wasAligned = false
    private val _feedback = MutableSharedFlow<Feedback>(extraBufferCapacity = FEEDBACK_BUFFER)

    /** 말하기·진동·자동 셔터 이벤트 (한 번씩만 처리). */
    val feedback: SharedFlow<Feedback> = _feedback.asSharedFlow()

    /**
     * 화면에 들어오거나 레퍼런스를 바꿀 때마다 새로 시작한다.
     * 이미 메모리에 있는 레퍼런스를 받으므로 비동기 조회 경쟁이 없다.
     * @param reference null이면 기본 모드
     */
    fun start(reference: Reference?) {
        live = LiveState()
        lastFrameAt = 0L
        wasAligned = false
        _state.value = CameraUiState(reference = reference, settings = _state.value.settings)
    }

    fun updateSettings(settings: FeedbackSettings) = _state.update { it.copy(settings = settings) }

    /** 카드 목록이 열려 있는 동안 등 자동 셔터를 잠시 멈춘다. */
    private var autoShutterPaused = false
    fun pauseAutoShutter(paused: Boolean) {
        autoShutterPaused = paused
    }

    /** 자동 셔터가 발사됐지만 찍지 못했으면 다시 기다리게 한다. */
    fun onFireRejected() {
        live = LiveGuidance.cancelFire(live)
    }

    /** 수동이든 자동이든 찍었으면 자동 셔터를 잠시 쉬게 한다. */
    fun onShot() {
        live = LiveGuidance.afterManualShot(live, SystemClock.elapsedRealtime())
    }

    fun onTilt(degrees: Float?) = _state.update { it.copy(rollDegrees = degrees) }

    fun onFrame(frame: PoseFrame) {
        val snapshot = _state.value
        val reference = snapshot.reference
        val now = SystemClock.elapsedRealtime()
        val frameAspect = frame.width.toFloat() / frame.height
        // 같은 값이면 같은 인스턴스를 유지해 오버레이 정적 부분의 재구성을 건너뛰게 한다
        val region = CropRegion.centerCrop(frameAspect, reference?.aspectRatio ?: frameAspect)
            .let { if (it == snapshot.region) snapshot.region else it }
        val current = MlKitPoseMapper.toPersonPose(frame.landmarks, frame.width, frame.height)
            ?.let { CropRegion.remap(it, region) }
        val refPose = reference?.pose
        val (next, output) = when {
            reference == null -> LiveGuidance.stepBasic(live, current, snapshot.rollDegrees, now)
            refPose != null -> LiveGuidance.step(
                live,
                LiveInput(
                    refPose, current, snapshot.rollDegrees, poseEnabled = true, nowMs = now,
                    autoShutter = snapshot.settings.autoShutter && !autoShutterPaused,
                ),
            )
            else -> live to null
        }
        live = next
        output?.let { out ->
            FeedbackPlanner.plan(wasAligned, out, snapshot.settings).forEach { event ->
                // 셔터 이벤트가 버려지면 다시 기다리게 (안 그러면 포즈를 풀 때까지 안 찍힘)
                if (!_feedback.tryEmit(event) && event == Feedback.Fire) onFireRejected()
            }
            wasAligned = out.aligned
        }
        val instantFps = if (lastFrameAt > 0) 1000f / (now - lastFrameAt).coerceAtLeast(1) else 0f
        lastFrameAt = now
        _state.update {
            it.copy(
                region = region,
                current = current,
                output = output,
                analysisMs = frame.analysisMs,
                fps = if (it.fps == 0f) instantFps else it.fps * FPS_SMOOTHING + instantFps * (1 - FPS_SMOOTHING),
            )
        }
    }

    private companion object {
        const val FPS_SMOOTHING = 0.9f
        const val FEEDBACK_BUFFER = 8
    }
}
