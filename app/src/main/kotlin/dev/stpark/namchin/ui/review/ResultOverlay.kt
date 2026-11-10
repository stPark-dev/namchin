package dev.stpark.namchin.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.stpark.namchin.R
import dev.stpark.namchin.core.bestshot.ReviewItem
import dev.stpark.namchin.data.Reference
import dev.stpark.namchin.review.ReviewState
import dev.stpark.namchin.ui.theme.Brand
import java.io.File

private val SHAPE = RoundedCornerShape(14.dp)

/** 연속 촬영 결과: 베스트 컷 크게, 레퍼런스와 나란히 비교, 아래 줄에서 다른 컷 보기. */
@Composable
fun ResultOverlay(state: ReviewState, onKeepBest: () -> Unit, onClose: () -> Unit) {
    if (state == ReviewState.Hidden) return
    Box(
        Modifier
            .fillMaxSize()
            .background(Brand.Ink)
            // 아래 카메라로 터치가 새지 않게
            .clickable(enabled = false) {}
            .safeDrawingPadding(),
    ) {
        when (state) {
            ReviewState.Analyzing -> Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator(color = Brand.Yellow)
                Text(stringResource(R.string.review_analyzing), color = Color.White)
            }
            is ReviewState.Ready -> ReadyContent(state, onKeepBest, onClose)
            ReviewState.Hidden -> Unit
        }
    }
}

@Composable
private fun ReadyContent(state: ReviewState.Ready, onKeepBest: () -> Unit, onClose: () -> Unit) {
    var selectedId by remember(state) { mutableStateOf(state.items.firstOrNull { it.best }?.id) }
    var compare by remember(state) { mutableStateOf(state.reference != null) }
    val selected = state.items.firstOrNull { it.id == selectedId } ?: state.items.firstOrNull() ?: return
    val reference = state.reference

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.review_title),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, null, tint = Color.White) }
        }
        if (reference != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.review_compare), color = Color.White.copy(alpha = 0.8f), modifier = Modifier.weight(1f))
                Switch(
                    checked = compare,
                    onCheckedChange = { compare = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = Brand.Yellow, checkedThumbColor = Brand.Ink),
                )
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (compare && reference != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Labeled(stringResource(R.string.review_reference), Modifier.weight(1f)) {
                        AsyncImage(File(reference.imagePath), null, Modifier.fillMaxWidth().aspectRatio(reference.aspectRatio).clip(SHAPE), contentScale = ContentScale.Crop)
                    }
                    Labeled(stringResource(R.string.review_best), Modifier.weight(1f)) {
                        AsyncImage(selected.id, null, Modifier.fillMaxWidth().aspectRatio(reference.aspectRatio).clip(SHAPE), contentScale = ContentScale.Crop)
                    }
                }
            } else {
                AsyncImage(selected.id, null, Modifier.fillMaxSize().clip(SHAPE), contentScale = ContentScale.Fit)
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.items, key = { it.id }) { item ->
                Thumb(item, selected = item.id == selected.id) { selectedId = item.id }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onClose, modifier = Modifier.weight(1f).height(52.dp)) {
                Text(stringResource(R.string.review_keep_all), color = Color.White)
            }
            Button(
                onClick = onKeepBest,
                colors = ButtonDefaults.buttonColors(containerColor = Brand.Yellow, contentColor = Brand.Ink),
                modifier = Modifier.weight(1f).height(52.dp),
            ) {
                Text(stringResource(R.string.review_keep_best), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun Labeled(label: String, modifier: Modifier, content: @Composable () -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
        content()
    }
}

@Composable
private fun Thumb(item: ReviewItem, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        Modifier
            .size(72.dp, 96.dp)
            .clip(shape)
            .border(
                width = if (selected || item.best) 2.dp else 1.dp,
                color = when {
                    item.best -> Brand.Yellow
                    selected -> Color.White
                    else -> Color.White.copy(alpha = 0.2f)
                },
                shape = shape,
            )
            .clickable(onClick = onClick),
    ) {
        AsyncImage(item.id, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        if (item.best) {
            Icon(
                Icons.Rounded.Star, null, tint = Brand.Ink,
                modifier = Modifier.align(Alignment.TopStart).padding(4.dp).size(18.dp)
                    .background(Brand.Yellow, RoundedCornerShape(50)).padding(2.dp),
            )
        }
        if (item.eyesClosed) {
            Text(
                stringResource(R.string.review_eyes_closed),
                color = Color.White,
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(Brand.Ink.copy(alpha = 0.7f)).padding(vertical = 2.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}
