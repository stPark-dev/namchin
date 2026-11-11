package dev.stpark.namchin.review

import dev.stpark.namchin.core.color.Filter

/** 결과 사진에 입힐 색감: 레퍼런스 색감 따라하기, 또는 앱 필터. */
sealed interface Look {
    val needsReference: Boolean

    /** 같은 색 변환표를 다시 쓸 수 있는지 가르는 열쇠. */
    fun tableKey(shotId: String): Any

    /** @property keepLightness 밝기는 원본 그대로, 색만 레퍼런스를 따른다 */
    data class Reference(val keepLightness: Boolean) : Look {
        override val needsReference = true
        override fun tableKey(shotId: String): Any = Triple("reference", shotId, keepLightness)
    }

    /** 앱 필터는 사진과 상관없이 표가 같다 */
    data class Preset(val filter: Filter) : Look {
        override val needsReference = false
        override fun tableKey(shotId: String): Any = filter
    }
}
