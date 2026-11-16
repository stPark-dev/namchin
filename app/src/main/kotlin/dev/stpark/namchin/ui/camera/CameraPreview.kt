package dev.stpark.namchin.ui.camera

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import android.annotation.SuppressLint
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.graphics.Bitmap
import android.util.Size
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import dev.stpark.namchin.R
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import dev.stpark.namchin.core.color.Filter
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.stpark.namchin.camera.LivePoseAnalyzer
import dev.stpark.namchin.camera.PoseFrame
import dev.stpark.namchin.camera.FocusPoint
import dev.stpark.namchin.camera.PortraitSupport
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** 분석 해상도. 정확도와 속도 절충 — 실측 후 조정. */
private val ANALYSIS_SIZE = Size(1280, 960)

/**
 * 후면 카메라 4:3 프리뷰 + 포즈 분석. 프리뷰는 FIT_CENTER라서
 * 같은 4:3 박스 안의 정규화 좌표가 분석 좌표와 그대로 겹친다.
 * @param portrait 배경 흐림 촬영 세션으로 연결 (찍는 동안만. 미리보기·촬영만 묶고 자세 분석은 쉰다)
 * @param onPortraitOffered 이 폰에서 배경 흐림을 쓸 수 있는지 (카메라를 연결할 때마다 알린다)
 * @param onPortraitActive 지금 연결된 카메라가 배경 흐림 세션인지
 * @param onPortraitFailed 배경 흐림 세션으로 연결하지 못해 일반 카메라로 되돌림
 * @param onUnbound 카메라가 풀림 (다시 연결되기 전까지 찍지 말 것)
 * @param focus 탭해서 고른 초점 위치. 바뀌면 그 자리에 초점·노출을 맞추고, 배경 흐림 세션으로 바꿀 때도 다시 맞춘다
 * @param upsideDown 폰을 거꾸로 들었음. 바깥 화면 전체가 180° 돌려 그려지므로 미리보기만 되돌려 실제 영상 그대로 보이게 하고,
 *   분석 영상·저장 사진은 사람이 바로 서게 180° 돌린다
 */
@Composable
fun CameraPreview(
    onFrame: (PoseFrame) -> Unit,
    onScene: (Bitmap) -> Unit,
    onExposure: (Float) -> Unit,
    onCameraReady: (Camera, ImageCapture) -> Unit,
    modifier: Modifier = Modifier,
    filter: Filter? = null,
    portrait: Boolean = false,
    upsideDown: Boolean = false,
    focus: FocusPoint? = null,
    onPortraitOffered: (Boolean) -> Unit = {},
    onPortraitActive: (Boolean) -> Unit = {},
    onPortraitFailed: () -> Unit = {},
    onUnbound: () -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FIT_CENTER
            // 필터 미리보기(RenderEffect)는 일반 View 그리기(TextureView)에만 입혀진다.
            // 필터가 없어도 이 방식이라 SurfaceView보다 화면이 한 프레임쯤 늦을 수 있다 (실측 후 판단)
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val effect = remember(filter) { filter?.let(LiveFilter::effect) }
    // 필터는 카메라 영상(안쪽 TextureView)에만 입힌다. PreviewView 전체에 입히면
    // 바깥 검은 여백까지 물들어 그 위에 반투명하게 덮인 위아래 띠 색이 변한다.
    // TextureView는 카메라가 붙을 때 새로 들어오므로, 들어올 때마다 다시 입힌다.
    val currentEffect by rememberUpdatedState(effect)
    DisposableEffect(previewView) {
        previewView.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
            override fun onChildViewAdded(parent: View, child: View) = child.setRenderEffect(currentEffect)
            override fun onChildViewRemoved(parent: View, child: View) = Unit
        })
        onDispose { previewView.setOnHierarchyChangeListener(null) }
    }
    SideEffect { previewView.children.forEach { it.setRenderEffect(effect) } }
    val currentOnFrame by rememberUpdatedState(onFrame)
    val currentOnScene by rememberUpdatedState(onScene)
    val currentOnExposure by rememberUpdatedState(onExposure)
    val currentOnCameraReady by rememberUpdatedState(onCameraReady)
    val currentOnPortraitOffered by rememberUpdatedState(onPortraitOffered)
    val currentOnPortraitActive by rememberUpdatedState(onPortraitActive)
    val currentOnPortraitFailed by rememberUpdatedState(onPortraitFailed)
    val currentOnUnbound by rememberUpdatedState(onUnbound)
    var failed by remember { mutableStateOf(false) }
    // 지금 묶인 분석·촬영 (거꾸로 들기에 맞춰 방향을 바꾸려고)
    val bound = remember { BoundUseCases() }
    val currentUpsideDown by rememberUpdatedState(upsideDown)
    fun rotationFor(upside: Boolean) = if (upside) Surface.ROTATION_180 else Surface.ROTATION_0
    SideEffect {
        bound.analysis?.targetRotation = rotationFor(upsideDown)
        bound.capture?.targetRotation = rotationFor(upsideDown)
    }
    val currentFocus by rememberUpdatedState(focus)
    fun applyFocus(point: FocusPoint) {
        val camera = bound.camera ?: return
        if (previewView.width == 0 || previewView.height == 0) return
        val metering = previewView.meteringPointFactory.createPoint(point.x * previewView.width, point.y * previewView.height)
        val action = FocusMeteringAction.Builder(metering, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
            .setAutoCancelDuration(FOCUS_HOLD_SECONDS, TimeUnit.SECONDS)
            .build()
        // 배경 흐림 세션 등 초점 영역을 못 받는 경우는 조용히 넘어간다
        runCatching { camera.cameraControl.startFocusAndMetering(action) }
            .onFailure { Log.w(TAG, "초점 맞추기 실패", it) }
    }
    LaunchedEffect(focus) { focus?.let(::applyFocus) }

    // 배경 흐림 촬영 세션을 켜고 끌 때마다 카메라를 다시 연결한다
    DisposableEffect(lifecycleOwner, portrait) {
        val executor = Executors.newSingleThreadExecutor()
        val analyzer = LivePoseAnalyzer(executor, onFrame = { currentOnFrame(it) }, onScene = { currentOnScene(it) })
        val future = ProcessCameraProvider.getInstance(context)
        val back = CameraSelector.DEFAULT_BACK_CAMERA
        var provider: ProcessCameraProvider? = null
        var analysisUseCase: ImageAnalysis? = null
        var watched: Camera? = null
        var disposed = false

        fun unwatch() {
            watched?.cameraInfo?.cameraState?.removeObservers(lifecycleOwner)
            watched = null
        }


        /**
         * @param bokeh 배경 흐림 촬영 세션 (미리보기와 촬영만, 분석 없음).
         * 묶기나 세션 시작에 실패하면 일반 카메라로 되돌린다.
         */
        fun bind(p: ProcessCameraProvider, selector: CameraSelector, bokeh: Boolean) {
            fun fallBack(reason: Throwable?) {
                Log.w(TAG, "배경 흐림 세션 실패, 일반 카메라로", reason)
                unwatch()
                currentOnPortraitFailed()
                bind(p, back, bokeh = false)
            }
            try {
                val rotation = rotationFor(currentUpsideDown)
                val ratio = ResolutionSelector.Builder()
                    .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                val preview = Preview.Builder().setResolutionSelector(ratio.build())
                    .also { monitorCapture(it) { exposure -> currentOnExposure(exposure) } }
                    .build()
                    .also { it.surfaceProvider = previewView.surfaceProvider }
                val analysis = ImageAnalysis.Builder()
                    .setResolutionSelector(
                        ratio.setResolutionStrategy(
                            ResolutionStrategy(ANALYSIS_SIZE, ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER),
                        ).build(),
                    )
                    .setTargetRotation(rotation)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(executor, analyzer) }
                analysisUseCase?.clearAnalyzer()
                analysisUseCase = analysis
                bound.analysis = analysis
                val capture = ImageCapture.Builder()
                    .setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                            .build(),
                    )
                    .setTargetRotation(rotation)
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                bound.capture = capture
                p.unbindAll()
                val camera = try {
                    if (bokeh) {
                        analysis.clearAnalyzer()
                        p.bindToLifecycle(lifecycleOwner, selector, preview, capture)
                    } else {
                        p.bindToLifecycle(lifecycleOwner, selector, preview, analysis, capture)
                    }
                } catch (e: Exception) {
                    if (!bokeh) throw e
                    return fallBack(e)
                }
                Log.i(TAG, "bokeh session=$bokeh rotation=$rotation")
                camera.cameraInfo.zoomState.value?.let {
                    Log.i(TAG, "zoom range=${it.minZoomRatio}..${it.maxZoomRatio}")
                }
                Log.i(TAG, "capture=${capture.resolutionInfo?.resolution}")
                bound.camera = camera
                currentOnCameraReady(camera, capture)
                // 배경 흐림으로 바꿔 찍을 때도 고른 주인공에 초점 (흐림 처리 기준)
                currentFocus?.let(::applyFocus)
                currentOnPortraitActive(bokeh)
                // 확장 세션은 묶은 뒤 설정 단계에서 실패할 수 있다 (예외 없이 카메라 상태 오류로 온다)
                if (bokeh) {
                    watched = camera
                    camera.cameraInfo.cameraState.observe(lifecycleOwner) { state ->
                        if (!disposed && watched === camera && state.error != null) fallBack(null)
                    }
                }
                // 오버레이는 프리뷰와 분석이 같은 4:3 화각이라고 가정한다
                Log.i(TAG, "preview=${preview.resolutionInfo?.resolution} analysis=${analysis.resolutionInfo?.resolution}")
            } catch (e: Exception) {
                Log.e(TAG, "카메라 연결 실패", e)
                failed = true
            }
        }
        val main = ContextCompat.getMainExecutor(context)
        future.addListener({
            if (disposed) return@addListener
            val p = try {
                future.get().also { provider = it }
            } catch (e: Exception) {
                Log.e(TAG, "카메라 연결 실패", e)
                failed = true
                return@addListener
            }
            // 평소엔 바로 일반 카메라로 (확장 라이브러리 준비를 기다리지 않는다)
            if (!portrait) bind(p, back, bokeh = false)
            val extensionsFuture = ExtensionsManager.getInstanceAsync(context, p)
            extensionsFuture.addListener({
                if (disposed) return@addListener
                val extensions = runCatching { extensionsFuture.get() }
                    .onFailure { Log.w(TAG, "카메라 확장 확인 실패", it) }
                    .getOrNull()
                val bokehAvailable = extensions != null &&
                    runCatching { extensions.isExtensionAvailable(back, ExtensionMode.BOKEH) }.getOrDefault(false)
                val offered = PortraitSupport.offered(bokeh = bokehAvailable)
                Log.i(TAG, "portrait support: extensions=${extensions != null} bokeh=$bokehAvailable")
                currentOnPortraitOffered(offered)
                if (portrait) {
                    if (extensions != null && offered) {
                        bind(p, extensions.getExtensionEnabledCameraSelector(back, ExtensionMode.BOKEH), bokeh = true)
                    } else {
                        currentOnPortraitFailed()
                        bind(p, back, bokeh = false)
                    }
                }
            }, main)
        }, main)

        onDispose {
            disposed = true
            bound.analysis = null
            bound.capture = null
            bound.camera = null
            unwatch()
            // 풀린 카메라로 찍으려 하지 않게 먼저 알린다
            currentOnUnbound()
            analysisUseCase?.clearAnalyzer()
            provider?.unbindAll()
            executor.shutdown()
            analyzer.close()
        }
    }
    Box(modifier) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = if (upsideDown) UPSIDE_DOWN_DEG else 0f },
        )
        if (failed) {
            Text(stringResource(R.string.camera_error), color = Color.White, modifier = Modifier.align(Alignment.Center))
        }
    }
}

private const val TAG = "CameraPreview"
private const val UPSIDE_DOWN_DEG = 180f

/** 지금 묶인 분석·촬영 쓰임새 (방향을 바꿀 때 쓴다). 메인 스레드에서만 다룬다 */
private class BoundUseCases {
    var analysis: ImageAnalysis? = null
    var capture: ImageCapture? = null
    var camera: Camera? = null
}

/** 탭한 초점을 이만큼 유지하고 자동 초점으로 돌아간다 */
private const val FOCUS_HOLD_SECONDS = 5L

private const val NANOS_PER_SECOND = 1_000_000_000f

/**
 * 프레임마다: 카메라가 쓰는 감도×노출시간(야간 판정)을 알리고,
 * 줌에 따라 실제로 어느 물리 렌즈(초광각/기본/망원)가 쓰이는지 로그로 남긴다.
 */
@SuppressLint("UnsafeOptInUsageError")
@androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
private fun monitorCapture(builder: Preview.Builder, onExposure: (Float) -> Unit) {
    var lastLens: String? = null
    Camera2Interop.Extender(builder).setSessionCaptureCallback(
        object : CameraCaptureSession.CaptureCallback() {
            override fun onCaptureCompleted(
                session: CameraCaptureSession,
                request: CaptureRequest,
                result: TotalCaptureResult,
            ) {
                val iso = result.get(CaptureResult.SENSOR_SENSITIVITY)
                val exposureNs = result.get(CaptureResult.SENSOR_EXPOSURE_TIME)
                if (iso != null && exposureNs != null) onExposure(iso * (exposureNs / NANOS_PER_SECOND))
                val lens = result.get(CaptureResult.LOGICAL_MULTI_CAMERA_ACTIVE_PHYSICAL_ID)
                if (lens != lastLens) {
                    lastLens = lens
                    Log.i(TAG, "active lens=$lens zoom=${result.get(CaptureResult.CONTROL_ZOOM_RATIO)}")
                }
            }
        },
    )
}
