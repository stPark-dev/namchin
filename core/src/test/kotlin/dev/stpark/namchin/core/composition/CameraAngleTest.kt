package dev.stpark.namchin.core.composition

import dev.stpark.namchin.core.guidance.Guidance
import dev.stpark.namchin.core.model.Joint
import dev.stpark.namchin.core.model.Point
import dev.stpark.namchin.core.testutil.STANDING
import dev.stpark.namchin.core.testutil.pose
import dev.stpark.namchin.core.testutil.with
import dev.stpark.namchin.core.testutil.withConfidence
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CameraAngleTest {

    /** 의자에 앉은 정면: 허벅지가 거의 수평 (골반 0.55 → 무릎 0.60), 정강이는 아래로. */
    private val seated = STANDING.with(
        Joint.LEFT_KNEE to Point(0.62f, 0.60f),
        Joint.RIGHT_KNEE to Point(0.38f, 0.60f),
        Joint.LEFT_ANKLE to Point(0.62f, 0.78f),
        Joint.RIGHT_ANKLE to Point(0.38f, 0.78f),
    )

    /** 무릎 위로 잘린 상반신 (다리 안 보임). */
    private val halfBody = pose(STANDING)
        .withConfidence(Joint.LEFT_KNEE, 0f).withConfidence(Joint.RIGHT_KNEE, 0f)
        .withConfidence(Joint.LEFT_ANKLE, 0f).withConfidence(Joint.RIGHT_ANKLE, 0f)

    @Test
    fun `full body shot with the phone tipped down asks to hold it upright`() {
        assertEquals(Guidance.PhoneTiltedDown, CameraAngle.cue(pose(STANDING), pitchDegrees = 25f, previous = emptyList()))
    }

    @Test
    fun `a level phone needs nothing`() {
        assertNull(CameraAngle.cue(pose(STANDING), pitchDegrees = 3f, previous = emptyList()))
        assertNull(CameraAngle.cue(pose(seated), pitchDegrees = 3f, previous = emptyList()))
    }

    @Test
    fun `a slight downward tilt is fine`() {
        assertNull(CameraAngle.cue(pose(STANDING), pitchDegrees = 15f, previous = emptyList()))
    }

    @Test
    fun `the tilt cue persists until the phone is clearly upright`() {
        assertEquals(Guidance.PhoneTiltedDown, CameraAngle.cue(pose(STANDING), 15f, previous = listOf(Guidance.PhoneTiltedDown)))
        assertNull(CameraAngle.cue(pose(STANDING), 8f, previous = listOf(Guidance.PhoneTiltedDown)))
    }

    @Test
    fun `tipping down for a half body shot is not about legs`() {
        assertNull(CameraAngle.cue(halfBody, pitchDegrees = 25f, previous = emptyList()))
    }

    @Test
    fun `looking up at a seated person asks to shoot from slightly above`() {
        assertEquals(Guidance.ShootFromAbove, CameraAngle.cue(pose(seated), pitchDegrees = -15f, previous = emptyList()))
    }

    @Test
    fun `looking up at a standing person is left alone`() {
        // 서 있는 전신은 약간 낮은 곳에서 올려 찍는 것도 기류의 방식 (다리 길게)
        assertNull(CameraAngle.cue(pose(STANDING), pitchDegrees = -15f, previous = emptyList()))
    }

    @Test
    fun `the from-above cue persists until the phone is clearly level`() {
        assertEquals(Guidance.ShootFromAbove, CameraAngle.cue(pose(seated), -7f, previous = listOf(Guidance.ShootFromAbove)))
        assertNull(CameraAngle.cue(pose(seated), -7f, previous = emptyList()))
    }

    @Test
    fun `unknown pitch says nothing`() {
        assertNull(CameraAngle.cue(pose(STANDING), pitchDegrees = null, previous = emptyList()))
    }

    /** 카메라를 보고 앉음: 허벅지가 카메라 쪽으로 짧게 찍혀 거의 세로로 보인다. */
    private val seatedFacingCamera = STANDING.with(
        Joint.LEFT_KNEE to Point(0.54f, 0.62f),
        Joint.RIGHT_KNEE to Point(0.46f, 0.62f),
        Joint.LEFT_ANKLE to Point(0.54f, 0.80f),
        Joint.RIGHT_ANKLE to Point(0.46f, 0.80f),
    )

    @Test
    fun `a person seated facing the camera is not told to stop tilting down`() {
        assertNull(CameraAngle.cue(pose(seatedFacingCamera), pitchDegrees = 25f, previous = emptyList()))
    }

    @Test
    fun `one raised thigh with the other leg hidden is not seated`() {
        val footOnStep = pose(seated)
            .withConfidence(Joint.RIGHT_HIP, 0f).withConfidence(Joint.RIGHT_KNEE, 0f).withConfidence(Joint.RIGHT_ANKLE, 0f)

        assertNull(CameraAngle.cue(footOnStep, pitchDegrees = -15f, previous = emptyList()))
    }

    @Test
    fun `seated needs both thighs level`() {
        assertEquals(true, CameraAngle.seated(pose(seated)))
        assertEquals(false, CameraAngle.seated(pose(STANDING)))
    }
}
