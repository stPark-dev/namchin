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
import dev.stpark.namchin.camera.ShutterGate
import dev.stpark.namchin.camera.Feedback
import dev.stpark.namchin.camera.FeedbackSettings
import dev.stpark.namchin.camera.GuideVoice
import dev.stpark.namchin.camera.Haptics
import dev.stpark.namchin.settings.AppSettingsStore
import dev.stpark.namchin.ui.theme.Brand
import dev.stpark.namchin.ui.theme.Jua
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.SliderDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.LifecycleEventObserver
import java.util.concurrent.atomic.AtomicBoolean
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import dev.stpark.namchin.camera.ZoomLogic
import androidx.camera.core.Camera
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.pointerInput
import dev.stpark.namchin.camera.tiltDegrees
import dev.stpark.namchin.core.framing.FramingAnalyzer
import dev.stpark.namchin.core.guidance.Guidance
import android.util.Rational
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.camera.core.ImageCapture
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.platform.LocalView
import dev.stpark.namchin.capture.PhotoSaver
import dev.stpark.namchin.data.Reference
import kotlinx.coroutines.delay
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlin.math.abs

private const val ANALYSIS_ASPECT = 3f / 4f

/**
 * 시작 화면. referenceId가 null이면 기본 모드, 있으면 레퍼런스 모드.
 * @param references 레퍼런스 목록 (null = 로딩 중)
 */
@Composable
fun CameraScreen(
    referenceId: Long?,
    references: List<Reference>?,
    onSelectReference: (Long?) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val request = rememberLauncherForActivityResult(RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (!granted) request.launch(Manifest.permission.CAMERA) }

    Box(Modifier.fillMaxSize().background(Brand.Ink).safeDrawingPadding()) {
        if (granted) {
            CameraContent(referenceId, references, onSelectReference, onOpenLibrary, onOpenSettings)
        } else {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(painterResource(R.drawable.brand_symbol_on_dark), null, Modifier.size(140.dp))
                Text(stringResource(R.string.camera_permission_needed), color = Brand.Paper, fontFamily = Jua, fontSize = 18.sp)
                Button(onClick = { request.launch(Manifest.permission.CAMERA) }) {
                    Text(stringResource(R.string.camera_permission_grant))
                }
            }
        }
    }
}

@Composable
private fun CameraContent(
    referenceId: Long?,
    references: List<Reference>?,
    onSelectReference: (Long?) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val appSettings by remember { AppSettingsStore.get(context) }.settings.collectAsStateWithLifecycle()
    val all = references.orEmpty()
    val vm: CameraViewModel = viewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    var ghostAlpha by rememberSaveable { mutableFloatStateOf(0.35f) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var zoomRatio by rememberSaveable { mutableFloatStateOf(1f) }
    var deckOpen by rememberSaveable { mutableStateOf(false) }
    var capturing by remember { mutableStateOf(false) }
    var flash by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<Int?>(null) }
    val zoomRange = camera?.cameraInfo?.zoomState?.value?.let { it.minZoomRatio..it.maxZoomRatio }
    fun setZoom(ratio: Float) {
        val range = zoomRange ?: return
        zoomRatio = ratio.coerceIn(range.start, range.endInclusive)
        camera?.cameraControl?.setZoomRatio(zoomRatio)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    // 목록이 로드된 뒤 고른 레퍼런스로 시작 (id가 같으면 목록이 갱신돼도 다시 시작하지 않음)
    val loaded = references != null
    LaunchedEffect(referenceId, loaded) {
        if (referenceId == null || loaded) vm.start(references?.firstOrNull { it.id == referenceId })
    }
    // 화면이 보일 때만 센서 사용
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { tiltDegrees(context).collect(vm::onTilt) }
    }
    val reference = state.reference
    LaunchedEffect(flash) { if (flash) { delay(FLASH_MS); flash = false } }
    LaunchedEffect(message) { if (message != null) { delay(MESSAGE_MS); message = null } }
    BackHandler(enabled = deckOpen) { deckOpen = false }
    LaunchedEffect(deckOpen) { vm.pauseAutoShutter(deckOpen) }

    // 화면을 떠나거나 백그라운드로 가면 진행 중인 연속 촬영을 멈춘다
    val screenActive = remember { AtomicBoolean(true) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> screenActive.set(true)
                Lifecycle.Event.ON_STOP -> screenActive.set(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            screenActive.set(false)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val canShoot = ShutterGate.canShoot(referenceId, reference?.id, imageCapture != null, capturing)
    fun shoot(count: Int) {
        val capture = imageCapture ?: return
        if (!ShutterGate.canShoot(referenceId, reference?.id, true, capturing)) return
        // 저장되는 사진도 화면의 밝은 영역(레퍼런스 비율)과 같게 자른다. 비율은 촬영 시점에 읽힌다
        capture.setCropAspectRatio(reference?.let { Rational(it.width, it.height) } ?: Rational(3, 4))
        capturing = true
        flash = true
        vm.onShot()
        if (state.settings.vibrate) view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        PhotoSaver.takeBurst(context, capture, count, appSettings.shutterSound, isActive = { screenActive.get() }) { saved ->
            capturing = false
            message = when {
                saved == 0 -> R.string.camera_save_failed
                saved > 1 -> R.string.camera_saved_burst
                else -> R.string.camera_saved
            }
        }
    }

    // 음성·진동·자동 셔터
    val voice = remember { GuideVoice(context) }
    val haptics = remember { Haptics(context) }
    DisposableEffect(voice) { onDispose { voice.close() } }
    LaunchedEffect(state.settings.voice) { if (!state.settings.voice) voice.stop() }
    DisposableEffect(lifecycleOwner, voice) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) voice.stop() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val currentShoot by rememberUpdatedState(::shoot)
    val currentCanShoot by rememberUpdatedState(canShoot && !deckOpen)
    LaunchedEffect(vm) {
        vm.feedback.collect { event ->
            when (event) {
                is Feedback.Speak -> voice.speak(context.getString(GuidanceText.of(event.cue)))
                Feedback.Tick -> haptics.tick()
                Feedback.AlignedBuzz -> haptics.aligned()
                Feedback.Fire -> if (!currentCanShoot) vm.onFireRejected() else currentShoot(BURST_COUNT)
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(8.dp).heightIn(min = 72.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (reference != null) AppliedReferenceChip(reference, onClear = { onSelectReference(null) })
                GuidanceBanner(state, Modifier.weight(1f))
                BrandChip(stringResource(R.string.settings_title), selected = false, onClick = onOpenSettings)
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
                    onCameraReady = { cam, capture ->
                        camera = cam
                        imageCapture = capture
                        cam.cameraControl.setZoomRatio(zoomRatio)
                    },
                    modifier = Modifier.fillMaxSize(),
                )
                val region = state.region
                if (region != null && appSettings.grid) GridOverlay(region, Modifier.fillMaxSize())
                if (region != null) {
                    if (reference != null) StaticGuide(reference, region, ghostAlpha, Modifier.fillMaxSize())
                    LiveSkeleton(state.current, region, state.output?.aligned == true, Modifier.fillMaxSize())
                    ViewfinderCorners(region, aligned = state.output?.aligned == true, Modifier.fillMaxSize())
                }
                val roll = state.rollDegrees
                val level = state.output?.let { out -> out.cues.none { it is Guidance.Level } }
                    ?: (roll == null || abs(roll) <= FramingAnalyzer.LEVEL_TOLERANCE_DEG)
                LevelIndicator(roll, level, Modifier.fillMaxSize())
                zoomRange?.let { range ->
                    ZoomButtons(
                        presets = ZoomLogic.presets(range.start, range.endInclusive),
                        current = zoomRatio,
                        onSelect = ::setZoom,
                        modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 8.dp),
                    )
                }
                FeedbackToggles(
                    settings = state.settings,
                    showAutoShutter = reference?.pose != null,
                    onChange = vm::updateSettings,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                )
                if (flash) Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.7f)))
                message?.let {
                    Text(
                        stringResource(it),
                        color = Brand.Yellow,
                        fontFamily = Jua,
                        fontSize = 18.sp,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(Brand.Ink.copy(alpha = 0.9f), RoundedCornerShape(50))
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                    )
                }
            }
            if (reference != null) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.camera_ghost), color = Brand.Paper, fontSize = 14.sp, fontFamily = Jua)
                    Slider(
                        value = ghostAlpha,
                        onValueChange = { ghostAlpha = it },
                        colors = SliderDefaults.colors(
                            thumbColor = Brand.Yellow,
                            activeTrackColor = Brand.Yellow,
                            inactiveTrackColor = Brand.InkSoft,
                        ),
                        modifier = Modifier.padding(start = 12.dp).weight(1f),
                    )
                }
            }
            // 셔터 줄: 카드 손패는 이 칸 안의 오른쪽에만 둬서 다른 버튼 터치를 가로채지 않는다
            Box(Modifier.fillMaxWidth().weight(1f)) {
                ShutterBar(
                    latestReference = all.firstOrNull(),
                    enabled = canShoot,
                    onShutter = { shoot(1) },
                    onOpenLibrary = onOpenLibrary,
                    modifier = Modifier.fillMaxSize(),
                )
                Text(
                    stringResource(
                        R.string.camera_stats,
                        state.analysisMs.toInt(),
                        state.fps,
                        state.output?.match?.let { "%.2f".format(it.score) } ?: "-",
                    ),
                    color = Color.Gray,
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 4.dp),
                )
                CardHand(
                    references = all.filter { it.id != referenceId },
                    onSelect = { onSelectReference(it.id) },
                    onExpand = { deckOpen = true },
                    modifier = Modifier.align(Alignment.BottomEnd).width(150.dp).fillMaxHeight(),
                )
            }
        }
        if (deckOpen) {
            CardDeck(
                references = all,
                onSelect = {
                    deckOpen = false
                    onSelectReference(it.id)
                },
                onClose = { deckOpen = false },
                onAddReference = {
                    deckOpen = false
                    onOpenLibrary()
                },
            )
        }
    }
}

private const val FLASH_MS = 80L
private const val BURST_COUNT = 3
private const val MESSAGE_MS = 1_500L

@Composable
private fun FeedbackToggles(
    settings: FeedbackSettings,
    showAutoShutter: Boolean,
    onChange: (FeedbackSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.End) {
        Toggle(stringResource(R.string.toggle_voice), settings.voice) { onChange(settings.copy(voice = it)) }
        Toggle(stringResource(R.string.toggle_vibrate), settings.vibrate) { onChange(settings.copy(vibrate = it)) }
        if (showAutoShutter) {
            Toggle(stringResource(R.string.toggle_auto), settings.autoShutter) { onChange(settings.copy(autoShutter = it)) }
        }
    }
}

@Composable
private fun Toggle(label: String, on: Boolean, onToggle: (Boolean) -> Unit) {
    // 화면 읽기(TalkBack)가 스위치와 켜짐/꺼짐 상태를 읽도록
    BrandChip(label, selected = on, modifier = Modifier.toggleable(value = on, role = Role.Switch, onValueChange = onToggle))
}

/** 노랑(선택) / 반투명 남색(기본) 둥근 칩. 카메라 화면 버튼 공통 모양. */
@Composable
private fun BrandChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Text(
        label,
        color = if (selected) Brand.Ink else Brand.Paper,
        fontSize = 14.sp,
        fontFamily = Jua,
        modifier = modifier
            .background(if (selected) Brand.Yellow else Brand.Ink.copy(alpha = 0.6f), RoundedCornerShape(50))
            .border(1.dp, if (selected) Brand.Yellow else Brand.Paper.copy(alpha = 0.35f), RoundedCornerShape(50))
            .let { m -> if (onClick != null) m.clickable(onClick = onClick) else m }
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun ZoomButtons(presets: List<Float>, current: Float, onSelect: (Float) -> Unit, modifier: Modifier = Modifier) {
    val selected = ZoomLogic.selected(current, presets)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
        presets.forEach { preset ->
            val isSelected = preset == selected
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Brand.Yellow else Brand.Ink.copy(alpha = 0.55f))
                    .clickable { onSelect(preset) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (isSelected) ZoomLogic.label(current) else ZoomLogic.label(preset),
                    color = if (isSelected) Brand.Ink else Brand.Paper,
                    fontFamily = Jua,
                    fontSize = 14.sp,
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
        color = if (output?.aligned == true) Brand.Yellow else Brand.Paper,
        fontSize = 20.sp,
        fontFamily = Jua,
    )
}
