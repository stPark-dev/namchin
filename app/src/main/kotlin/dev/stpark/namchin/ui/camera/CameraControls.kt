package dev.stpark.namchin.ui.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Grid3x3
import androidx.compose.material.icons.rounded.GridOff
import androidx.compose.material.icons.rounded.MobileOff
import androidx.compose.material.icons.rounded.MotionPhotosAuto
import androidx.compose.material.icons.rounded.MotionPhotosOff
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.stpark.namchin.R
import dev.stpark.namchin.camera.CameraUiState
import dev.stpark.namchin.camera.FeedbackSettings
import dev.stpark.namchin.camera.GuidanceText
import dev.stpark.namchin.camera.ZoomLogic
import dev.stpark.namchin.ui.theme.Brand

/** 미리보기 위에 띄우는 반투명 유리 느낌 바탕. */
private val Glass = Brand.Ink.copy(alpha = 0.55f)
private val GlassBorder = Color.White.copy(alpha = 0.14f)

/**
 * 미리보기 위쪽 안내 캡슐. 맞으면 체크 아이콘과 노란 글씨.
 * 안내가 있을 때 캡슐을 누르면 "왜?" 한 줄이 아래에 펼쳐진다 (안내가 바뀌면 접힘).
 */
@Composable
fun GuidanceCapsule(state: CameraUiState, modifier: Modifier = Modifier) {
    val output = state.output
    val display = output?.display
    val aligned = output?.aligned == true && display == null
    val noPose = state.reference != null && state.reference.pose == null
    val text = when {
        noPose -> stringResource(R.string.camera_no_reference_pose)
        display != null -> GuidanceText.text(display)
        aligned -> stringResource(R.string.camera_aligned)
        else -> return
    }
    val cue = display.takeUnless { noPose }
    // 같은 이유인 동안(예: 포즈 방향만 바뀜)은 펼친 채로 둔다
    val reason = cue?.let(GuidanceText::reason)
    var showWhy by remember(reason) { mutableStateOf(false) }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(Glass)
                .border(1.dp, GlassBorder, RoundedCornerShape(50))
                .then(
                    if (reason != null) {
                        Modifier.clickable(onClickLabel = stringResource(R.string.cd_why), role = Role.Button) { showWhy = !showWhy }
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (aligned) Icon(Icons.Rounded.CheckCircle, null, tint = Brand.Yellow, modifier = Modifier.size(18.dp))
            Text(
                text,
                color = if (aligned) Brand.Yellow else Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            if (reason != null) {
                Icon(
                    Icons.AutoMirrored.Rounded.HelpOutline,
                    stringResource(R.string.cd_why),
                    tint = if (showWhy) Brand.Yellow else Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        if (reason != null && showWhy) {
            Text(
                stringResource(reason),
                color = Color.White,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Glass)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

/** 오른쪽 세로 아이콘 줄: 음성·진동·자동 셔터·격자. 켜지면 노랑, 꺼지면 흐린 흰색. */
@Composable
fun ControlRail(
    settings: FeedbackSettings,
    grid: Boolean,
    showAutoShutter: Boolean,
    onChange: (FeedbackSettings) -> Unit,
    onGrid: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(50))
            .background(Glass)
            .border(1.dp, GlassBorder, RoundedCornerShape(50))
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        IconToggle(
            Icons.AutoMirrored.Rounded.VolumeUp, Icons.AutoMirrored.Rounded.VolumeOff,
            settings.voice, stringResource(R.string.cd_voice),
        ) { onChange(settings.copy(voice = it)) }
        IconToggle(
            Icons.Rounded.Vibration, Icons.Rounded.MobileOff,
            settings.vibrate, stringResource(R.string.cd_vibrate),
        ) { onChange(settings.copy(vibrate = it)) }
        if (showAutoShutter) {
            IconToggle(
                Icons.Rounded.MotionPhotosAuto, Icons.Rounded.MotionPhotosOff,
                settings.autoShutter, stringResource(R.string.cd_auto_shutter),
            ) { onChange(settings.copy(autoShutter = it)) }
        }
        IconToggle(Icons.Rounded.Grid3x3, Icons.Rounded.GridOff, grid, stringResource(R.string.cd_grid), onGrid)
    }
}

@Composable
private fun IconToggle(
    on: ImageVector,
    off: ImageVector,
    checked: Boolean,
    label: String,
    onToggle: (Boolean) -> Unit,
) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            // 화면 읽기(TalkBack)가 스위치와 켜짐/꺼짐 상태를 읽도록
            .toggleable(value = checked, role = Role.Switch, onValueChange = onToggle)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (checked) on else off,
            contentDescription = null,
            tint = if (checked) Brand.Yellow else Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(22.dp),
        )
    }
}

/** 줌 배율 캡슐. 선택된 배율만 노란 글씨로 크게. */
@Composable
fun ZoomCapsule(presets: List<Float>, current: Float, onSelect: (Float) -> Unit, modifier: Modifier = Modifier) {
    val selected = ZoomLogic.selected(current, presets)
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(Glass)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        presets.forEach { preset ->
            val isSelected = preset == selected
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Brand.Ink.copy(alpha = 0.8f) else Color.Transparent)
                    .clickable { onSelect(preset) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (isSelected) ZoomLogic.label(current) else ZoomLogic.label(preset).removeSuffix("×"),
                    color = if (isSelected) Brand.Yellow else Color.White,
                    fontSize = if (isSelected) 13.sp else 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

/** 레퍼런스 겹쳐 보기 투명도. 아이콘 + 얇은 슬라이더. */
@Composable
fun GhostSlider(alpha: Float, onChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Opacity, stringResource(R.string.cd_ghost), tint = Color.White.copy(alpha = 0.8f))
        Slider(
            value = alpha,
            onValueChange = onChange,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Brand.Yellow,
                inactiveTrackColor = Color.White.copy(alpha = 0.2f),
            ),
            modifier = Modifier.padding(start = 12.dp).width(220.dp),
        )
    }
}

/** 어두운 곳: 포즈 비교·자동 셔터가 꺼졌음을 알리는 배지. */
@Composable
fun NightBadge(modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(Glass)
            .border(1.dp, GlassBorder, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Rounded.NightsStay, null, tint = Brand.Yellow, modifier = Modifier.size(16.dp))
        Text(stringResource(R.string.camera_night), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

/** 짧게 뜨는 알림 캡슐. */
@Composable
fun Toast(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        color = Color.White,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Glass)
            .border(1.dp, GlassBorder, RoundedCornerShape(50))
            .padding(horizontal = 20.dp, vertical = 10.dp),
    )
}
