package dev.stpark.namchin.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.stpark.namchin.ui.camera.CameraScreen
import dev.stpark.namchin.ui.home.HomeScreen
import dev.stpark.namchin.ui.home.ReferencesViewModel
import dev.stpark.namchin.ui.importer.ImportRoute
import dev.stpark.namchin.ui.reference.ReferenceDetailScreen

private val ScreenSaver = listSaver<Screen, String>(
    save = { Screen.save(it) },
    restore = { Screen.restore(it) ?: Screen.Camera() },
)

/** @param sharedUri 다른 앱에서 공유로 받은 이미지 (처리 후 onSharedConsumed 호출) */
@Composable
fun NamchinApp(sharedUri: Uri?, onSharedConsumed: () -> Unit) {
    var screen by rememberSaveable(stateSaver = ScreenSaver) { mutableStateOf<Screen>(Screen.Camera()) }
    // 관리 화면에 다녀와도 적용 중이던 레퍼런스를 유지한다
    var cameraReferenceId by rememberSaveable { mutableStateOf<Long?>(null) }
    val referencesVm: ReferencesViewModel = viewModel()
    val references by referencesVm.references.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        uri?.let { screen = Screen.Import.new(it.toString()) }
    }
    fun toCamera(referenceId: Long? = cameraReferenceId) {
        cameraReferenceId = referenceId
        screen = Screen.Camera(referenceId)
    }

    LaunchedEffect(sharedUri) {
        sharedUri?.let {
            screen = Screen.Import.new(it.toString())
            onSharedConsumed()
        }
    }
    BackHandler(enabled = screen !is Screen.Camera) {
        when (screen) {
            is Screen.Detail, is Screen.Import -> screen = Screen.Library
            else -> toCamera()
        }
    }

    Surface {
        when (val s = screen) {
            is Screen.Camera -> {
                // 적용 중 레퍼런스가 삭제됐으면 기본 모드로
                val loaded = references
                LaunchedEffect(loaded, s.referenceId) {
                    if (loaded != null && s.referenceId != null && loaded.none { it.id == s.referenceId }) toCamera(null)
                }
                CameraScreen(
                    referenceId = s.referenceId,
                    references = references,
                    onSelectReference = ::toCamera,
                    onOpenLibrary = { screen = Screen.Library },
                )
            }
            Screen.Library -> HomeScreen(
                references = references,
                onBack = { toCamera() },
                onAddClick = { picker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) },
                onReferenceClick = { screen = Screen.Detail(it) },
            )
            is Screen.Import -> Box(Modifier.safeDrawingPadding()) {
                ImportRoute(
                    import = s,
                    onSaved = { screen = Screen.Detail(it) },
                    onCancel = { screen = Screen.Library },
                )
            }
            is Screen.Detail -> {
                val loaded = references
                val reference = loaded?.firstOrNull { it.id == s.id }
                // 목록은 불러왔는데 없으면(삭제됨) 관리 화면으로
                LaunchedEffect(loaded, reference) { if (loaded != null && reference == null) screen = Screen.Library }
                if (reference != null) {
                    ReferenceDetailScreen(
                        reference = reference,
                        onShoot = { toCamera(s.id) },
                        onDelete = {
                            if (s.id == cameraReferenceId) cameraReferenceId = null
                            referencesVm.delete(s.id)
                            screen = Screen.Library
                        },
                        onBack = { screen = Screen.Library },
                    )
                }
            }
        }
    }
}
