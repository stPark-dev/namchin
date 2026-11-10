package dev.stpark.namchin.ui.camera

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.stpark.namchin.R
import dev.stpark.namchin.data.Reference
import dev.stpark.namchin.core.tagging.Tag
import dev.stpark.namchin.core.tagging.TagVocabulary
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as rowItems
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.stpark.namchin.ui.theme.Brand
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import coil3.request.ImageRequest
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import dev.stpark.namchin.ui.theme.Jua
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import java.io.File

private val CARD_SHAPE = RoundedCornerShape(10.dp)
private const val SWIPE_UP_THRESHOLD_PX = 60f

@Composable
/**
 * @param loadSize 이미지를 이 픽셀 크기로 불러온다. 없으면 처음 그려질 때의 크기에 맞춘다
 *   (크기가 커지는 애니메이션에선 작게 불러와 늘어나 흐려지므로 최종 크기를 넘긴다)
 */
fun ReferenceCard(reference: Reference, modifier: Modifier = Modifier, loadSize: IntSize? = null) {
    val context = LocalContext.current
    val model = remember(reference.imagePath, loadSize) {
        if (loadSize == null) {
            File(reference.imagePath)
        } else {
            ImageRequest.Builder(context).data(File(reference.imagePath)).size(loadSize.width, loadSize.height).build()
        }
    }
    AsyncImage(
        model = model,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .shadow(6.dp, CARD_SHAPE)
            .clip(CARD_SHAPE)
            .border(BorderStroke(1.5.dp, Color.White.copy(alpha = 0.9f)), CARD_SHAPE)
            .background(Color.DarkGray),
    )
}

/**
 * 오른쪽 아래 모서리에 2~3장 빼꼼. 카드를 탭하거나 꾹 눌러 끌어올리면 크게 보기, 손패를 위로 쓸면 전체 펼침.
 * @param references 앞에 보일 순서대로 (첫 번째가 맨 앞)
 */
@Composable
fun CardHand(
    references: List<Reference>,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
    peekingId: Long? = null,
    onPeekOpen: (Reference, Rect, Float) -> Unit = { _, _, _ -> },
    onPeekStart: (Reference, Rect, Float) -> Unit = { _, _, _ -> },
    onPeekDrag: (Float) -> Unit = {},
    onPeekEnd: () -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current
    val bounds = remember { mutableMapOf<Long, Rect>() }
    val shown = references.take(CardFan.MAX_CARDS)
    if (shown.isEmpty()) return
    val fan = CardFan.layout(shown.size)
    Box(
        modifier
            // 화면 가장자리(내비게이션 바 위)에서 잘려 "빼꼼" 보이게
            .clipToBounds()
            .pointerInput(Unit) {
                var dragged = 0f
                detectVerticalDragGestures(
                    onDragStart = { dragged = 0f },
                    onVerticalDrag = { change, amount ->
                        change.consume()
                        dragged += amount
                        if (dragged < -SWIPE_UP_THRESHOLD_PX) {
                            dragged = 0f
                            onExpand()
                        }
                    },
                )
            },
        contentAlignment = Alignment.BottomEnd,
    ) {
        // fan은 뒤→앞 순서, shown은 앞→뒤 순서
        shown.asReversed().zip(fan).forEach { (reference, pose) ->
            ReferenceCard(
                reference,
                Modifier
                    // 카드 일부가 화면 밖으로 나가 "빼꼼" 보이도록 오른쪽·아래로 민다
                    .offset((pose.offsetXDp + 30).dp, (pose.offsetYDp + 12).dp)
                    .rotate(pose.rotationDeg)
                    .size(96.dp, 128.dp)
                    .onGloballyPositioned { bounds[reference.id] = it.boundsInRoot() }
                    // 크게 보는 중인 카드는 오버레이가 그리므로 손패에선 숨김
                    .alpha(if (peekingId == reference.id) 0f else 1f)
                    // 탭 = 크게 보기로 부드럽게 열림 (적용은 그 화면의 버튼으로). 꾹 누르면 탭으로 치지 않음
                    .pointerInput(reference.id) {
                        detectTapGestures(
                            onTap = { onPeekOpen(reference, bounds[reference.id] ?: Rect.Zero, pose.rotationDeg) },
                            onLongPress = {},
                        )
                    }
                    // 꾹 누른 채 위로 끌기 = 크게 보기
                    .pointerInput(reference.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onPeekStart(reference, bounds[reference.id] ?: Rect.Zero, pose.rotationDeg)
                            },
                            onDrag = { change, drag ->
                                change.consume()
                                onPeekDrag(drag.y)
                            },
                            onDragEnd = onPeekEnd,
                            onDragCancel = onPeekEnd,
                        )
                    },
            )
        }
    }
}

/** 손패를 펼친 전체 목록. 탭하면 적용하고 닫힌다. */
@Composable
fun CardDeck(
    references: List<Reference>,
    learnedTags: Map<Long, Set<Tag>>,
    onSelect: (Reference) -> Unit,
    onClose: () -> Unit,
    onAddReference: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brand.Ink.copy(alpha = 0.94f))
            .clickable(onClick = onClose)
            .safeDrawingPadding(),
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.deck_title),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.Close, stringResource(R.string.cd_close), tint = Color.White)
                }
            }
            if (references.isEmpty()) {
                Column(Modifier.fillMaxSize(), verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource(R.drawable.brand_symbol_on_dark), null, Modifier.size(120.dp))
                    Text(stringResource(R.string.deck_empty), color = Brand.Paper, fontFamily = Jua, fontSize = 18.sp)
                    Button(onClick = onAddReference, modifier = Modifier.padding(top = 16.dp)) {
                        Text(stringResource(R.string.deck_add))
                    }
                }
            } else {
                // 태그 필터: 레퍼런스들에 실제로 있는 태그만
                // 내 태그 + 배운 제안 (목록이 바뀔 때만 다시 계산)
                val tagsById = remember(references, learnedTags) {
                    references.associate { it.id to (it.tags + learnedTags[it.id].orEmpty()) }
                }
                val available = remember(tagsById) {
                    val present = tagsById.values.flatten().toSet()
                    TagVocabulary.CATEGORIES.flatMap { it.tags }.filter { it in present }
                }
                var filter by remember { mutableStateOf<Tag?>(null) }
                val shown = remember(references, tagsById, filter) {
                    filter?.let { f -> references.filter { f in tagsById[it.id].orEmpty() } } ?: references
                }
                LazyRow(
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    item { DeckFilterChip(stringResource(R.string.deck_filter_all), filter == null) { filter = null } }
                    rowItems(available, key = { it.name }) { tag ->
                        DeckFilterChip(tag.name, filter == tag) { filter = if (filter == tag) null else tag }
                    }
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                ) {
                    items(shown, key = { it.id }) { reference ->
                        ReferenceCard(
                            reference,
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(3f / 4f)
                                .clickable { onSelect(reference) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeckFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (selected) Brand.Ink else Color.White,
        fontSize = 14.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) Brand.Yellow else Color.White.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

/** 적용 중인 레퍼런스. ✕로 기본 모드로 돌아간다. */
@Composable
fun AppliedReferenceChip(reference: Reference, onClear: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.size(64.dp, 80.dp)) {
        ReferenceCard(reference, Modifier.fillMaxSize().padding(top = 8.dp, end = 8.dp))
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
                .clip(CircleShape)
                .background(Brand.Ink)
                .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                .clickable(onClick = onClear),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Close, stringResource(R.string.cd_clear_reference), tint = Color.White, modifier = Modifier.size(14.dp))
        }
    }
}
