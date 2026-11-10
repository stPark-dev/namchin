package dev.stpark.namchin.ui.reference

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.stpark.namchin.R
import dev.stpark.namchin.core.reference.SizeBasis
import dev.stpark.namchin.data.Reference
import dev.stpark.namchin.ui.common.ReferenceImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferenceDetailScreen(reference: Reference, onDelete: () -> Unit, onBack: () -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.detail_title)) },
                navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.back)) } },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ReferenceImage(reference, Modifier.fillMaxWidth())
            ProfileCard(reference)
            OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.delete))
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            text = { Text(stringResource(R.string.detail_delete_confirm)) },
            confirmButton = { TextButton(onClick = onDelete) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun ProfileCard(reference: Reference) {
    val profile = reference.profile
    val yes = stringResource(R.string.yes)
    val no = stringResource(R.string.no)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.profile_person, if (profile.personFound) yes else no))
            Text(
                stringResource(
                    R.string.profile_pose,
                    if (profile.poseComparable) yes else no,
                    profile.comparableParts,
                ),
            )
            Text(
                stringResource(
                    R.string.profile_size_basis,
                    stringResource(
                        when (profile.sizeBasis) {
                            SizeBasis.TORSO -> R.string.size_basis_torso
                            SizeBasis.NECK -> R.string.size_basis_neck
                            SizeBasis.NONE -> R.string.size_basis_none
                        },
                    ),
                ),
            )
            Text(stringResource(R.string.profile_size, reference.width, reference.height))
        }
    }
}
