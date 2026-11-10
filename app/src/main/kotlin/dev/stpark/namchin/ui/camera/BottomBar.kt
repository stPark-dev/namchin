package dev.stpark.namchin.ui.camera

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.stpark.namchin.R
import dev.stpark.namchin.camera.CameraUiState
import dev.stpark.namchin.data.Reference

/**
 * 셔터 줄과 오른쪽 카드 손패, (설정 시) 분석 정보.
 * 카드 손패는 이 칸 안의 오른쪽에만 둬서 다른 버튼 터치를 가로채지 않는다.
 */
@Composable
internal fun BottomBar(
    state: CameraUiState,
    latestReference: Reference?,
    cards: List<Reference>,
    shutterEnabled: Boolean,
    debugInfo: Boolean,
    peek: CardPeekState,
    peekDistancePx: Float,
    onShutter: () -> Unit,
    onLongShutter: () -> Unit,
    onOpenLibrary: () -> Unit,
    onExpandCards: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        ShutterBar(
            latestReference = latestReference,
            enabled = shutterEnabled,
            onShutter = onShutter,
            onLongShutter = onLongShutter,
            onOpenLibrary = onOpenLibrary,
            modifier = Modifier.fillMaxSize(),
        )
        if (debugInfo) DebugStats(state, Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 4.dp))
        CardHand(
            references = cards,
            onExpand = onExpandCards,
            modifier = Modifier.align(Alignment.BottomEnd).width(150.dp).fillMaxHeight(),
            peekingId = peek.reference?.id,
            onPeekOpen = peek::openFrom,
            onPeekStart = peek::start,
            onPeekDrag = { peek.dragBy(it, peekDistancePx) },
            onPeekEnd = peek::release,
        )
    }
}

/** 설정 '분석 정보 표시': 분석 시간·fps·포즈 점수·밝기·노출. */
@Composable
private fun DebugStats(state: CameraUiState, modifier: Modifier) {
    Text(
        stringResource(
            R.string.camera_stats,
            state.analysisMs.toInt(),
            state.fps,
            state.output?.match?.let { "%.2f".format(it.score) } ?: "-",
            state.brightness,
            state.exposure ?: 0f,
        ),
        color = Color.White.copy(alpha = 0.5f),
        fontSize = 10.sp,
        modifier = modifier,
    )
}
