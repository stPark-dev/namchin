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
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.stpark.namchin.ui.home.HomeScreen
import dev.stpark.namchin.ui.home.ReferencesViewModel
import dev.stpark.namchin.ui.importer.ImportRoute
import dev.stpark.namchin.ui.reference.ReferenceDetailScreen

private val ScreenSaver = listSaver<Screen, String>(
    save = { Screen.save(it) },
    restore = { Screen.restore(it) ?: Screen.Home },
)

/** @param sharedUri 다른 앱에서 공유로 받은 이미지 (처리 후 onSharedConsumed 호출) */
@Composable
fun NamchinApp(sharedUri: Uri?, onSharedConsumed: () -> Unit) {
    var screen by rememberSaveable(stateSaver = ScreenSaver) { mutableStateOf<Screen>(Screen.Home) }
    val referencesVm: ReferencesViewModel = viewModel()
    val references by referencesVm.references.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        uri?.let { screen = Screen.Import.new(it.toString()) }
    }

    LaunchedEffect(sharedUri) {
        sharedUri?.let {
            screen = Screen.Import.new(it.toString())
            onSharedConsumed()
        }
    }
    BackHandler(enabled = screen != Screen.Home) { screen = Screen.Home }

    Surface {
        when (val s = screen) {
            Screen.Home -> HomeScreen(
                references = references,
                onAddClick = { picker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) },
                onReferenceClick = { screen = Screen.Detail(it) },
            )
            is Screen.Import -> Box(Modifier.safeDrawingPadding()) {
                ImportRoute(
                    import = s,
                    onSaved = { screen = Screen.Detail(it) },
                    onCancel = { screen = Screen.Home },
                )
            }
            is Screen.Detail -> {
                val loaded = references
                val reference = loaded?.firstOrNull { it.id == s.id }
                // 목록은 불러왔는데 없으면(삭제됨) 홈으로
                LaunchedEffect(loaded, reference) { if (loaded != null && reference == null) screen = Screen.Home }
                if (reference != null) {
                    ReferenceDetailScreen(
                        reference = reference,
                        onDelete = {
                            referencesVm.delete(s.id)
                            screen = Screen.Home
                        },
                        onBack = { screen = Screen.Home },
                    )
                }
            }
        }
    }
}
