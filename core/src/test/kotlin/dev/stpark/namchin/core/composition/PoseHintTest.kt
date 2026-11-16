package dev.stpark.namchin.core.composition

import dev.stpark.namchin.core.model.Joint
import dev.stpark.namchin.core.model.Point
import dev.stpark.namchin.core.testutil.STANDING
import dev.stpark.namchin.core.testutil.pose
import dev.stpark.namchin.core.testutil.with
import dev.stpark.namchin.core.testutil.withConfidence
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PoseHintTest {

    @Test
    fun `both arms hanging straight at the sides is a stiff pose`() {
        assertTrue(PoseHint.stiffArms(pose(STANDING), wasStiff = false))
    }

    @Test
    fun `a hand raised toward the face makes a triangle`() {
        val handToFace = STANDING.with(Joint.LEFT_ELBOW to Point(0.62f, 0.36f), Joint.LEFT_WRIST to Point(0.54f, 0.24f))

        assertFalse(PoseHint.stiffArms(pose(handToFace), wasStiff = false))
    }

    @Test
    fun `a hand on the hip makes a triangle`() {
        val handOnHip = STANDING.with(Joint.RIGHT_ELBOW to Point(0.37f, 0.42f), Joint.RIGHT_WRIST to Point(0.46f, 0.53f))

        assertFalse(PoseHint.stiffArms(pose(handOnHip), wasStiff = false))
    }

    @Test
    fun `slightly bent arms stay stiff once judged stiff`() {
        // 팔꿈치 약 158° — 처음엔 차렷이 아니지만, 차렷이던 직후엔 유지 (깜빡임 방지)
        val slightlyBent = STANDING.with(Joint.LEFT_ELBOW to Point(0.58f, 0.40f), Joint.LEFT_WRIST to Point(0.57f, 0.50f))

        assertFalse(PoseHint.stiffArms(pose(slightlyBent), wasStiff = false))
        assertTrue(PoseHint.stiffArms(pose(slightlyBent), wasStiff = true))
    }

    @Test
    fun `unseen arms are never judged stiff`() {
        val hiddenWrist = pose(STANDING).withConfidence(Joint.LEFT_WRIST, 0.1f)

        assertFalse(PoseHint.stiffArms(hiddenWrist, wasStiff = false))
    }
}
