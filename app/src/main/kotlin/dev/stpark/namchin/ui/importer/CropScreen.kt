package dev.stpark.namchin.ui.importer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.stpark.namchin.R
import dev.stpark.namchin.importer.Corner
import dev.stpark.namchin.importer.CropGeometry
import dev.stpark.namchin.importer.ImportUiState
import dev.stpark.namchin.importer.NormRect

private val DIM = Color(0x99000000)

@Composable
fun CropScreen(
    state: ImportUiState.Cropping,
    onRectChange: (NormRect) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val image = remember(state.bitmap) { state.bitmap.asImageBitmap() }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            stringResource(if (state.autoDetected) R.string.crop_auto_detected else R.string.crop_auto_failed),
        )
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.aspectRatio(state.bitmap.width.toFloat() / state.bitmap.height)) {
                Image(image, null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                CropOverlay(state.rect, onRectChange, Modifier.fillMaxSize())
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.cancel)) }
            Button(onClick = onConfirm, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.crop_confirm)) }
        }
    }
}

@Composable
private fun CropOverlay(rect: NormRect, onRectChange: (NormRect) -> Unit, modifier: Modifier) {
    val currentRect by rememberUpdatedState(rect)
    val currentOnChange by rememberUpdatedState(onRectChange)
    Canvas(
        modifier.pointerInput(Unit) {
            var corner = Corner.TOP_LEFT
            detectDragGestures(
                onDragStart = { start -> corner = nearestCorner(currentRect, start, size.width, size.height) },
                onDrag = { change, drag ->
                    change.consume()
                    currentOnChange(CropGeometry.drag(currentRect, corner, drag.x / size.width, drag.y / size.height))
                },
            )
        },
    ) {
        val l = rect.left * size.width
        val t = rect.top * size.height
        val r = rect.right * size.width
        val b = rect.bottom * size.height
        drawRect(DIM, Offset.Zero, Size(size.width, t))
        drawRect(DIM, Offset(0f, b), Size(size.width, size.height - b))
        drawRect(DIM, Offset(0f, t), Size(l, b - t))
        drawRect(DIM, Offset(r, t), Size(size.width - r, b - t))
        drawRect(Color.White, Offset(l, t), Size(r - l, b - t), style = Stroke(2.dp.toPx()))
        listOf(Offset(l, t), Offset(r, t), Offset(l, b), Offset(r, b)).forEach {
            drawCircle(Color.White, radius = 10.dp.toPx(), center = it)
        }
    }
}

private fun nearestCorner(rect: NormRect, touch: Offset, width: Int, height: Int): Corner {
    val corners = mapOf(
        Corner.TOP_LEFT to Offset(rect.left * width, rect.top * height),
        Corner.TOP_RIGHT to Offset(rect.right * width, rect.top * height),
        Corner.BOTTOM_LEFT to Offset(rect.left * width, rect.bottom * height),
        Corner.BOTTOM_RIGHT to Offset(rect.right * width, rect.bottom * height),
    )
    return corners.minBy { (_, p) -> (p - touch).getDistanceSquared() }.key
}
