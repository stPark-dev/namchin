package dev.stpark.namchin.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.stpark.namchin.R
import dev.stpark.namchin.camera.CameraUiState
import dev.stpark.namchin.camera.CameraViewModel
import dev.stpark.namchin.camera.GuidanceText
import dev.stpark.namchin.camera.ZoomLogic
import androidx.camera.core.Camera
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.input.pointer.pointerInput
import dev.stpark.namchin.camera.tiltDegrees
import dev.stpark.namchin.core.framing.FramingAnalyzer
import dev.stpark.namchin.core.guidance.Guidance
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlin.math.abs

private const val ANALYSIS_ASPECT = 3f / 4f

@Composable
fun CameraScreen(referenceId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val request = rememberLauncherForActivityResult(RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (!granted) request.launch(Manifest.permission.CAMERA) }

    Box(Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding()) {
        if (granted) {
            CameraContent(referenceId, onBack)
        } else {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.camera_permission_needed), color = Color.White)
                Button(onClick = { request.launch(Manifest.permission.CAMERA) }) {
                    Text(stringResource(R.string.camera_permission_grant))
                }
                TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
            }
        }
    }
}

@Composable
private fun CameraContent(referenceId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val vm: CameraViewModel = viewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    var ghostAlpha by rememberSaveable { mutableFloatStateOf(0.35f) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var zoomRatio by rememberSaveable { mutableFloatStateOf(1f) }
    val zoomRange = camera?.cameraInfo?.zoomState?.value?.let { it.minZoomRatio..it.maxZoomRatio }
    fun setZoom(ratio: Float) {
        val range = zoomRange ?: return
        zoomRatio = ratio.coerceIn(range.start, range.endInclusive)
        camera?.cameraControl?.setZoomRatio(zoomRatio)
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(referenceId) { vm.start(referenceId) }
    // 화면이 보일 때만 센서 사용
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { tiltDegrees(context).collect(vm::onTilt) }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
            GuidanceBanner(state, Modifier.weight(1f))
        }
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(ANALYSIS_ASPECT)
                .pointerInput(zoomRange) {
                    val range = zoomRange ?: return@pointerInput
                    detectTransformGestures { _, _, scale, _ ->
                        setZoom(ZoomLogic.pinch(zoomRatio, scale, range.start, range.endInclusive))
                    }
                },
        ) {
            CameraPreview(
                onFrame = vm::onFrame,
                onCameraReady = {
                    camera = it
                    it.cameraControl.setZoomRatio(zoomRatio)
                },
                modifier = Modifier.fillMaxSize(),
            )
            val reference = state.reference
            val region = state.region
            if (reference != null && region != null) {
                StaticGuide(reference, region, ghostAlpha, Modifier.fillMaxSize())
                LiveSkeleton(state.current, region, state.output?.aligned == true, Modifier.fillMaxSize())
            }
            val roll = state.rollDegrees
            val level = state.output?.let { out -> out.cues.none { it is Guidance.Level } }
                ?: (roll == null || abs(roll) <= FramingAnalyzer.LEVEL_TOLERANCE_DEG)
            LevelIndicator(roll, level, Modifier.fillMaxSize())
        }
        zoomRange?.let { range ->
            ZoomButtons(
                presets = ZoomLogic.presets(range.start, range.endInclusive),
                current = zoomRatio,
                onSelect = ::setZoom,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.camera_ghost), color = Color.White)
            Slider(value = ghostAlpha, onValueChange = { ghostAlpha = it })
            Text(
                stringResource(
                    R.string.camera_stats,
                    state.analysisMs.toInt(),
                    state.fps,
                    state.output?.match?.let { "%.2f".format(it.score) } ?: "-",
                ),
                color = Color.Gray,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun ZoomButtons(presets: List<Float>, current: Float, onSelect: (Float) -> Unit, modifier: Modifier = Modifier) {
    val selected = ZoomLogic.selected(current, presets)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
        presets.forEach { preset ->
            val isSelected = preset == selected
            TextButton(
                onClick = { onSelect(preset) },
                modifier = Modifier.background(
                    if (isSelected) Color.White.copy(alpha = 0.25f) else Color.Transparent,
                    CircleShape,
                ),
            ) {
                Text(
                    if (isSelected) ZoomLogic.label(current) else ZoomLogic.label(preset),
                    color = if (isSelected) Color(0xFFFFD600) else Color.White,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun GuidanceBanner(state: CameraUiState, modifier: Modifier = Modifier) {
    val output = state.output
    val display = output?.display
    val text = when {
        state.reference != null && state.reference.pose == null -> stringResource(R.string.camera_no_reference_pose)
        output == null -> ""
        display != null -> stringResource(GuidanceText.of(display))
        output.aligned -> stringResource(R.string.camera_aligned)
        else -> ""
    }
    Text(
        text,
        modifier = modifier.padding(horizontal = 8.dp),
        color = if (output?.aligned == true) Color(0xFF00E676) else Color.White,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
    )
}
