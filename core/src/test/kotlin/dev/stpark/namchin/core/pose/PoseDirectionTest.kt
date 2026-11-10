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

    private fun direction(
        reference: Map<Joint, Point>,
        current: Map<Joint, Point>,
        part: BodyPart,
        mirrored: Boolean = false,
        previous: Direction? = null,
    ) = PoseDirection.of(pose(reference), pose(current), part, mirrored, previous)

    /** 오른팔이 거의 옆(80°)으로 뻗은 레퍼런스: 지금 내린 팔과의 차이가 대각선 근처. */
    private val rightArmNearlyOut = STANDING.with(Joint.RIGHT_ELBOW to Point(0.3515f, 0.3174f))

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

    @Test
    fun `right arm outward is toward screen left`() {
        assertEquals(Direction.OUTWARD, direction(rightArmNearlyOut, STANDING, BodyPart.RIGHT_UPPER_ARM))
    }

    @Test
    fun `near the diagonal the previous axis is kept`() {
        assertEquals(
            Direction.RAISE,
            direction(rightArmNearlyOut, STANDING, BodyPart.RIGHT_UPPER_ARM, previous = Direction.RAISE),
        )
    }

    @Test
    fun `small bend difference keeps saying bend once started`() {
        // 무릎을 20°만 더 굽혀야: 처음엔 방향으로, 이미 '굽혀'였으면 계속 '굽혀'
        val slightlyBent = STANDING.with(Joint.LEFT_ANKLE to Point(0.598f, 0.888f))

        assertEquals(Direction.OUTWARD, direction(slightlyBent, STANDING, BodyPart.LEFT_SHIN))
        assertEquals(Direction.BEND, direction(slightlyBent, STANDING, BodyPart.LEFT_SHIN, previous = Direction.BEND))
    }

    @Test
    fun `mirrored match compares bending with the flipped reference`() {
        val rightSeated = STANDING.with(
            Joint.RIGHT_KNEE to Point(0.32f, 0.56f),
            Joint.RIGHT_ANKLE to Point(0.32f, 0.72f),
        )

        assertEquals(Direction.BEND, direction(rightSeated, STANDING, BodyPart.LEFT_SHIN, mirrored = true))
    }

    @Test
    fun `barely leaning torso gives no lean direction unless already leaning`() {
        val barely = STANDING.with(Joint.LEFT_SHOULDER to Point(0.552f, 0.30f))

        assertNull(direction(barely, STANDING, BodyPart.LEFT_TORSO))
        assertEquals(Direction.LEAN_LEFT, direction(barely, STANDING, BodyPart.LEFT_TORSO, previous = Direction.LEAN_LEFT))
    }

    @Test
    fun `unknown when a segment has no length`() {
        val collapsed = STANDING.with(Joint.RIGHT_ELBOW to Point(0.45f, 0.30f))

        assertNull(direction(rightArmOut, collapsed, BodyPart.RIGHT_UPPER_ARM))
    }
}
