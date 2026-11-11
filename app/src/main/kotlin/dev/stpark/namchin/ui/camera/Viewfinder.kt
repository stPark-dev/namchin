package dev.stpark.namchin.ui.camera

import androidx.camera.core.Camera
import androidx.camera.core.ImageCapture
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.stpark.namchin.R
import dev.stpark.namchin.camera.CameraViewModel
import dev.stpark.namchin.core.color.Filter
import dev.stpark.namchin.review.Look
import dev.stpark.namchin.ui.common.lookLabel
import dev.stpark.namchin.camera.CameraUiState
import dev.stpark.namchin.camera.ZoomLogic
import dev.stpark.namchin.core.framing.FramingAnalyzer
import dev.stpark.namchin.core.guidance.Guidance
import kotlin.math.abs

private const val ANALYSIS_ASPECT = 3f / 4f

/** 줌 상태: 현재 배율과 카메라가 지원하는 범위 (카메라 준비 전엔 range = null). 표시용. */
internal data class ZoomState(val ratio: Float, val range: ClosedFloatingPointRange<Float>?)

/**
 * 3:4 미리보기와 그 위의 모든 겹침: 격자·레퍼런스 고스트·골격·꺾쇠·수평계·안내 캡슐·아이콘 줄·줌·플래시·토스트.
 */
@Composable
internal fun Viewfinder(
    vm: CameraViewModel,
    state: CameraUiState,
    grid: Boolean,
    ghostAlpha: Float,
    zoom: ZoomState,
    onZoom: (Float) -> Unit,
    onPinch: (Float) -> Unit,
    onGrid: (Boolean) -> Unit,
    onCameraReady: (Camera, ImageCapture) -> Unit,
    flash: Boolean,
    message: Int?,
) {
    val reference = state.reference
    val aligned = state.output?.aligned == true
    // 제스처 블록은 범위가 바뀔 때만 다시 시작하므로 콜백은 최신 것을 부른다.
    // 배율 계산은 부모가 실제 상태로 한다 (여기서 받은 zoom.ratio는 다음 화면 갱신 전까지 옛 값)
    val currentOnPinch by rememberUpdatedState(onPinch)
    // 구도 틀 / 필터 고르기 줄 (한 번에 하나만)
    var picker by rememberSaveable { mutableStateOf(Picker.NONE) }
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(ANALYSIS_ASPECT)
            .pointerInput(zoom.range) {
                if (zoom.range == null) return@pointerInput
                detectTransformGestures { _, _, scale, _ -> currentOnPinch(scale) }
            },
    ) {
        CameraPreview(
            onFrame = vm::onFrame,
            onScene = vm::onSceneFrame,
            onExposure = vm::onExposure,
            onCameraReady = onCameraReady,
            modifier = Modifier.fillMaxSize(),
            filter = state.filter,
        )
        val region = state.region
        if (region != null && grid) GridOverlay(region, Modifier.fillMaxSize())
        if (region != null) {
            if (reference != null) StaticGuide(reference, region, ghostAlpha, Modifier.fillMaxSize())
            else OutsideDim(region, Modifier.fillMaxSize())
            if (reference == null && state.template != null) {
                TemplateOverlay(state.template, state.templateSide, region, aligned, Modifier.fillMaxSize())
            }
            LiveSkeleton(state.current, region, aligned, Modifier.fillMaxSize())
            ViewfinderCorners(region, aligned = aligned, Modifier.fillMaxSize())
        }
        val roll = state.rollDegrees
        val level = state.output?.let { out -> out.cues.none { it is Guidance.Level } }
            ?: (roll == null || abs(roll) <= FramingAnalyzer.LEVEL_TOLERANCE_DEG)
        LevelIndicator(roll, level, Modifier.fillMaxSize())
        GuidanceCapsule(
            state,
            // 위쪽 꺾쇠 사이 가운데 (꺾쇠·오른쪽 아이콘 줄과 겹치지 않게 양쪽 여백 동일)
            Modifier.align(Alignment.TopCenter).padding(top = 16.dp, start = 64.dp, end = 64.dp),
        )
        ControlRail(
            settings = state.settings,
            grid = grid,
            showAutoShutter = reference?.pose != null,
            onChange = vm::updateSettings,
            onGrid = onGrid,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 10.dp),
            templateLabel = if (reference == null) {
                stringResource(R.string.cd_template_current, stringResource(templateLabel(state.template)))
            } else {
                null
            },
            templateActive = state.template != null,
            onTemplate = { picker = if (picker == Picker.TEMPLATE) Picker.NONE else Picker.TEMPLATE },
            filterLabel = stringResource(R.string.cd_filter_current, stringResource(lookLabel(state.filter?.let(Look::Preset)))),
            filterActive = state.filter != null,
            onFilter = { picker = if (picker == Picker.FILTER) Picker.NONE else Picker.FILTER },
        )
        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp, start = 12.dp, end = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (reference == null && picker == Picker.TEMPLATE) {
                TemplatePicker(state.template, onSelect = { vm.setTemplate(it); picker = Picker.NONE })
            }
            if (picker == Picker.FILTER) {
                ChoicePicker(
                    listOf(null) + Filter.entries, state.filter,
                    label = { f -> lookLabel(f?.let(Look::Preset)) },
                    onSelect = { vm.setFilter(it); picker = Picker.NONE },
                )
            }
            zoom.range?.let { range ->
                ZoomCapsule(
                    presets = ZoomLogic.presets(range.start, range.endInclusive),
                    current = zoom.ratio,
                    onSelect = onZoom,
                )
            }
        }
        if (flash) Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.7f)))
        message?.let { Toast(stringResource(it), Modifier.align(Alignment.Center)) }
    }
}

private enum class Picker { NONE, TEMPLATE, FILTER }
