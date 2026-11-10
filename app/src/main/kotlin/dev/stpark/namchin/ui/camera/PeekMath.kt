package dev.stpark.namchin.ui.camera

/** 손패 카드 "꾹 눌러 끌어올려 크게 보기" 계산. */
object PeekMath {
    /** 이만큼(진행도) 끌어올렸다 놓으면 열린다. 열린 상태에선 이만큼 끌어내리면 닫힌다. */
    const val OPEN_THRESHOLD = 0.35f
    const val WIDTH_RATIO = 0.8f
    const val MAX_HEIGHT_RATIO = 0.62f

    /** 크게 열린 카드의 세로 중심 (화면 높이 비율). 아래에 적용 버튼 자리를 남긴다. */
    const val CENTER_Y_RATIO = 0.42f

    /** 손패 카드를 탭하면 끌지 않아도 끝까지 연다. */
    const val TAP_TARGET = 1f

    /** 손을 뗐을 때 갈 곳. 실제로 끌던 중이 아니면(취소된 옛 제스처 등) null = 아무것도 안 함. */
    fun releaseTarget(progress: Float, open: Boolean, dragging: Boolean): Float? =
        if (dragging) settle(progress, open) else null

    data class Bounds(val left: Float, val top: Float, val width: Float, val height: Float)

    /** 위로 끌어올린 거리 → 0(손패)..1(크게). */
    fun progress(dragUpPx: Float, distancePx: Float): Float = (dragUpPx / distancePx).coerceIn(0f, 1f)

    /** 손을 뗐을 때 갈 곳: 1 = 크게 열림, 0 = 손패로. */
    fun settle(progress: Float, wasOpen: Boolean): Float = if (wasOpen) {
        if (progress <= 1f - OPEN_THRESHOLD) 0f else 1f
    } else {
        if (progress >= OPEN_THRESHOLD) 1f else 0f
    }

    /** 크게 열렸을 때 카드 자리: 레퍼런스 비율 유지, 화면 가로 80% (너무 길면 높이 제한). */
    fun target(screenWidth: Float, screenHeight: Float, aspect: Float): Bounds {
        var width = screenWidth * WIDTH_RATIO
        var height = width / aspect
        val maxHeight = screenHeight * MAX_HEIGHT_RATIO
        if (height > maxHeight) {
            height = maxHeight
            width = height * aspect
        }
        return Bounds((screenWidth - width) / 2, screenHeight * CENTER_Y_RATIO - height / 2, width, height)
    }

    fun lerp(from: Bounds, to: Bounds, t: Float): Bounds = Bounds(
        from.left + (to.left - from.left) * t,
        from.top + (to.top - from.top) * t,
        from.width + (to.width - from.width) * t,
        from.height + (to.height - from.height) * t,
    )
}
