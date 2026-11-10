package dev.stpark.namchin.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.util.Rational
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.camera.core.Camera
import androidx.camera.core.ImageCapture
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.stpark.namchin.R
import dev.stpark.namchin.camera.CameraViewModel
import dev.stpark.namchin.camera.CardOrder
import dev.stpark.namchin.camera.ShutterGate
import dev.stpark.namchin.camera.ZoomLogic
import dev.stpark.namchin.capture.PhotoSaver
import dev.stpark.namchin.core.tagging.Tag
import dev.stpark.namchin.data.Reference
import dev.stpark.namchin.ml.SceneEmbedder
import dev.stpark.namchin.review.ReviewState
import dev.stpark.namchin.review.ReviewViewModel
import dev.stpark.namchin.settings.AppSettingsStore
import dev.stpark.namchin.ui.review.ResultOverlay
import dev.stpark.namchin.ui.theme.Brand
import dev.stpark.namchin.ui.theme.Jua
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * 시작 화면. referenceId가 null이면 기본 모드, 있으면 레퍼런스 모드.
 * @param references 레퍼런스 목록 (null = 로딩 중)
 */
@Composable
fun CameraScreen(
    referenceId: Long?,
    references: List<Reference>?,
    learnedTags: Map<Long, Set<Tag>>,
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
            CameraContent(referenceId, references, learnedTags, onSelectReference, onOpenLibrary, onOpenSettings)
        } else {
            PermissionRequest(onGrant = { request.launch(Manifest.permission.CAMERA) })
        }
    }
}

@Composable
private fun PermissionRequest(onGrant: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(painterResource(R.drawable.brand_symbol_on_dark), null, Modifier.size(140.dp))
        Text(stringResource(R.string.camera_permission_needed), color = Brand.Paper, fontFamily = Jua, fontSize = 18.sp)
        Button(onClick = onGrant) { Text(stringResource(R.string.camera_permission_grant)) }
    }
}

@Composable
private fun CameraContent(
    referenceId: Long?,
    references: List<Reference>?,
    learnedTags: Map<Long, Set<Tag>>,
    onSelectReference: (Long?) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val settingsStore = remember { AppSettingsStore.get(context) }
    val appSettings by settingsStore.settings.collectAsStateWithLifecycle()
    val all = references.orEmpty()
    val vm: CameraViewModel = viewModel()
    val reviewVm: ReviewViewModel = viewModel()
    val review by reviewVm.state.collectAsStateWithLifecycle()
    val reviewing = review != ReviewState.Hidden
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

    // 목록이 로드된 뒤 고른 레퍼런스로 시작 (id가 같으면 목록이 갱신돼도 다시 시작하지 않음)
    val loaded = references != null
    LaunchedEffect(referenceId, loaded) {
        if (referenceId == null || loaded) vm.start(references?.firstOrNull { it.id == referenceId })
    }
    SensorEffect(vm)
    val reference = state.reference
    LaunchedEffect(all) { vm.updateReferences(all) }
    // 장면 임베딩 모델을 미리 불러 첫 장면 분석이 늦지 않게
    LaunchedEffect(Unit) { withContext(Dispatchers.Default) { SceneEmbedder.get(context) } }
    val orderedCards = remember(all, state.cardOrder) { CardOrder.arrange(all, state.cardOrder) { it.id } }
    LaunchedEffect(flash) { if (flash) { delay(FLASH_MS); flash = false } }
    LaunchedEffect(message) { if (message != null) { delay(MESSAGE_MS); message = null } }
    BackHandler(enabled = deckOpen) { deckOpen = false }
    BackHandler(enabled = reviewing) { reviewVm.close() }
    val scope = rememberCoroutineScope()
    val peek = remember { CardPeekState(scope) }
    val density = LocalDensity.current
    val peekDistancePx = with(density) { PEEK_DISTANCE.toPx() }
    var chipSlot by remember { mutableStateOf(Rect.Zero) }
    BackHandler(enabled = peek.active) { peek.dismiss() }
    // 카드 목록·결과 화면·크게 보기가 떠 있는 동안 자동 셔터를 멈춘다
    LaunchedEffect(deckOpen, reviewing, peek.active) { vm.pauseAutoShutter(deckOpen || reviewing || peek.active) }

    val screenActive = rememberScreenActive()
    val canShoot = ShutterGate.canShoot(referenceId, reference?.id, imageCapture != null, capturing)
    fun shoot(count: Int) {
        if (reviewing) return
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
            when {
                saved.isEmpty() -> message = R.string.camera_save_failed
                // 여러 장이거나 레퍼런스 모드면 결과 화면 (베스트 컷·레퍼런스 색감)
                (saved.size > 1 || reference != null) && screenActive.get() -> reviewVm.review(saved, reference)
                saved.size > 1 -> message = R.string.camera_saved_burst
                else -> message = R.string.camera_saved
            }
        }
    }

    FeedbackEffect(
        vm,
        voiceOn = state.settings.voice,
        canFire = canShoot && !deckOpen && !reviewing && !peek.active,
        fire = { shoot(BURST_COUNT) },
    )

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // 상단: 적용 중 레퍼런스(왼쪽), 설정(오른쪽). 안내 문구는 미리보기 위 캡슐로
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).heightIn(min = 64.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 칩 자리: 크게 보기에서 적용하면 카드가 이 자리로 날아온다
                Box(Modifier.size(CHIP_WIDTH, CHIP_HEIGHT).onGloballyPositioned { chipSlot = it.boundsInRoot() }) {
                    if (reference != null) AppliedReferenceChip(reference, onClear = { onSelectReference(null) })
                }
                Spacer(Modifier.weight(1f))
                if (state.lowLight) NightBadge(Modifier.padding(end = 8.dp))
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Rounded.Settings, stringResource(R.string.cd_settings), tint = Color.White)
                }
            }
            Viewfinder(
                vm = vm,
                state = state,
                grid = appSettings.grid,
                ghostAlpha = ghostAlpha,
                zoom = ZoomState(zoomRatio, zoomRange),
                onZoom = ::setZoom,
                onPinch = { scale ->
                    zoomRange?.let { setZoom(ZoomLogic.pinch(zoomRatio, scale, it.start, it.endInclusive)) }
                },
                onGrid = { settingsStore.update(appSettings.copy(grid = it)) },
                onCameraReady = { cam, capture ->
                    camera = cam
                    imageCapture = capture
                    cam.cameraControl.setZoomRatio(zoomRatio)
                },
                flash = flash,
                message = message,
            )
            if (reference != null) {
                GhostSlider(
                    alpha = ghostAlpha,
                    onChange = { ghostAlpha = it },
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp),
                )
            }
            BottomBar(
                state = state,
                latestReference = all.firstOrNull(),
                // 지금 장면과 배경이 비슷한 레퍼런스가 앞으로
                cards = orderedCards.filter { it.id != referenceId },
                shutterEnabled = canShoot && !reviewing,
                debugInfo = appSettings.debugInfo,
                peek = peek,
                peekDistancePx = peekDistancePx,
                onShutter = { shoot(1) },
                onLongShutter = { shoot(BURST_COUNT) },
                onOpenLibrary = onOpenLibrary,
                onExpandCards = { deckOpen = true },
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
        }
        PeekOverlay(
            state = peek,
            onApply = {
                // 칩 안의 카드 자리 (AppliedReferenceChip의 위·오른쪽 여백을 뺀 영역)
                val inset = with(density) { CHIP_CARD_INSET.toPx() }
                val card = Rect(chipSlot.left, chipSlot.top + inset, chipSlot.right - inset, chipSlot.bottom)
                peek.applyTo(card) { onSelectReference(it.id) }
            },
        )
        val colorPreview by reviewVm.color.collectAsStateWithLifecycle()
        ResultOverlay(
            state = review,
            color = colorPreview,
            onPreviewColor = reviewVm::previewColor,
            onClearColor = reviewVm::clearColor,
            onSaveColored = { id, strength, keepLightness ->
                message = R.string.color_saving
                reviewVm.saveColored(id, strength, keepLightness) { ok ->
                    message = if (ok) R.string.color_saved else R.string.color_save_failed
                }
            },
            onKeep = { id ->
                reviewVm.keepOnly(id) { failed ->
                    message = if (failed == 0) R.string.review_kept else R.string.review_keep_failed
                }
            },
            onClose = reviewVm::close,
        )
        if (deckOpen) {
            CardDeck(
                references = all,
                learnedTags = learnedTags,
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
private val CHIP_WIDTH = 64.dp
private val CHIP_HEIGHT = 80.dp
private val CHIP_CARD_INSET = 8.dp
private const val BURST_COUNT = 3
private const val MESSAGE_MS = 1_500L
