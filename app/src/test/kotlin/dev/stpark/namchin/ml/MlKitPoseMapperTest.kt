package dev.stpark.namchin.ml

import com.google.mlkit.vision.pose.PoseLandmark
import dev.stpark.namchin.core.model.Joint
import dev.stpark.namchin.core.model.Point
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MlKitPoseMapperTest {

    @Test
    fun `maps landmarks to normalized joints with aspect ratio`() {
        val pose = MlKitPoseMapper.toPersonPose(
            listOf(
                RawLandmark(PoseLandmark.NOSE, x = 400f, y = 300f, likelihood = 0.9f),
                RawLandmark(PoseLandmark.LEFT_SHOULDER, x = 500f, y = 600f, likelihood = 0.8f),
            ),
            width = 800,
            height = 1000,
        )!!

        assertEquals(0.8f, pose.aspectRatio, 1e-6f)
        assertEquals(Point(0.5f, 0.3f), pose.keypoints.getValue(Joint.NOSE).position)
        assertEquals(0.8f, pose.keypoints.getValue(Joint.LEFT_SHOULDER).confidence)
    }

    @Test
    fun `every core joint has an ML Kit landmark`() {
        assertEquals(Joint.entries.toSet(), MlKitPoseMapper.JOINT_BY_LANDMARK.values.toSet())
    }

    @Test
    fun `landmarks without a core joint are dropped`() {
        val pose = MlKitPoseMapper.toPersonPose(
            listOf(
                RawLandmark(PoseLandmark.LEFT_EAR, 1f, 1f, 0.9f),
                RawLandmark(PoseLandmark.NOSE, 1f, 1f, 0.9f),
            ),
            width = 10,
            height = 10,
        )!!

        assertEquals(setOf(Joint.NOSE), pose.keypoints.keys)
    }

    @Test
    fun `no landmarks means no person`() {
        assertNull(MlKitPoseMapper.toPersonPose(emptyList(), 10, 10))
    }
}
