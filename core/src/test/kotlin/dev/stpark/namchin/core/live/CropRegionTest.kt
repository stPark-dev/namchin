package dev.stpark.namchin.core.live

import dev.stpark.namchin.core.model.Joint
import dev.stpark.namchin.core.model.Point
import dev.stpark.namchin.core.testutil.pose
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CropRegionTest {

    @Test
    fun `same aspect keeps the whole frame`() {
        assertEquals(Region(0f, 0f, 1f, 1f), CropRegion.centerCrop(sourceAspect = 0.75f, targetAspect = 0.75f))
    }

    @Test
    fun `wider target trims top and bottom`() {
        // 3:4 프리뷰에서 1:1 영역 → 높이의 3/4만 사용
        val region = CropRegion.centerCrop(sourceAspect = 0.75f, targetAspect = 1f)

        assertEquals(Region(0f, 0.125f, 1f, 0.875f), region)
    }

    @Test
    fun `taller target trims left and right`() {
        // 3:4 프리뷰에서 9:16 영역 → 너비의 0.75 사용
        val region = CropRegion.centerCrop(sourceAspect = 0.75f, targetAspect = 9f / 16f)

        assertEquals(0.125f, region.left, 1e-5f)
        assertEquals(0.875f, region.right, 1e-5f)
        assertEquals(0f, region.top)
        assertEquals(1f, region.bottom)
    }

    @Test
    fun `remap expresses keypoints in region coordinates with the region aspect`() {
        val source = pose(mapOf(Joint.NOSE to Point(0.5f, 0.25f)), aspectRatio = 0.75f)
        val region = Region(0f, 0.125f, 1f, 0.875f)

        val remapped = CropRegion.remap(source, region)

        assertEquals(1f, remapped.aspectRatio, 1e-5f)
        assertEquals(Point(0.5f, (0.25f - 0.125f) / 0.75f), remapped.keypoints.getValue(Joint.NOSE).position)
    }
}
