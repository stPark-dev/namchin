package dev.stpark.namchin.ui.camera

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.stpark.namchin.R
import dev.stpark.namchin.data.Reference
import dev.stpark.namchin.ui.theme.Brand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** 꾹 눌렀을 때 살짝 들리는 정도 (진행도). */
private const val LIFT = 0.06f
private val SPRING = spring<Float>(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)

/** 칩으로 날아갈 땐 튕기지 않고 착 붙게. */
private val FLY_SPRING = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)

/**
 * 손패 카드 크게 보기 상태. progress 0 = 손패 자리, 1 = 화면 가운데 크게.
 * 손가락을 따라 진행도가 바뀌고, 놓으면 스프링으로 열리거나 돌아간다.
 */
@Stable
class CardPeekState(private val scope: CoroutineScope) {
    var reference by mutableStateOf<Reference?>(null)
        private set

    /** 손패에서 출발한 카드 자리 (루트 좌표)와 기울기 */
    var from by mutableStateOf(Rect.Zero)
        private set
    var fromRotation by mutableFloatStateOf(0f)
        private set
    val progress = Animatable(0f)

    /** 적용 후 왼쪽 위 칩으로 날아가는 진행도 (0 = 가운데, 1 = 칩 자리) */
    val fly = Animatable(0f)
    var flyTarget by mutableStateOf(Rect.Zero)
        private set
    private var drag = 0f
    private var open = false
    private var dragging = false

    val active: Boolean get() = reference != null

    /** 손패 카드를 꾹 눌렀을 때: 살짝 들고 끌기를 기다린다. */
    fun start(reference: Reference, bounds: Rect, rotation: Float) {
        begin(reference, bounds, rotation)
        dragging = true
        scope.launch { progress.animateTo(LIFT, SPRING) }
    }

    /** 손패 카드를 탭했을 때: 끝까지 부드럽게 연다. */
    fun openFrom(reference: Reference, bounds: Rect, rotation: Float) {
        begin(reference, bounds, rotation)
        open = true
        scope.launch { progress.animateTo(PeekMath.TAP_TARGET, SPRING) }
    }

    private fun begin(reference: Reference, bounds: Rect, rotation: Float) {
        this.reference = reference
        from = bounds
        fromRotation = rotation
        drag = 0f
        open = false
    }

    /** 열린 카드를 다시 끌기 시작할 때. */
    fun beginDrag() {
        drag = 0f
        dragging = true
    }

    /** @param dy 손가락 세로 이동(px, 위가 음수) */
    fun dragBy(dy: Float, distancePx: Float) {
        drag -= dy
        val p = if (open) (1f + drag / distancePx).coerceIn(0f, 1f) else maxOf(LIFT, PeekMath.progress(drag, distancePx))
        scope.launch { progress.snapTo(p) }
    }

    /** 손을 뗐을 때: 충분히 끌었으면 열리고, 아니면 손패로. */
    fun release() {
        // 실제로 끌던 중일 때만 (카드가 손패에서 빠지며 취소된 옛 제스처는 무시)
        val target = PeekMath.releaseTarget(progress.value, open, dragging) ?: return
        dragging = false
        open = target == 1f
        scope.launch {
            progress.animateTo(target, SPRING)
            if (target == 0f) reference = null
        }
    }

    /**
     * 적용: 카드를 왼쪽 위 칩 자리로 날려 보낸 뒤, 도착하면 onArrived(적용).
     * @param chip 적용된 레퍼런스 칩의 카드 자리 (루트 좌표)
     */
    fun applyTo(chip: Rect, onArrived: (Reference) -> Unit) {
        val target = reference ?: return
        if (fly.isRunning || fly.value > 0f) return
        flyTarget = chip
        dragging = false
        scope.launch {
            fly.animateTo(1f, FLY_SPRING)
            onArrived(target)
            reference = null
            open = false
            progress.snapTo(0f)
            fly.snapTo(0f)
        }
    }

    fun dismiss() {
        open = false
        dragging = false
        scope.launch {
            progress.animateTo(0f, SPRING)
            reference = null
        }
    }
}

/** 크게 보기 오버레이: 배경이 어두워지고 카드가 손패에서 가운데로 커지며, 열리면 아래에 적용 버튼. */
@Composable
fun PeekOverlay(state: CardPeekState, onApply: () -> Unit, modifier: Modifier = Modifier) {
    val reference = state.reference ?: return
    val density = LocalDensity.current
    val distancePx = with(density) { PEEK_DISTANCE.toPx() }
    var origin by remember { mutableStateOf(Offset.Zero) }
    val p = state.progress.value
    val fly = state.fly.value

    BoxWithConstraints(modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInRoot() }) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        // 배경: 열릴수록 어둡게. 열려 있을 때 바깥을 누르면 닫힘
        Box(
            Modifier
                .fillMaxSize()
                .background(Brand.Ink.copy(alpha = PeekMath.dimAlpha(p, fly)))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = p > 0.5f && fly == 0f,
                    onClick = state::dismiss,
                ),
        )
        val from = PeekMath.Bounds(state.from.left - origin.x, state.from.top - origin.y, state.from.width, state.from.height)
        val target = PeekMath.target(w, h, reference.aspectRatio)
        val chip = PeekMath.Bounds(
            state.flyTarget.left - origin.x, state.flyTarget.top - origin.y,
            state.flyTarget.width, state.flyTarget.height,
        )
        // 열림(손패 → 가운데) 다음 적용(가운데 → 칩)
        val cur = PeekMath.lerp(PeekMath.lerp(from, target, p), chip, fly)
        ReferenceCard(
            reference,
            // 손패 크기에서 커지므로 처음부터 크게 열린 크기로 불러온다 (작게 불러와 늘리면 흐려짐)
            loadSize = IntSize(target.width.roundToInt(), target.height.roundToInt()),
            modifier = Modifier
                .offset { IntOffset(cur.left.roundToInt(), cur.top.roundToInt()) }
                .size(with(density) { cur.width.toDp() }, with(density) { cur.height.toDp() })
                .rotate(state.fromRotation * (1f - p))
                .pointerInput(reference.id) {
                    detectVerticalDragGestures(
                        onDragStart = { state.beginDrag() },
                        onVerticalDrag = { change, dy ->
                            change.consume()
                            state.dragBy(dy, distancePx)
                        },
                        onDragEnd = state::release,
                        onDragCancel = state::release,
                    )
                },
        )
        // 적용 버튼: 거의 다 열렸을 때 나타나고, 날아가기 시작하면 바로 사라짐
        val buttonAlpha = ((p - 0.6f) / 0.4f).coerceIn(0f, 1f) * (1f - (fly * 4f).coerceAtMost(1f))
        if (buttonAlpha > 0f) {
            Button(
                onClick = onApply,
                enabled = p > 0.9f && fly == 0f,
                colors = ButtonDefaults.buttonColors(containerColor = Brand.Yellow, contentColor = Brand.Ink),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset { IntOffset(0, (target.top + target.height).roundToInt()) }
                    .padding(top = 24.dp)
                    .alpha(buttonAlpha),
            ) {
                Icon(Icons.Rounded.Check, null)
                Text(stringResource(R.string.peek_apply), fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

/** 이만큼 끌면 완전히 열림/닫힘. */
val PEEK_DISTANCE = 280.dp
