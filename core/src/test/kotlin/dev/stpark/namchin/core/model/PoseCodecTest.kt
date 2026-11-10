package dev.stpark.namchin.core.model

import dev.stpark.namchin.core.testutil.STANDING
import dev.stpark.namchin.core.testutil.pose
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PoseCodecTest {

    @Test
    fun `round trips a pose`() {
        val original = pose(STANDING, confidence = 0.73f, aspectRatio = 0.8f)

        assertEquals(original, PoseCodec.decode(PoseCodec.encode(original)))
    }

    @Test
    fun `round trips a pose without keypoints`() {
        val empty = PersonPose(emptyMap(), aspectRatio = 0.75f)

        assertEquals(empty, PoseCodec.decode(PoseCodec.encode(empty)))
    }

    @Test
    fun `unknown joints are skipped`() {
        val decoded = PoseCodec.decode("1.0|NOSE,0.5,0.2,0.9;LEFT_EYE,0.4,0.2,0.9")!!

        assertEquals(setOf(Joint.NOSE), decoded.keypoints.keys)
    }

    @Test
    fun `malformed input returns null`() {
        assertNull(PoseCodec.decode(""))
        assertNull(PoseCodec.decode("abc|NOSE,0.5,0.2,0.9"))
        assertNull(PoseCodec.decode("1.0|NOSE,0.5,0.2"))
        assertNull(PoseCodec.decode("1.0|NOSE,x,0.2,0.9"))
        assertNull(PoseCodec.decode("0|"))
    }
}
