package dev.stpark.namchin.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.stpark.namchin.R
import dev.stpark.namchin.data.Reference
import dev.stpark.namchin.ui.common.ReferenceImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    references: List<Reference>?,
    onBack: () -> Unit,
    onAddClick: () -> Unit,
    onReferenceClick: (Long) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Image(
                        painterResource(R.drawable.brand_logo),
                        contentDescription = stringResource(R.string.app_name),
                        modifier = Modifier.height(32.dp),
                    )
                },
                navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.library_to_camera)) } },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onAddClick) { Text(stringResource(R.string.home_add_reference)) }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when {
                references == null -> CircularProgressIndicator()
                references.isEmpty() -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource(R.drawable.brand_symbol), null, Modifier.size(140.dp))
                    Text(
                        stringResource(R.string.home_empty),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp),
                    )
                }
                else -> LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(3),
                    contentPadding = PaddingValues(8.dp),
                    verticalItemSpacing = 8.dp,
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(references, key = { it.id }) { ref ->
                        ReferenceImage(
                            ref,
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onReferenceClick(ref.id) },
                            stroke = 1.dp,
                        )
                    }
                }
            }
        }
    }
}
