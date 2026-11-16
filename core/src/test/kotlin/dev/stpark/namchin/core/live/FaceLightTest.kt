package dev.stpark.namchin.core.live

import dev.stpark.namchin.core.model.Joint
import dev.stpark.namchin.core.model.Point
import dev.stpark.namchin.core.testutil.STANDING
import dev.stpark.namchin.core.testutil.pose
import dev.stpark.namchin.core.testutil.withConfidence
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FaceLightTest {

    @Test
    fun `a face much darker than the frame is in shadow`() {
        assertTrue(FaceLight.dark(faceLuma = 60f, frameLuma = 140f, wasDark = false))
    }

    @Test
    fun `an evenly lit face is fine`() {
        assertFalse(FaceLight.dark(faceLuma = 120f, frameLuma = 130f, wasDark = false))
    }

    @Test
    fun `a bright face is never in shadow even against a brighter sky`() {
        assertFalse(FaceLight.dark(faceLuma = 130f, frameLuma = 240f, wasDark = false))
    }

    @Test
    fun `shadow judgement persists until the face is clearly lit`() {
        // 비율 0.66: 처음엔 아니지만 어둡다고 본 직후엔 유지
        assertFalse(FaceLight.dark(faceLuma = 86f, frameLuma = 130f, wasDark = false))
        assertTrue(FaceLight.dark(faceLuma = 86f, frameLuma = 130f, wasDark = true))
    }

    @Test
    fun `face box is centred on the nose and sized by the ears`() {
        val box = FaceLight.faceBox(pose(STANDING))!!

        // 귀 사이 0.06 → 가로 반폭 0.036, 세로 반폭 0.042, 코 (0.5, 0.2) — 하늘이 섞이지 않게 얼굴만
        assertEquals(0.464f, box.left, 1e-4f)
        assertEquals(0.536f, box.right, 1e-4f)
        assertEquals(0.158f, box.top, 1e-4f)
        assertEquals(0.242f, box.bottom, 1e-4f)
    }

    @Test
    fun `face box keeps its pixel shape in a non-square image`() {
        val box = FaceLight.faceBox(pose(STANDING, aspectRatio = 0.75f))!!

        // 세로 반폭(정규화) = 픽셀 기준 세로 반폭 × 가로/세로 비율
        assertEquals(0.036f, (box.right - box.left) / 2, 1e-4f)
        assertEquals(0.0315f, (box.bottom - box.top) / 2, 1e-4f)
    }

    @Test
    fun `no face box without a reliable nose and ears`() {
        assertNull(FaceLight.faceBox(pose(STANDING).withConfidence(Joint.NOSE, 0.1f)))
        assertNull(FaceLight.faceBox(pose(STANDING).withConfidence(Joint.LEFT_EAR, 0.1f).withConfidence(Joint.RIGHT_EAR, 0.1f)))
    }

    @Test
    fun `face box falls back to one ear`() {
        val box = FaceLight.faceBox(pose(STANDING).withConfidence(Joint.LEFT_EAR, 0.1f))!!

        // 코~오른귀 0.03 × 2 = 귀 사이 추정 0.06
        assertEquals(0.464f, box.left, 1e-4f)
    }

    @Test
    fun `upright points map back to the sensor buffer for each rotation`() {
        // 버퍼 640×480, 세로로 세운 이미지 480×640
        assertEquals(Point(0.25f, 0.9f), FaceLight.toBuffer(Point(0.1f, 0.25f), 90))
        assertEquals(Point(0.75f, 0.1f), FaceLight.toBuffer(Point(0.1f, 0.25f), 270))
        assertEquals(Point(0.9f, 0.75f), FaceLight.toBuffer(Point(0.1f, 0.25f), 180))
        assertEquals(Point(0.1f, 0.25f), FaceLight.toBuffer(Point(0.1f, 0.25f), 0))
    }
}
