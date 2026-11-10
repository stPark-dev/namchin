package dev.stpark.namchin.ui.importer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.stpark.namchin.R
import dev.stpark.namchin.importer.ImportUiState
import dev.stpark.namchin.importer.ImportViewModel
import dev.stpark.namchin.ui.Screen

@Composable
fun ImportRoute(import: Screen.Import, onSaved: (Long) -> Unit, onCancel: () -> Unit) {
    val vm: ImportViewModel = viewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(import.token) { vm.start(import.token, import.uri.toUri()) }
    LaunchedEffect(state) {
        (state as? ImportUiState.Saved)?.let {
            vm.reset()
            onSaved(it.id)
        }
    }
    val cancel = {
        vm.reset()
        onCancel()
    }

    when (val s = state) {
        is ImportUiState.Cropping -> CropScreen(s, vm::updateCrop, vm::confirm, cancel)
        is ImportUiState.Failed -> Centered {
            Text(stringResource(s.message))
            Button(onClick = cancel) { Text(stringResource(R.string.back)) }
        }
        ImportUiState.Analyzing -> Centered {
            CircularProgressIndicator()
            Text(stringResource(R.string.import_analyzing))
        }
        ImportUiState.Idle, ImportUiState.Loading, is ImportUiState.Saved -> Centered { CircularProgressIndicator() }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { content() }
}
