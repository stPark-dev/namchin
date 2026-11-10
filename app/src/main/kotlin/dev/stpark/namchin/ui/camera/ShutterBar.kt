package dev.stpark.namchin.ui.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.stpark.namchin.R
import dev.stpark.namchin.data.Reference

/** 기본 카메라 앱처럼: 왼쪽 갤러리(레퍼런스 관리), 가운데 셔터. */
@Composable
fun ShutterBar(
    latestReference: Reference?,
    capturing: Boolean,
    onShutter: () -> Unit,
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
                .border(2.dp, Color.White, RoundedCornerShape(12.dp))
                .clickable(onClick = onOpenLibrary),
            contentAlignment = Alignment.Center,
        ) {
            if (latestReference != null) {
                ReferenceCard(latestReference, Modifier.size(56.dp))
            } else {
                Text(stringResource(R.string.camera_gallery), color = Color.White, fontSize = 11.sp)
            }
        }
        val shutterLabel = stringResource(R.string.camera_shutter)
        Box(
            Modifier
                .align(Alignment.Center)
                .size(76.dp)
                .border(4.dp, Color.White, CircleShape)
                .padding(8.dp)
                .clip(CircleShape)
                .background(if (capturing) Color.Gray else Color.White)
                .clickable(enabled = !capturing, onClick = onShutter)
                .semantics { contentDescription = shutterLabel },
        )
    }
}
