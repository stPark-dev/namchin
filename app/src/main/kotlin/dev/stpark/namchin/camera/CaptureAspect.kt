package dev.stpark.namchin.camera

/** 저장될 사진 비율 (가로:세로). 화면의 밝은 영역과 같다. */
data class CaptureAspect(val width: Int, val height: Int) {
    val ratio: Float get() = width.toFloat() / height

    companion object {
        /** 레퍼런스가 있으면 그 비율, 없으면 기본 3:4 또는 인스타 4:5. */
        fun of(referenceWidth: Int?, referenceHeight: Int?, instaRatio: Boolean): CaptureAspect = when {
            referenceWidth != null && referenceHeight != null -> CaptureAspect(referenceWidth, referenceHeight)
            instaRatio -> CaptureAspect(4, 5)
            else -> CaptureAspect(3, 4)
        }
    }
}
