package dev.stpark.namchin.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import dev.stpark.namchin.core.model.BodyPart
import dev.stpark.namchin.core.model.PersonPose
import dev.stpark.namchin.core.model.Point
import dev.stpark.namchin.core.pose.PoseSimilarity

private val RELIABLE = Color(0xFF00E676)
private val UNRELIABLE = Color(0xFFFF5252)

/** 이미지와 같은 크기·비율의 영역 위에 골격을 그린다. 신뢰도 낮은 관절은 빨간 점. */
@Composable
fun SkeletonOverlay(pose: PersonPose?, modifier: Modifier = Modifier, stroke: Dp = 2.dp) {
    if (pose == null) return
    Canvas(modifier.clipToBounds()) {
        fun Point.toOffset() = Offset(x * size.width, y * size.height)
        val width = stroke.toPx()
        BodyPart.entries.forEach { part ->
            val from = pose.reliable(part.from, PoseSimilarity.MIN_CONFIDENCE) ?: return@forEach
            val to = pose.reliable(part.to, PoseSimilarity.MIN_CONFIDENCE) ?: return@forEach
            drawLine(RELIABLE, from.toOffset(), to.toOffset(), strokeWidth = width)
        }
        pose.keypoints.values.forEach {
            val color = if (it.confidence >= PoseSimilarity.MIN_CONFIDENCE) RELIABLE else UNRELIABLE
            drawCircle(color, radius = width * 1.5f, center = it.position.toOffset())
        }
    }
}
