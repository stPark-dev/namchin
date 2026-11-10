package dev.stpark.namchin.core.pose

import dev.stpark.namchin.core.model.BodyPart
import dev.stpark.namchin.core.model.Joint
import dev.stpark.namchin.core.model.Point
import dev.stpark.namchin.core.testutil.STANDING
import dev.stpark.namchin.core.testutil.mirrored
import dev.stpark.namchin.core.testutil.pose
import dev.stpark.namchin.core.testutil.transformed
import dev.stpark.namchin.core.testutil.with
import dev.stpark.namchin.core.testutil.withConfidence
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PoseSimilarityTest {

    /** 오른팔을 옆으로 수평하게 든 자세 (인물 오른쪽 = 화면 왼쪽). */
    private val rightArmRaised = STANDING.with(
        Joint.RIGHT_ELBOW to Point(0.35f, 0.30f),
        Joint.RIGHT_WRIST to Point(0.25f, 0.30f),
    )

    @Test
    fun `identical poses score 1`() {
        val match = PoseSimilarity.compare(pose(STANDING), pose(STANDING))

        assertNotNull(match)
        assertEquals(1f, match!!.score, 1e-4f)
        assertFalse(match.mirrored)
    }

    @Test
    fun `score is invariant to distance and position`() {
        val far = pose(STANDING.transformed(scale = 0.5f, dx = 0.1f, dy = 0.2f))

        val match = PoseSimilarity.compare(pose(STANDING), far)

        assertEquals(1f, match!!.score, 1e-4f)
    }

    @Test
    fun `different arm lowers score and is reported as worst part`() {
        val match = PoseSimilarity.compare(pose(rightArmRaised), pose(STANDING))

        assertTrue(match!!.score < 0.9f)
        assertTrue(match.worstPart in setOf(BodyPart.RIGHT_UPPER_ARM, BodyPart.RIGHT_FOREARM))
    }

    @Test
    fun `mirrored pose is accepted and flagged`() {
        val reference = pose(rightArmRaised)
        val current = pose(rightArmRaised.mirrored()) // 왼팔을 든 거울 자세

        val match = PoseSimilarity.compare(reference, current)

        assertEquals(1f, match!!.score, 1e-4f)
        assertTrue(match.mirrored)
    }

    @Test
    fun `worst part is named from the current person's body when mirrored`() {
        // 레퍼런스: 오른팔 수평. 현재: 왼팔을 들었지만 팔꿈치가 굽음.
        val bentLeftArm = rightArmRaised.mirrored().with(Joint.LEFT_WRIST to Point(0.65f, 0.20f))

        val match = PoseSimilarity.compare(pose(rightArmRaised), pose(bentLeftArm))

        assertTrue(match!!.mirrored)
        assertEquals(BodyPart.LEFT_FOREARM, match.worstPart)
    }

    @Test
    fun `opposite direction of a limb scores 0 for that part`() {
        val armUp = STANDING.with(
            Joint.RIGHT_ELBOW to Point(0.44f, 0.20f),
            Joint.RIGHT_WRIST to Point(0.43f, 0.10f),
        )

        val match = PoseSimilarity.compare(pose(armUp), pose(STANDING))

        assertEquals(0f, match!!.partScores.getValue(BodyPart.RIGHT_UPPER_ARM), 0.05f)
    }

    @Test
    fun `low confidence keypoints are ignored`() {
        val unreliableArm = pose(rightArmRaised).withConfidence(Joint.RIGHT_WRIST, 0.1f)

        val match = PoseSimilarity.compare(unreliableArm, pose(STANDING))

        assertFalse(BodyPart.RIGHT_FOREARM in match!!.partScores)
    }

    @Test
    fun `returns null when too few parts are comparable`() {
        val legsOnly = pose(
            STANDING.filterKeys { it.name.contains("HIP") || it.name.contains("KNEE") },
        )

        assertNull(PoseSimilarity.compare(legsOnly, pose(STANDING)))
    }
}
