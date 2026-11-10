package dev.stpark.namchin.core.framing

import dev.stpark.namchin.core.model.Joint
import dev.stpark.namchin.core.testutil.STANDING
import dev.stpark.namchin.core.testutil.pose
import dev.stpark.namchin.core.testutil.withConfidence
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PersonFrameTest {

    @Test
    fun `frame spans reliable keypoints`() {
        val frame = PersonFrame.from(pose(STANDING))!!

        assertEquals(0.20f, frame.top, 1e-4f)
        assertEquals(0.85f, frame.bottom, 1e-4f)
        assertEquals(0.50f, frame.centerX, 1e-4f)
        assertEquals(0.65f, frame.height, 1e-4f)
        assertEquals(0.525f, frame.centerY, 1e-4f)
    }

    @Test
    fun `low confidence keypoints do not extend the frame`() {
        val noisy = pose(STANDING)
            .withConfidence(Joint.LEFT_ANKLE, 0.1f)
            .withConfidence(Joint.RIGHT_ANKLE, 0.1f)

        assertEquals(0.70f, PersonFrame.from(noisy)!!.bottom, 1e-4f)
    }

    @Test
    fun `returns null without enough reliable keypoints`() {
        assertNull(PersonFrame.from(pose(STANDING, confidence = 0.1f)))
    }
}
