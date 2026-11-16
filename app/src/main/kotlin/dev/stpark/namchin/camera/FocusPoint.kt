package dev.stpark.namchin.camera

/** 미리보기 위 초점 위치 (미리보기 뷰 기준 0..1). */
data class FocusPoint(val x: Float, val y: Float) {
    companion object {
        /**
         * 화면을 탭한 위치 → 미리보기 기준 위치.
         * 거꾸로 들면 화면 전체를 180° 돌려 그리고 미리보기만 되돌리므로, 미리보기 기준으로는 반대 자리다.
         */
        fun fromTap(x: Float, y: Float, width: Float, height: Float, upsideDown: Boolean): FocusPoint {
            val nx = (x / width).coerceIn(0f, 1f)
            val ny = (y / height).coerceIn(0f, 1f)
            return if (upsideDown) FocusPoint(1f - nx, 1f - ny) else FocusPoint(nx, ny)
        }
    }
}
