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
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
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
import androidx.compose.runtime.SideEffect
import dev.stpark.namchin.core.color.Filter
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.stpark.namchin.camera.LivePoseAnalyzer
import dev.stpark.namchin.camera.PoseFrame
import java.util.concurrent.Executors

/** 분석 해상도. 정확도와 속도 절충 — 실측 후 조정. */
private val ANALYSIS_SIZE = Size(1280, 960)

/**
 * 후면 카메라 4:3 프리뷰 + 포즈 분석. 프리뷰는 FIT_CENTER라서
 * 같은 4:3 박스 안의 정규화 좌표가 분석 좌표와 그대로 겹친다.
 */
@Composable
fun CameraPreview(
    onFrame: (PoseFrame) -> Unit,
    onScene: (Bitmap) -> Unit,
    onExposure: (Float) -> Unit,
    onCameraReady: (Camera, ImageCapture) -> Unit,
    modifier: Modifier = Modifier,
    filter: Filter? = null,
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
    SideEffect { previewView.setRenderEffect(effect) }
    val currentOnFrame by rememberUpdatedState(onFrame)
    val currentOnScene by rememberUpdatedState(onScene)
    val currentOnExposure by rememberUpdatedState(onExposure)
    val currentOnCameraReady by rememberUpdatedState(onCameraReady)
    var failed by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val executor = Executors.newSingleThreadExecutor()
        val analyzer = LivePoseAnalyzer(executor, onFrame = { currentOnFrame(it) }, onScene = { currentOnScene(it) })
        val future = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        var analysisUseCase: ImageAnalysis? = null
        var disposed = false
        future.addListener({
            if (disposed) return@addListener
            try {
                val p = future.get().also { provider = it }
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
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(executor, analyzer) }
                analysisUseCase = analysis
                val capture = ImageCapture.Builder()
                    .setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                            .build(),
                    )
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                p.unbindAll()
                val camera = p.bindToLifecycle(
                    lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis, capture,
                )
                camera.cameraInfo.zoomState.value?.let {
                    Log.i(TAG, "zoom range=${it.minZoomRatio}..${it.maxZoomRatio}")
                }
                Log.i(TAG, "capture=${capture.resolutionInfo?.resolution}")
                currentOnCameraReady(camera, capture)
                // 오버레이는 프리뷰와 분석이 같은 4:3 화각이라고 가정한다
                Log.i(TAG, "preview=${preview.resolutionInfo?.resolution} analysis=${analysis.resolutionInfo?.resolution}")
            } catch (e: Exception) {
                Log.e(TAG, "카메라 연결 실패", e)
                failed = true
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            disposed = true
            analysisUseCase?.clearAnalyzer()
            provider?.unbindAll()
            executor.shutdown()
            analyzer.close()
        }
    }
    Box(modifier) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        if (failed) {
            Text(stringResource(R.string.camera_error), color = Color.White, modifier = Modifier.align(Alignment.Center))
        }
    }
}

private const val TAG = "CameraPreview"
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
