package dev.stpark.namchin.ui.camera

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.stpark.namchin.R
import dev.stpark.namchin.data.Reference
import dev.stpark.namchin.ui.theme.Brand
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke

/** 기본 카메라 앱처럼: 왼쪽 갤러리(레퍼런스 관리), 가운데 셔터. */
@Composable
fun ShutterBar(
    latestReference: Reference?,
    enabled: Boolean,
    onShutter: () -> Unit,
    onLongShutter: () -> Unit,
    onOpenLibrary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = 32.dp)
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.5.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                .clickable(onClick = onOpenLibrary),
            contentAlignment = Alignment.Center,
        ) {
            if (latestReference != null) {
                ReferenceCard(latestReference, Modifier.size(56.dp))
            } else {
                Icon(Icons.Rounded.PhotoLibrary, stringResource(R.string.camera_gallery), tint = Color.White)
            }
        }
        val shutterLabel = stringResource(R.string.camera_shutter)
        LensShutter(
            enabled = enabled,
            modifier = Modifier
                .align(Alignment.Center)
                .size(80.dp)
                .clip(CircleShape)
                // 탭 = 한 장, 길게 누르기 = 연속 촬영
                .combinedClickable(enabled = enabled, onClick = onShutter, onLongClick = onLongShutter)
                .semantics { contentDescription = shutterLabel },
        )
    }
}

/** 앱 아이콘의 렌즈: 노란 링 + 노란 몸체 + 가운데 흰 하트. 비활성이면 회색. */
@Composable
private fun LensShutter(enabled: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val body = if (enabled) Brand.Yellow else Color.Gray
        val ring = 3.5.dp.toPx()
        val radius = size.minDimension / 2
        drawCircle(Color.White, radius - ring / 2, style = Stroke(ring))
        drawCircle(body, radius - ring * 2.2f)
        // 하트: 두 원 + 아래로 모이는 삼각형을 경로 하나로
        val h = radius * 0.5f
        val cx = center.x
        val cy = center.y + h * 0.08f
        val heart = Path().apply {
            moveTo(cx, cy + h * 0.38f)
            cubicTo(cx - h * 0.62f, cy - h * 0.02f, cx - h * 0.36f, cy - h * 0.52f, cx, cy - h * 0.2f)
            cubicTo(cx + h * 0.36f, cy - h * 0.52f, cx + h * 0.62f, cy - h * 0.02f, cx, cy + h * 0.38f)
            close()
        }
        drawPath(heart, Brand.Paper)
    }
}
