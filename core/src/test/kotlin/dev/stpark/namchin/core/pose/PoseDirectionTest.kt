package dev.stpark.namchin.core.pose

import dev.stpark.namchin.core.model.BodyPart
import dev.stpark.namchin.core.model.Joint
import dev.stpark.namchin.core.model.Point
import dev.stpark.namchin.core.pose.PoseDirection.Direction
import dev.stpark.namchin.core.testutil.STANDING
import dev.stpark.namchin.core.testutil.mirrored
import dev.stpark.namchin.core.testutil.pose
import dev.stpark.namchin.core.testutil.with
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PoseDirectionTest {

    /** 오른팔을 옆으로 수평하게 든 자세 (인물 오른쪽 = 화면 왼쪽). */
    private val rightArmOut = STANDING.with(
        Joint.RIGHT_ELBOW to Point(0.35f, 0.30f),
        Joint.RIGHT_WRIST to Point(0.25f, 0.30f),
    )

    private fun direction(reference: Map<Joint, Point>, current: Map<Joint, Point>, part: BodyPart, mirrored: Boolean = false) =
        PoseDirection.of(pose(reference), pose(current), part, mirrored)

    @Test
    fun `arm that should be higher is raised`() {
        assertEquals(Direction.RAISE, direction(rightArmOut, STANDING, BodyPart.RIGHT_UPPER_ARM))
    }

    @Test
    fun `arm that should be lower is lowered`() {
        assertEquals(Direction.LOWER, direction(STANDING, rightArmOut, BodyPart.RIGHT_UPPER_ARM))
    }

    @Test
    fun `arm that should be further from the body is spread outward`() {
        // 왼팔(화면 오른쪽)을 비스듬히 바깥 아래로
        val leftArmDiagonal = STANDING.with(
            Joint.LEFT_ELBOW to Point(0.62f, 0.37f),
            Joint.LEFT_WRIST to Point(0.69f, 0.44f),
        )

        assertEquals(Direction.OUTWARD, direction(leftArmDiagonal, STANDING, BodyPart.LEFT_UPPER_ARM))
        assertEquals(Direction.INWARD, direction(STANDING, leftArmDiagonal, BodyPart.LEFT_UPPER_ARM))
    }

    @Test
    fun `knee that should be bent is bent, and straightened the other way round`() {
        val seated = STANDING.with(
            Joint.LEFT_KNEE to Point(0.68f, 0.56f),
            Joint.LEFT_ANKLE to Point(0.68f, 0.72f),
        )

        assertEquals(Direction.BEND, direction(seated, STANDING, BodyPart.LEFT_SHIN))
        assertEquals(Direction.STRAIGHTEN, direction(STANDING, seated, BodyPart.LEFT_SHIN))
    }

    @Test
    fun `torso leaning is described in screen directions`() {
        val leanScreenRight = STANDING.with(
            Joint.LEFT_SHOULDER to Point(0.60f, 0.30f),
            Joint.RIGHT_SHOULDER to Point(0.50f, 0.30f),
        )

        assertEquals(Direction.LEAN_RIGHT, direction(leanScreenRight, STANDING, BodyPart.LEFT_TORSO))
        assertEquals(Direction.LEAN_LEFT, direction(STANDING, leanScreenRight, BodyPart.LEFT_TORSO))
    }

    @Test
    fun `mirrored match compares with the flipped reference`() {
        // 레퍼런스: 오른팔 수평. 현재: 거울 자세지만 왼팔이 내려와 있음 → 왼팔을 올려야
        val current = rightArmOut.mirrored().with(
            Joint.LEFT_ELBOW to Point(0.56f, 0.40f),
            Joint.LEFT_WRIST to Point(0.57f, 0.50f),
        )

        assertEquals(Direction.RAISE, direction(rightArmOut, current, BodyPart.LEFT_UPPER_ARM, mirrored = true))
    }

    @Test
    fun `unknown when a joint is not visible`() {
        val noElbow = STANDING.filterKeys { it != Joint.RIGHT_ELBOW }

        assertNull(direction(rightArmOut, noElbow, BodyPart.RIGHT_UPPER_ARM))
    }
}
