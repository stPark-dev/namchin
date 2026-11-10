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
private val GRID = Color(0x99FFFFFF)

/** region 위치에 내용을 배치한다 (4:3 프리뷰 박스 기준). */
@Composable
private fun InRegion(region: Region, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    BoxWithConstraints(modifier) {
        Box(
            Modifier
                .offset(maxWidth * region.left, maxHeight * region.top)
                .size(maxWidth * region.width, maxHeight * region.height),
        ) { content() }
    }
}

/**
 * 프레임마다 바뀌지 않는 부분: 찍히지 않는 영역 어둡게, 반투명 레퍼런스, 목표 골격(흰색).
 * 인자가 그대로면 재구성을 건너뛴다.
 */
@Composable
fun StaticGuide(reference: Reference, region: Region, ghostAlpha: Float, modifier: Modifier = Modifier) {
    Box(modifier) {
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
        InRegion(region, Modifier.fillMaxSize()) {
            AsyncImage(
                model = File(reference.imagePath),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize().alpha(ghostAlpha),
            )
            SkeletonOverlay(reference.pose, Modifier.fillMaxSize(), stroke = 3.dp, color = TARGET)
        }
    }
}

/** 프레임마다 바뀌는 현재 골격. 맞으면 초록, 아니면 노랑. */
@Composable
fun LiveSkeleton(current: PersonPose?, region: Region, aligned: Boolean, modifier: Modifier = Modifier) {
    InRegion(region, modifier) {
        SkeletonOverlay(current, Modifier.fillMaxSize(), stroke = 3.dp, color = if (aligned) ALIGNED else CURRENT)
    }
}

/** 찍힐 영역의 삼분할 격자. */
@Composable
fun GridOverlay(region: Region, modifier: Modifier = Modifier) {
    InRegion(region, modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 1.dp.toPx()
            for (i in 1..2) {
                val x = size.width * i / 3
                val y = size.height * i / 3
                drawLine(GRID, Offset(x, 0f), Offset(x, size.height), stroke)
                drawLine(GRID, Offset(0f, y), Offset(size.width, y), stroke)
            }
        }
    }
}

/** 화면 중앙 수평선. 폰이 기운 만큼 반대로 돌려 실제 수평을 보여준다. 기울기를 모르면 숨긴다. */
@Composable
fun LevelIndicator(rollDegrees: Float?, level: Boolean, modifier: Modifier = Modifier) {
    if (rollDegrees == null) return
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(160.dp, 4.dp).rotate(-rollDegrees)) {
            drawRect(if (level) ALIGNED else CURRENT)
        }
    }
}
