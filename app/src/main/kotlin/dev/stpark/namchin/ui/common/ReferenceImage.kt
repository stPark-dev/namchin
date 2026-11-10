package dev.stpark.namchin.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.stpark.namchin.data.Reference
import java.io.File

/** 레퍼런스 이미지 + 골격 오버레이 (원본 비율 유지). */
@Composable
fun ReferenceImage(reference: Reference, modifier: Modifier = Modifier, stroke: Dp = 2.dp) {
    Box(modifier.aspectRatio(reference.aspectRatio)) {
        AsyncImage(
            model = File(reference.imagePath),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
        )
        SkeletonOverlay(reference.pose, Modifier.fillMaxSize(), stroke)
    }
}
