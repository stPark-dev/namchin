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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import dev.stpark.namchin.ui.theme.Brand
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.stpark.namchin.core.live.Region
import dev.stpark.namchin.core.model.PersonPose
import dev.stpark.namchin.data.Reference
import dev.stpark.namchin.ui.common.SkeletonOverlay
import java.io.File

private val DIM = Brand.Ink.copy(alpha = 0.72f)
private val TARGET = Color.White.copy(alpha = 0.8f)
private val CURRENT = Brand.Yellow
private val ALIGNED = Brand.Success
private val GRID = Color.White.copy(alpha = 0.6f)

/** region 위치에 내용을 배치한다 (4:3 프리뷰 박스 기준). */
@Composable
internal fun InRegion(region: Region, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
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
        OutsideDim(region, Modifier.fillMaxSize())
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

/** 사진에 담기지 않는 바깥을 어둡게. */
@Composable
fun OutsideDim(region: Region, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val l = region.left * size.width
        val t = region.top * size.height
        val r = region.right * size.width
        val b = region.bottom * size.height
        drawRect(DIM, Offset.Zero, Size(size.width, t))
        drawRect(DIM, Offset(0f, b), Size(size.width, size.height - b))
        drawRect(DIM, Offset(0f, t), Size(l, b - t))
        drawRect(DIM, Offset(r, t), Size(size.width - r, b - t))
    }
}

/** 프레임마다 바뀌는 현재 골격. 맞으면 초록, 아니면 노랑. */
@Composable
fun LiveSkeleton(current: PersonPose?, region: Region, aligned: Boolean, modifier: Modifier = Modifier) {
    InRegion(region, modifier) {
        SkeletonOverlay(current, Modifier.fillMaxSize(), stroke = 3.dp, color = if (aligned) ALIGNED else CURRENT)
    }
}

/**
 * 앱 아이콘의 뷰파인더 꺾쇠를 찍힐 영역 네 모서리에 그린다.
 * 구도·포즈가 맞으면 노랗게 바뀌고 오른쪽 위에 반짝임 선 3개가 나타난다 (아이콘 모티프).
 */
@Composable
fun ViewfinderCorners(region: Region, aligned: Boolean, modifier: Modifier = Modifier) {
    InRegion(region, modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val color = if (aligned) Brand.Yellow else Color.White
            val stroke = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            val inset = 14.dp.toPx()
            val arm = minOf(32.dp.toPx(), size.minDimension / 5)
            val radius = 10.dp.toPx()
            fun corner(x: Float, y: Float, dx: Float, dy: Float) {
                // (x, y) 모서리에서 dx/dy 방향(±1)으로 뻗는 둥근 꺾쇠
                val path = Path().apply {
                    moveTo(x, y + dy * arm)
                    lineTo(x, y + dy * radius)
                    quadraticTo(x, y, x + dx * radius, y)
                    lineTo(x + dx * arm, y)
                }
                drawPath(path, color, style = stroke)
            }
            val l = inset
            val t = inset
            val r = size.width - inset
            val b = size.height - inset
            corner(l, t, 1f, 1f)
            corner(r, t, -1f, 1f)
            corner(l, b, 1f, -1f)
            corner(r, b, -1f, -1f)
            if (aligned) {
                // 반짝임: 오른쪽 위 꺾쇠 바깥으로 짧은 선 3개
                val spark = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                val len = 14.dp.toPx()
                listOf(-75f, -45f, -15f).forEach { deg ->
                    val rad = Math.toRadians(deg.toDouble())
                    val ox = r + 10.dp.toPx() * kotlin.math.cos(rad).toFloat()
                    val oy = t + 10.dp.toPx() * kotlin.math.sin(rad).toFloat()
                    drawLine(
                        Brand.Yellow,
                        Offset(ox, oy),
                        Offset(ox + len * kotlin.math.cos(rad).toFloat(), oy + len * kotlin.math.sin(rad).toFloat()),
                        spark.width,
                        StrokeCap.Round,
                    )
                }
            }
        }
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
