package dev.stpark.namchin.core.composition

import dev.stpark.namchin.core.guidance.Guidance
import dev.stpark.namchin.core.guidance.Guidance.CutPoint
import dev.stpark.namchin.core.guidance.Guidance.Side
import dev.stpark.namchin.core.model.Joint
import dev.stpark.namchin.core.model.Point
import dev.stpark.namchin.core.testutil.STANDING
import dev.stpark.namchin.core.testutil.inFrame
import dev.stpark.namchin.core.testutil.mirrored
import dev.stpark.namchin.core.testutil.pose
import dev.stpark.namchin.core.testutil.transformed
import dev.stpark.namchin.core.testutil.with
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CompositionAnalyzerTest {

    /** 실제처럼 영역 밖 관절 신뢰도는 0으로 만든 뒤 분석. */
    private fun analyze(points: Map<Joint, Point>, previous: List<Guidance> = emptyList()) =
        CompositionAnalyzer.analyze(pose(points).inFrame(), previous)

    private val lookingRight = STANDING.with(Joint.NOSE to Point(0.56f, 0.20f))

    // --- 전신 / 다리 길게 ---

    @Test
    fun `well framed full body facing front needs nothing`() {
        assertTrue(analyze(STANDING).isEmpty())
    }

    @Test
    fun `full body with feet floating high asks to put feet at the bottom`() {
        val small = STANDING.transformed(scale = 0.8f, dx = 0.1f, dy = 0f) // 발목 0.72

        assertEquals(listOf(Guidance.FeetToBottom), analyze(small))
    }

    @Test
    fun `feet cue persists until clearly fixed`() {
        val almost = STANDING.with(Joint.LEFT_ANKLE to Point(0.53f, 0.86f), Joint.RIGHT_ANKLE to Point(0.47f, 0.86f))

        assertTrue(analyze(almost).isEmpty())
        assertEquals(listOf(Guidance.FeetToBottom), analyze(almost, previous = listOf(Guidance.FeetToBottom)))
    }

    @Test
    fun `feet resting at the bottom edge is the goal, not an ankle cut`() {
        val atBottom = STANDING.transformed(scale = 1f, dx = 0f, dy = 0.06f) // 발목 0.96

        assertTrue(analyze(atBottom).isEmpty())
    }

    @Test
    fun `small full body low in a landscape never gets the headroom cue`() {
        // 발끝은 아래, 머리 위는 크게 빔 → 전신이므로 "아래로" 안내 금지 (발끝 안내와 번갈아 나오지 않게)
        val tiny = STANDING.transformed(scale = 0.6f, dx = 0.2f, dy = 0.38f) // 발목 0.92, 정수리 ≈ 0.46

        assertTrue(analyze(tiny).isEmpty())
        assertTrue(analyze(tiny, previous = listOf(Guidance.FeetToBottom)).isEmpty())
    }

    @Test
    fun `occluded legs with an implausible chain are ignored`() {
        // 테이블에 가려진 다리를 ML Kit이 추측: 무릎이 엉덩이보다 위
        val seated = STANDING.with(
            Joint.LEFT_KNEE to Point(0.53f, 0.50f),
            Joint.RIGHT_KNEE to Point(0.47f, 0.50f),
            Joint.LEFT_ANKLE to Point(0.53f, 0.60f),
            Joint.RIGHT_ANKLE to Point(0.47f, 0.60f),
        )

        assertFalse(Guidance.FeetToBottom in analyze(seated))
    }

    // --- 잘림 ---

    @Test
    fun `frame edge just past the ankles is a cut at the ankle`() {
        val shifted = STANDING.transformed(scale = 1f, dx = 0f, dy = 0.12f) // 발목 1.02

        assertTrue(Guidance.CutAtJoint(CutPoint.ANKLE) in analyze(shifted))
    }

    @Test
    fun `frame edge through the knees is a cut at the knee, not the ankle`() {
        val cues = analyze(STANDING.transformed(scale = 1f, dx = 0f, dy = 0.3f)) // 무릎 1.0, 발목 1.2

        assertTrue(Guidance.CutAtJoint(CutPoint.KNEE) in cues)
        assertFalse(Guidance.CutAtJoint(CutPoint.ANKLE) in cues)
    }

    @Test
    fun `knee cut persists in a wider band`() {
        val nearKnee = STANDING.transformed(scale = 1f, dx = 0f, dy = 0.25f) // 무릎 0.95

        assertFalse(Guidance.CutAtJoint(CutPoint.KNEE) in analyze(nearKnee))
        assertTrue(
            Guidance.CutAtJoint(CutPoint.KNEE) in analyze(nearKnee, previous = listOf(Guidance.CutAtJoint(CutPoint.KNEE))),
        )
    }

    @Test
    fun `hand at the side edge is a cut at the wrist`() {
        val reaching = STANDING.with(Joint.LEFT_ELBOW to Point(0.85f, 0.35f), Joint.LEFT_WRIST to Point(0.99f, 0.35f))

        assertTrue(Guidance.CutAtJoint(CutPoint.WRIST) in analyze(reaching))
    }

    // --- 머리 ---

    @Test
    fun `head slightly above the top edge is a head cut`() {
        val high = STANDING.transformed(scale = 1f, dx = 0f, dy = -0.2f) // 코 0.0

        assertTrue(Guidance.HeadCut in analyze(high))
    }

    @Test
    fun `face cut off at the eyes is still a head cut`() {
        val veryHigh = STANDING.transformed(scale = 1f, dx = 0f, dy = -0.25f) // 코 -0.05 → 신뢰도 0

        assertTrue(Guidance.HeadCut in analyze(veryHigh))
    }

    @Test
    fun `head cut persists until there is a little room`() {
        val justIn = STANDING.transformed(scale = 1f, dx = 0f, dy = -0.12f) // 코 0.08, 정수리 ≈ 0.01

        assertFalse(Guidance.HeadCut in analyze(justIn))
        assertTrue(Guidance.HeadCut in analyze(justIn, previous = listOf(Guidance.HeadCut)))
    }

    private val upperBody = mapOf(
        Joint.NOSE to Point(0.5f, 0.40f),
        Joint.LEFT_SHOULDER to Point(0.6f, 0.50f),
        Joint.RIGHT_SHOULDER to Point(0.4f, 0.50f),
        Joint.LEFT_HIP to Point(0.56f, 0.88f),
        Joint.RIGHT_HIP to Point(0.44f, 0.88f),
    )

    @Test
    fun `big upper body with lots of empty space above asks to tilt down`() {
        assertEquals(listOf(Guidance.TooMuchHeadroom), analyze(upperBody)) // 정수리 0.33
    }

    @Test
    fun `headroom cue persists until clearly fixed`() {
        val slightly = upperBody.transformed(scale = 1f, dx = 0f, dy = -0.15f) // 정수리 0.18

        assertTrue(analyze(slightly).isEmpty())
        assertEquals(listOf(Guidance.TooMuchHeadroom), analyze(slightly, previous = listOf(Guidance.TooMuchHeadroom)))
    }

    @Test
    fun `small upper body low in the frame is not judged for headroom`() {
        val smallLow = upperBody.transformed(scale = 0.4f, dx = 0.3f, dy = 0.55f) // 세로 크기 약 0.22

        assertTrue(analyze(smallLow).isEmpty())
    }

    // --- 시선 ---

    @Test
    fun `looking right while centered asks for space on screen right`() {
        assertEquals(listOf(Guidance.GazeSpace(Side.RIGHT)), analyze(lookingRight))
    }

    @Test
    fun `looking left is the mirror case`() {
        assertEquals(listOf(Guidance.GazeSpace(Side.LEFT)), analyze(lookingRight.mirrored()))
    }

    @Test
    fun `gaze cue persists until the face reaches the third`() {
        val moved = lookingRight.transformed(scale = 1f, dx = -0.12f, dy = 0f) // 코 0.44: 새로 안내할 정도는 아님

        assertTrue(analyze(moved).isEmpty())
        assertEquals(
            listOf(Guidance.GazeSpace(Side.RIGHT)),
            analyze(moved, previous = listOf(Guidance.GazeSpace(Side.RIGHT))),
        )
    }

    @Test
    fun `looking right from the left third is already good`() {
        assertTrue(analyze(lookingRight.transformed(scale = 1f, dx = -0.2f, dy = 0f)).isEmpty())
    }

    @Test
    fun `body turned three quarters while the face looks at the camera is not a gaze cue`() {
        // 어깨는 좁고 코가 어깨 중점에서 벗어나 있지만, 귀 기준으로 얼굴은 정면
        val turned = STANDING.with(
            Joint.LEFT_SHOULDER to Point(0.52f, 0.30f),
            Joint.RIGHT_SHOULDER to Point(0.46f, 0.30f),
            Joint.NOSE to Point(0.53f, 0.20f),
            Joint.LEFT_EAR to Point(0.56f, 0.19f),
            Joint.RIGHT_EAR to Point(0.50f, 0.19f),
        )

        assertTrue(analyze(turned).isEmpty())
    }

    @Test
    fun `gaze is judged even when one shoulder is hidden`() {
        val profile = pose(lookingRight).inFrame().let {
            it.copy(keypoints = it.keypoints + (Joint.LEFT_SHOULDER to it.keypoints.getValue(Joint.LEFT_SHOULDER).copy(confidence = 0f)))
        }

        assertEquals(listOf(Guidance.GazeSpace(Side.RIGHT)), CompositionAnalyzer.analyze(profile, emptyList()))
    }
}
