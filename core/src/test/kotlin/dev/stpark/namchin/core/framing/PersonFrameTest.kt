package dev.stpark.namchin.core.framing

import dev.stpark.namchin.core.model.Joint
import dev.stpark.namchin.core.model.Point
import dev.stpark.namchin.core.testutil.STANDING
import dev.stpark.namchin.core.testutil.pose
import dev.stpark.namchin.core.testutil.with
import dev.stpark.namchin.core.testutil.withConfidence
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PersonFrameTest {

    @Test
    fun `frame is anchored on the shoulders and scaled by torso length`() {
        val frame = PersonFrame.from(pose(STANDING))!!

        assertEquals(0.50f, frame.anchorX, 1e-4f)
        assertEquals(0.30f, frame.anchorY, 1e-4f)
        assertEquals(0.25f, frame.torso, 1e-4f)
    }

    @Test
    fun `limbs do not affect the frame`() {
        val armsUp = STANDING.with(
            Joint.LEFT_WRIST to Point(0.60f, 0.05f),
            Joint.RIGHT_WRIST to Point(0.40f, 0.05f),
            Joint.LEFT_ANKLE to Point(0.90f, 0.99f),
        )

        assertEquals(PersonFrame.from(pose(STANDING)), PersonFrame.from(pose(armsUp)))
    }

    @Test
    fun `torso length is measured in image-height units`() {
        val leaning = STANDING.with(Joint.LEFT_HIP to Point(0.55f, 0.55f), Joint.RIGHT_HIP to Point(0.45f, 0.55f))
            .mapValues { (j, p) -> if (j.name.contains("HIP")) Point(p.x + 0.2f, p.y) else p }

        val square = PersonFrame.from(pose(leaning, aspectRatio = 1f))!!
        val wide = PersonFrame.from(pose(leaning, aspectRatio = 2f))!!

        assertEquals(Math.hypot(0.2, 0.25).toFloat(), square.torso, 1e-4f)
        assertEquals(Math.hypot(0.4, 0.25).toFloat(), wide.torso, 1e-4f)
    }

    @Test
    fun `returns null when a shoulder or hip is unreliable`() {
        assertNull(PersonFrame.from(pose(STANDING).withConfidence(Joint.LEFT_HIP, 0.1f)))
        assertNull(PersonFrame.from(pose(STANDING).withConfidence(Joint.RIGHT_SHOULDER, 0.1f)))
    }
}
