package dev.stpark.namchin.camera

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.stpark.namchin.core.live.CropRegion
import dev.stpark.namchin.core.live.LiveGuidance
import dev.stpark.namchin.core.live.LiveInput
import dev.stpark.namchin.core.live.LiveOutput
import dev.stpark.namchin.core.live.LiveState
import dev.stpark.namchin.core.live.Region
import dev.stpark.namchin.core.model.PersonPose
import dev.stpark.namchin.data.Reference
import dev.stpark.namchin.data.ReferenceRepository
import dev.stpark.namchin.ml.MlKitPoseMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * @property region 분석 프레임 안에서 레퍼런스 비율로 실제 찍힐 영역
 * @property current region 기준 좌표로 바꾼 현재 포즈
 */
data class CameraUiState(
    val reference: Reference? = null,
    val region: Region? = null,
    val current: PersonPose? = null,
    val output: LiveOutput? = null,
    val rollDegrees: Float = 0f,
    val analysisMs: Long = 0,
    val fps: Float = 0f,
)

class CameraViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = ReferenceRepository.get(app)
    private val _state = MutableStateFlow(CameraUiState())
    val state: StateFlow<CameraUiState> = _state.asStateFlow()
    private var live = LiveState()
    private var referenceId: Long? = null
    private var lastFrameAt = 0L

    /** 레퍼런스가 바뀌면 상태를 새로 시작한다. */
    fun start(referenceId: Long) {
        if (referenceId == this.referenceId) return
        this.referenceId = referenceId
        live = LiveState()
        _state.value = CameraUiState()
        viewModelScope.launch {
            val reference = repository.get(referenceId)
            _state.update { it.copy(reference = reference) }
        }
    }

    fun onTilt(degrees: Float) = _state.update { it.copy(rollDegrees = degrees) }

    fun onFrame(frame: PoseFrame) {
        val snapshot = _state.value
        val reference = snapshot.reference ?: return
        val now = SystemClock.elapsedRealtime()
        val region = CropRegion.centerCrop(frame.width.toFloat() / frame.height, reference.aspectRatio)
        val current = MlKitPoseMapper.toPersonPose(frame.landmarks, frame.width, frame.height)
            ?.let { CropRegion.remap(it, region) }
        val output = reference.pose?.let { refPose ->
            val (next, out) = LiveGuidance.step(
                live,
                LiveInput(refPose, current, snapshot.rollDegrees, poseEnabled = true, nowMs = now),
            )
            live = next
            out
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
    }
}
