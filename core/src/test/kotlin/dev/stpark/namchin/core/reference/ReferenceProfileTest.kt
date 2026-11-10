package dev.stpark.namchin.core.reference

import dev.stpark.namchin.core.model.Joint
import dev.stpark.namchin.core.testutil.STANDING
import dev.stpark.namchin.core.testutil.pose
import dev.stpark.namchin.core.testutil.withConfidence
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ReferenceProfileTest {

    @Test
    fun `no pose means no person`() {
        val profile = ReferenceProfile.of(null)

        assertFalse(profile.personFound)
        assertEquals(0, profile.comparableParts)
        assertFalse(profile.poseComparable)
        assertEquals(SizeBasis.NONE, profile.sizeBasis)
    }

    @Test
    fun `full body is pose comparable and sized by torso`() {
        val profile = ReferenceProfile.of(pose(STANDING))

        assertTrue(profile.personFound)
        assertEquals(10, profile.comparableParts)
        assertTrue(profile.poseComparable)
        assertEquals(SizeBasis.TORSO, profile.sizeBasis)
    }

    @Test
    fun `close-up without hips is sized by neck`() {
        val closeUp = listOf(Joint.LEFT_HIP, Joint.RIGHT_HIP, Joint.LEFT_KNEE, Joint.RIGHT_KNEE)
            .fold(pose(STANDING)) { p, j -> p.withConfidence(j, 0.1f) }

        val profile = ReferenceProfile.of(closeUp)

        assertEquals(4, profile.comparableParts)
        assertTrue(profile.poseComparable)
        assertEquals(SizeBasis.NECK, profile.sizeBasis)
    }

    @Test
    fun `person without shoulders cannot be framed`() {
        val noShoulders = pose(STANDING)
            .withConfidence(Joint.LEFT_SHOULDER, 0.1f)
            .withConfidence(Joint.RIGHT_SHOULDER, 0.1f)

        val profile = ReferenceProfile.of(noShoulders)

        assertTrue(profile.personFound)
        assertEquals(SizeBasis.NONE, profile.sizeBasis)
    }
}
