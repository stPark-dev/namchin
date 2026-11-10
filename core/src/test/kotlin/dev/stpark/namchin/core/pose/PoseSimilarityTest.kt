package dev.stpark.namchin.core.pose

import dev.stpark.namchin.core.model.BodyPart
import dev.stpark.namchin.core.model.Joint
import dev.stpark.namchin.core.model.Point
import dev.stpark.namchin.core.testutil.STANDING
import dev.stpark.namchin.core.testutil.inAspect
import dev.stpark.namchin.core.testutil.mirrored
import dev.stpark.namchin.core.testutil.pose
import dev.stpark.namchin.core.testutil.transformed
import dev.stpark.namchin.core.testutil.with
import dev.stpark.namchin.core.testutil.withConfidence
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
        val match = PoseSimilarity.compare(pose(STANDING), pose(STANDING))!!

        assertEquals(1f, match.score, 1e-4f)
        assertEquals(1f, match.minPartScore, 1e-4f)
        assertFalse(match.mirrored)
    }

    @Test
    fun `score is invariant to distance and position`() {
        val far = pose(STANDING.transformed(scale = 0.5f, dx = 0.1f, dy = 0.2f))

        assertEquals(1f, PoseSimilarity.compare(pose(STANDING), far)!!.score, 1e-4f)
    }

    @Test
    fun `score is invariant to image aspect ratio`() {
        val reference = pose(rightArmRaised, aspectRatio = 1f)
        val tallPreview = pose(rightArmRaised.inAspect(9f / 16f), aspectRatio = 9f / 16f)

        assertEquals(1f, PoseSimilarity.compare(reference, tallPreview)!!.score, 1e-4f)
    }

    @Test
    fun `different arm is reported as worst part with a failing segment`() {
        val match = PoseSimilarity.compare(pose(rightArmRaised), pose(STANDING))!!

        assertTrue(match.worstPart in setOf(BodyPart.RIGHT_UPPER_ARM, BodyPart.RIGHT_FOREARM))
        assertTrue(match.minPartScore < 0.1f)
        assertFalse(match.mirrored)
    }

    @Test
    fun `mirrored pose is accepted and flagged`() {
        val match = PoseSimilarity.compare(pose(rightArmRaised), pose(rightArmRaised.mirrored()))!!

        assertEquals(1f, match.score, 1e-4f)
        assertTrue(match.mirrored)
    }

    @Test
    fun `worst part is named from the current person's body when mirrored`() {
        val bentLeftArm = rightArmRaised.mirrored().with(Joint.LEFT_WRIST to Point(0.65f, 0.20f))

        val match = PoseSimilarity.compare(pose(rightArmRaised), pose(bentLeftArm))!!

        assertTrue(match.mirrored)
        assertEquals(BodyPart.LEFT_FOREARM, match.worstPart)
    }

    @Test
    fun `direct comparison is kept unless mirror is clearly better`() {
        // 대칭 자세: 정방향/반전 점수가 거의 같다 → 정방향 유지
        assertFalse(PoseSimilarity.compare(pose(STANDING), pose(STANDING))!!.mirrored)
    }

    @Test
    fun `previous mirror choice sticks when scores are close`() {
        val match = PoseSimilarity.compare(pose(STANDING), pose(STANDING), preferMirrored = true)!!

        assertTrue(match.mirrored)
    }

    @Test
    fun `opposite direction of a limb scores 0 for that part`() {
        val armUp = STANDING.with(
            Joint.RIGHT_ELBOW to Point(0.44f, 0.20f),
            Joint.RIGHT_WRIST to Point(0.43f, 0.10f),
        )

        val match = PoseSimilarity.compare(pose(armUp), pose(STANDING))!!

        assertEquals(0f, match.partScores.getValue(BodyPart.RIGHT_UPPER_ARM), 0.05f)
    }

    @Test
    fun `parts hidden in the current pose count as zero`() {
        val armsHidden = listOf(Joint.LEFT_ELBOW, Joint.LEFT_WRIST, Joint.RIGHT_ELBOW, Joint.RIGHT_WRIST)
            .fold(pose(STANDING)) { p, j -> p.withConfidence(j, 0.1f) }

        val match = PoseSimilarity.compare(pose(rightArmRaised), armsHidden)!!

        assertEquals(0.6f, match.score, 1e-4f)
        assertEquals(0f, match.minPartScore, 1e-4f)
    }

    @Test
    fun `parts hidden in the reference are not compared`() {
        val unreliableRef = pose(rightArmRaised).withConfidence(Joint.RIGHT_WRIST, 0.1f)

        val match = PoseSimilarity.compare(unreliableRef, pose(STANDING))!!

        assertFalse(BodyPart.RIGHT_FOREARM in match.partScores)
        assertEquals(9, match.partScores.size)
    }

    @Test
    fun `reference with too few parts is not comparable`() {
        val legsOnly = pose(STANDING.filterKeys { it.name.contains("HIP") || it.name.contains("KNEE") })

        assertFalse(PoseSimilarity.isComparable(legsOnly))
        assertNull(PoseSimilarity.compare(legsOnly, pose(STANDING)))
        assertTrue(PoseSimilarity.isComparable(pose(STANDING)))
    }
}
