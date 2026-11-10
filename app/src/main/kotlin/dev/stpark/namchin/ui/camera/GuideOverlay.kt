package dev.stpark.namchin.ui.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.stpark.namchin.core.live.Region
import dev.stpark.namchin.core.model.PersonPose
import dev.stpark.namchin.data.Reference
import dev.stpark.namchin.ui.common.SkeletonOverlay
import java.io.File

private val DIM = Color(0xAA000000)
private val TARGET = Color(0xCCFFFFFF)
private val CURRENT = Color(0xFFFFD600)
private val ALIGNED = Color(0xFF00E676)

/**
 * 4:3 프리뷰 위에 그린다: 찍히지 않는 영역 어둡게, 반투명 레퍼런스, 목표 골격(흰색), 현재 골격.
 */
@Composable
fun GuideOverlay(
    reference: Reference,
    region: Region,
    current: PersonPose?,
    aligned: Boolean,
    ghostAlpha: Float,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val l = region.left * size.width
            val t = region.top * size.height
            val r = region.right * size.width
            val b = region.bottom * size.height
            drawRect(DIM, Offset.Zero, Size(size.width, t))
            drawRect(DIM, Offset(0f, b), Size(size.width, size.height - b))
            drawRect(DIM, Offset(0f, t), Size(l, b - t))
            drawRect(DIM, Offset(r, t), Size(size.width - r, b - t))
        }
        Box(
            Modifier
                .offset(maxWidth * region.left, maxHeight * region.top)
                .size(maxWidth * region.width, maxHeight * region.height),
        ) {
            AsyncImage(
                model = File(reference.imagePath),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize().alpha(ghostAlpha),
            )
            SkeletonOverlay(reference.pose, Modifier.fillMaxSize(), stroke = 3.dp, color = TARGET)
            SkeletonOverlay(current, Modifier.fillMaxSize(), stroke = 3.dp, color = if (aligned) ALIGNED else CURRENT)
        }
    }
}

/** 화면 중앙 수평선. 폰이 기운 만큼 반대로 돌려 실제 수평을 보여준다. */
@Composable
fun LevelIndicator(rollDegrees: Float, level: Boolean, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(160.dp, 4.dp).rotate(-rollDegrees)) {
            drawRect(if (level) ALIGNED else CURRENT)
        }
    }
}
