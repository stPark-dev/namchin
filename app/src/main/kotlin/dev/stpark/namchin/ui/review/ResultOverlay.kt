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
import androidx.compose.foundation.Image
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import dev.stpark.namchin.core.color.Filter
import dev.stpark.namchin.review.ColorPreview
import dev.stpark.namchin.review.Look
import dev.stpark.namchin.ui.common.lookLabel
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.input.pointer.pointerInput
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
fun ResultOverlay(
    state: ReviewState,
    /** 마치기: keepId 한 장만 남기거나(null이면 모두), 고른 색감(look)을 남는 사진에 입힌다 */
    onFinish: (keepId: String?, look: Look?, strength: Float) -> Unit,
    /** 아무것도 바꾸지 않고 닫기 */
    onClose: () -> Unit,
    color: ColorPreview? = null,
    onPreviewColor: (String, Look, Float) -> Unit = { _, _, _ -> },
    onClearColor: () -> Unit = {},
    /** 처음 골라 둘 색감 (카메라에서 고른 필터) */
    initialLook: Look? = null,
) {
    if (state == ReviewState.Hidden) return
    Box(
        Modifier
            .fillMaxSize()
            .background(Brand.Ink)
            // 터치를 받는 것만으로 아래 카메라(셔터·줌)로 새지 않는다.
            // 소비(consume)까지 하면 손가락이 살짝만 움직여도 버튼이 누르기를 취소한다
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent()
                }
            }
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
            is ReviewState.Ready -> ReadyContent(state, onFinish, onClose, color, onPreviewColor, onClearColor, initialLook)
            ReviewState.Hidden -> Unit
        }
    }
}

@Composable
private fun ReadyContent(
    state: ReviewState.Ready,
    onFinish: (keepId: String?, look: Look?, strength: Float) -> Unit,
    onClose: () -> Unit,
    color: ColorPreview?,
    onPreviewColor: (String, Look, Float) -> Unit,
    onClearColor: () -> Unit,
    initialLook: Look?,
) {
    var selectedId by remember(state) { mutableStateOf((state.items.firstOrNull { it.best } ?: state.items.firstOrNull())?.id) }
    var compare by remember(state) { mutableStateOf(state.reference != null) }
    val selected = state.items.firstOrNull { it.id == selectedId } ?: state.items.firstOrNull() ?: return
    val reference = state.reference
    var look by remember(state) { mutableStateOf(initialLook?.takeIf { !it.needsReference || reference != null }) }
    // 카메라에서 고른 필터는 촬영 때 본 그대로(100%)부터
    var strength by remember(state) { mutableFloatStateOf(if (initialLook is Look.Preset) 1f else DEFAULT_STRENGTH) }
    // 다른 필터를 갔다 와도 레퍼런스 색감의 밝기 유지 선택을 기억한다
    var keepLightness by remember(state) { mutableStateOf(false) }
    // 색감·강도·선택 사진이 바뀌면 미리보기 다시 (뷰모델이 마지막 요청만 처리)
    LaunchedEffect(look, strength, selected.id) {
        look?.let { onPreviewColor(selected.id, it, strength) } ?: onClearColor()
    }
    // 같은 사진의 색감 미리보기를 보여준다. 다른 필터로 바꾸는 중엔 새 미리보기가 올 때까지 앞의 것을 둔다 (원본이 번쩍이지 않게)
    val shown: Any = color
        ?.takeIf { look != null && it.shotId == selected.id }
        ?.bitmap?.asImageBitmap() ?: selected.id

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.review_title),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, stringResource(R.string.cd_close), tint = Color.White) }
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
                        ShotImage(shown, Modifier.fillMaxWidth().aspectRatio(reference.aspectRatio).clip(SHAPE), ContentScale.Crop)
                    }
                }
            } else {
                ShotImage(shown, Modifier.fillMaxSize().clip(SHAPE), ContentScale.Fit)
            }
        }
        LookControls(
            look = look,
            hasReference = reference != null,
            keepLightness = keepLightness,
            strength = strength,
            onLook = {
                if (it is Look.Reference) keepLightness = it.keepLightness
                look = it
            },
            onStrength = { strength = it },
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.items, key = { it.id }) { item ->
                Thumb(item, selected = item.id == selected.id) { selectedId = item.id }
            }
        }
        if (state.items.size == 1) {
            // 한 장이면 고를 게 없으니 (고른 필터만 입히고) 마치기
            Button(
                onClick = { onFinish(null, look, strength) },
                colors = ButtonDefaults.buttonColors(containerColor = Brand.Yellow, contentColor = Brand.Ink),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text(stringResource(R.string.review_done), fontWeight = FontWeight.SemiBold)
            }
        } else Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { onFinish(null, look, strength) }, modifier = Modifier.weight(1f).height(52.dp)) {
                Text(stringResource(R.string.review_keep_all), color = Color.White)
            }
            // 보고 있는 사진을 남긴다. 베스트가 아닌 걸 골랐으면 문구로 분명히
            Button(
                onClick = { onFinish(selected.id, look, strength) },
                colors = ButtonDefaults.buttonColors(containerColor = Brand.Yellow, contentColor = Brand.Ink),
                modifier = Modifier.weight(1f).height(52.dp),
            ) {
                Text(
                    stringResource(if (selected.best) R.string.review_keep_best else R.string.review_keep_selected),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun ShotImage(model: Any, modifier: Modifier, scale: ContentScale) {
    if (model is ImageBitmap) {
        Image(model, null, modifier, contentScale = scale)
    } else {
        AsyncImage(model, null, modifier, contentScale = scale)
    }
}

/**
 * 색감 고르기: 원본 · 레퍼런스 색감(레퍼런스가 있을 때) · 앱 필터.
 * 고르면 강도 슬라이더. 고른 색감은 마칠 때 남는 사진에 입혀진다. 레퍼런스 색감은 밝기 유지도 고를 수 있다.
 */
@Composable
private fun LookControls(
    look: Look?,
    hasReference: Boolean,
    keepLightness: Boolean,
    strength: Float,
    onLook: (Look?) -> Unit,
    onStrength: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Palette, null, tint = Brand.Yellow, modifier = Modifier.size(20.dp))
            Text(
                stringResource(R.string.look_title),
                color = Color.White,
                modifier = Modifier.padding(start = 8.dp).weight(1f),
            )
        }
        val choices = buildList {
            add(null)
            if (hasReference) add(Look.Reference(keepLightness))
            Filter.entries.forEach { add(Look.Preset(it)) }
        }
        Row(
            Modifier.horizontalScroll(rememberScrollState()).selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            choices.forEach { choice ->
                val isSelected = lookKey(choice) == lookKey(look)
                // 골라진 칩이 목록 밖에 있으면 보이는 곳까지 넘긴다 (카메라에서 고른 필터가 뒤쪽일 때)
                val requester = remember { BringIntoViewRequester() }
                LaunchedEffect(isSelected) {
                    if (!isSelected) return@LaunchedEffect
                    // 처음 열릴 땐 칩이 아직 배치 전이라, 한 프레임 그린 뒤에 넘긴다
                    withFrameNanos { }
                    requester.bringIntoView()
                }
                FilterChip(
                    selected = isSelected,
                    onClick = { onLook(choice) },
                    modifier = Modifier.bringIntoViewRequester(requester),
                    label = { Text(stringResource(lookLabel(choice))) },
                    colors = FilterChipDefaults.filterChipColors(
                        labelColor = Color.White,
                        selectedContainerColor = Brand.Yellow,
                        selectedLabelColor = Brand.Ink,
                    ),
                )
            }
        }
        if (look != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Slider(
                    value = strength,
                    onValueChange = onStrength,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Brand.Yellow,
                        inactiveTrackColor = Color.White.copy(alpha = 0.2f),
                    ),
                    modifier = Modifier.weight(1f),
                )
                if (look is Look.Reference) {
                    // 어두운 레퍼런스여도 사진 밝기는 그대로, 색만 가져오기
                    FilterChip(
                        selected = look.keepLightness,
                        onClick = { onLook(look.copy(keepLightness = !look.keepLightness)) },
                        label = { Text(stringResource(R.string.color_keep_lightness)) },
                        leadingIcon = { Icon(Icons.Rounded.LightMode, null, Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            labelColor = Color.White,
                            iconColor = Color.White,
                            selectedContainerColor = Brand.Yellow,
                            selectedLabelColor = Brand.Ink,
                            selectedLeadingIconColor = Brand.Ink,
                        ),
                    )
                }
            }
        }
    }
}

/** 밝기 유지 옵션이 달라도 같은 "레퍼런스 색감" 칩 */
private fun lookKey(look: Look?): String = when (look) {
    null -> "original"
    is Look.Reference -> "reference"
    is Look.Preset -> look.filter.name
}

private const val DEFAULT_STRENGTH = 0.8f

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
