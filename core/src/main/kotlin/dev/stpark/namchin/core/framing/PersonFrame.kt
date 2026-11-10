package dev.stpark.namchin.core.framing

import dev.stpark.namchin.core.model.PersonPose

/** 인물이 화면에서 차지하는 영역 (정규화 좌표). */
data class PersonFrame(val top: Float, val bottom: Float, val centerX: Float) {
    val height: Float get() = bottom - top
    val centerY: Float get() = (top + bottom) / 2

    companion object {
        const val MIN_CONFIDENCE = 0.5f
        private const val MIN_KEYPOINTS = 3

        fun from(pose: PersonPose): PersonFrame? {
            val points = pose.keypoints.values
                .filter { it.confidence >= MIN_CONFIDENCE }
                .map { it.position }
            if (points.size < MIN_KEYPOINTS) return null
            val top = points.minOf { it.y }
            val bottom = points.maxOf { it.y }
            if (bottom <= top) return null
            val centerX = (points.minOf { it.x } + points.maxOf { it.x }) / 2
            return PersonFrame(top, bottom, centerX)
        }
    }
}
