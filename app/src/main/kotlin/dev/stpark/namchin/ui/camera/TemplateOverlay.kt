package dev.stpark.namchin.ui.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.stpark.namchin.R
import dev.stpark.namchin.core.composition.CompositionTemplate
import dev.stpark.namchin.core.composition.TemplateGuide
import dev.stpark.namchin.core.guidance.Guidance
import dev.stpark.namchin.core.live.Region
import dev.stpark.namchin.ui.theme.Brand

/** 구도 틀: 인물 자리 윤곽(점선)과 보조선. 맞으면 노랑. */
@Composable
fun TemplateOverlay(
    template: CompositionTemplate,
    side: Guidance.Side?,
    region: Region,
    aligned: Boolean,
    modifier: Modifier = Modifier,
) {
    val guide = TemplateGuide.of(template, side)
    InRegion(region, modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val color = if (aligned) Brand.Yellow else Color.White.copy(alpha = 0.75f)
            val dash = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 8.dp.toPx()))
            val thin = Stroke(width = 1.5.dp.toPx(), pathEffect = dash)
            val bold = Stroke(width = 2.5.dp.toPx(), pathEffect = dash)
            guide.lines.forEach { line ->
                drawLine(
                    color, Offset(line.x1 * size.width, line.y1 * size.height),
                    Offset(line.x2 * size.width, line.y2 * size.height),
                    strokeWidth = thin.width, pathEffect = dash,
                )
            }
            // 높이 단위 크기 → 픽셀: 세로는 size.height 기준이므로 가로도 같은 값을 곱한다
            val h = size.height
            val cx = guide.centerX * size.width
            val radius = guide.headRadius * h
            drawCircle(color, radius, Offset(cx, guide.headTop * h + radius), style = bold)
            drawPath(silhouette(guide, cx, h), color, style = bold)
        }
    }
}

/** 미리보기 위 구도 틀 고르기 줄. null = 틀 없음. */
@Composable
fun TemplatePicker(
    selected: CompositionTemplate?,
    onSelect: (CompositionTemplate?) -> Unit,
    modifier: Modifier = Modifier,
) = ChoicePicker(listOf(null) + CompositionTemplate.entries, selected, ::templateLabel, onSelect, modifier)

/** 미리보기 위 가로 고르기 줄 (하나만 고름). */
@Composable
fun <T> ChoicePicker(
    choices: List<T>,
    selected: T,
    label: (T) -> Int,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(Glass)
            .horizontalScroll(rememberScrollState())
            .selectableGroup()
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        choices.forEach { choice ->
            val isSelected = choice == selected
            Box(
                Modifier
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) Brand.Ink.copy(alpha = 0.8f) else Color.Transparent)
                    .border(1.dp, if (isSelected) Brand.Yellow else Color.Transparent, RoundedCornerShape(50))
                    .selectable(selected = isSelected, role = Role.RadioButton) { onSelect(choice) }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(label(choice)), color = if (isSelected) Brand.Yellow else Color.White, fontSize = 13.sp)
            }
        }
    }
}

fun templateLabel(template: CompositionTemplate?): Int = when (template) {
    null -> R.string.template_none
    CompositionTemplate.FULL_BODY -> R.string.template_full_body
    CompositionTemplate.HALF_BODY -> R.string.template_half_body
    CompositionTemplate.SIDE_THIRD -> R.string.template_side_third
    CompositionTemplate.HORIZON_T -> R.string.template_horizon
}

/** 어깨 → 허리 → 두 다리로 이어지는 몸 윤곽 (머리 제외). */
private fun silhouette(guide: TemplateGuide, cx: Float, h: Float): Path {
    val shoulderY = guide.shoulderY * h
    val hipY = guide.hipY * h
    val bottom = guide.bottom * h
    val shoulder = guide.shoulderHalfWidth * h
    val hip = guide.hipHalfWidth * h
    val legGap = hip * 0.15f
    val crotch = minOf(hipY + (guide.hipY - guide.shoulderY) * h * 0.15f, bottom)
    val neck = shoulder * 0.35f
    return Path().apply {
        moveTo(cx - neck, shoulderY)
        // 둥근 어깨
        quadraticTo(cx - shoulder, shoulderY, cx - shoulder, shoulderY + shoulder * 0.6f)
        lineTo(cx - hip, hipY)
        if (bottom <= hipY) {
            // 상반신: 다리 없이 허리에서 닫는다
            lineTo(cx + hip, hipY)
            lineTo(cx + shoulder, shoulderY + shoulder * 0.6f)
            quadraticTo(cx + shoulder, shoulderY, cx + neck, shoulderY)
            return@apply
        }
        lineTo(cx - hip * 0.85f, bottom)
        lineTo(cx - legGap, bottom)
        lineTo(cx, crotch)
        lineTo(cx + legGap, bottom)
        lineTo(cx + hip * 0.85f, bottom)
        lineTo(cx + hip, hipY)
        lineTo(cx + shoulder, shoulderY + shoulder * 0.6f)
        quadraticTo(cx + shoulder, shoulderY, cx + neck, shoulderY)
    }
}
