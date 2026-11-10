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
import androidx.compose.material3.TextButton
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
import dev.stpark.namchin.ui.theme.Brand
import dev.stpark.namchin.ui.theme.Jua
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import java.io.File

private val CARD_SHAPE = RoundedCornerShape(10.dp)
private const val SWIPE_UP_THRESHOLD_PX = 60f

@Composable
fun ReferenceCard(reference: Reference, modifier: Modifier = Modifier) {
    AsyncImage(
        model = File(reference.imagePath),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .shadow(6.dp, CARD_SHAPE)
            .clip(CARD_SHAPE)
            .border(BorderStroke(2.dp, Brand.Paper), CARD_SHAPE)
            .background(Color.DarkGray),
    )
}

/**
 * 오른쪽 아래 모서리에 2~3장 빼꼼. 앞 카드를 탭하면 적용, 손패를 위로 쓸면 전체 펼침.
 * @param references 앞에 보일 순서대로 (첫 번째가 맨 앞)
 */
@Composable
fun CardHand(
    references: List<Reference>,
    onSelect: (Reference) -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                    .clickable { onSelect(reference) },
            )
        }
    }
}

/** 손패를 펼친 전체 목록. 탭하면 적용하고 닫힌다. */
@Composable
fun CardDeck(
    references: List<Reference>,
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
                    color = Brand.Paper,
                    fontFamily = Jua,
                    fontSize = 22.sp,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onClose) {
                    Text(stringResource(R.string.deck_close), color = Brand.Yellow, fontFamily = Jua, fontSize = 16.sp)
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
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                ) {
                    items(references, key = { it.id }) { reference ->
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

/** 적용 중인 레퍼런스. ✕로 기본 모드로 돌아간다. */
@Composable
fun AppliedReferenceChip(reference: Reference, onClear: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.size(64.dp, 80.dp)) {
        ReferenceCard(reference, Modifier.fillMaxSize().padding(top = 8.dp, end = 8.dp))
        Text(
            "✕",
            color = Brand.Ink,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .clip(RoundedCornerShape(50))
                .background(Brand.Yellow)
                .clickable(onClick = onClear)
                .padding(horizontal = 7.dp, vertical = 1.dp),
        )
    }
}
