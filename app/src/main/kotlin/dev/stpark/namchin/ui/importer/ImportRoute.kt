package dev.stpark.namchin.ui.importer

import android.net.Uri
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.stpark.namchin.R
import dev.stpark.namchin.importer.ImportUiState
import dev.stpark.namchin.importer.ImportViewModel

@Composable
fun ImportRoute(uri: Uri, onSaved: (Long) -> Unit, onCancel: () -> Unit) {
    val vm: ImportViewModel = viewModel(key = uri.toString())
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(uri) { vm.load(uri) }
    LaunchedEffect(state) { (state as? ImportUiState.Saved)?.let { onSaved(it.id) } }

    when (val s = state) {
        is ImportUiState.Cropping -> CropScreen(s, vm::updateCrop, vm::confirm, onCancel)
        is ImportUiState.Failed -> Centered {
            Text(stringResource(s.message))
            Button(onClick = onCancel) { Text(stringResource(R.string.back)) }
        }
        ImportUiState.Analyzing -> Centered {
            CircularProgressIndicator()
            Text(stringResource(R.string.import_analyzing))
        }
        ImportUiState.Loading, is ImportUiState.Saved -> Centered { CircularProgressIndicator() }
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
