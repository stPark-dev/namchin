package dev.stpark.namchin.importer

import dev.stpark.namchin.core.crop.CropRect
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CropGeometryTest {

    private val full = NormRect(0f, 0f, 1f, 1f)

    @Test
    fun `dragging a corner moves only that corner`() {
        assertEquals(NormRect(0.1f, 0.2f, 1f, 1f), CropGeometry.drag(full, Corner.TOP_LEFT, 0.1f, 0.2f))
        assertEquals(NormRect(0f, 0f, 0.9f, 0.8f), CropGeometry.drag(full, Corner.BOTTOM_RIGHT, -0.1f, -0.2f))
        assertEquals(NormRect(0f, 0.1f, 0.7f, 1f), CropGeometry.drag(full, Corner.TOP_RIGHT, -0.3f, 0.1f))
        assertEquals(NormRect(0.2f, 0f, 1f, 0.6f), CropGeometry.drag(full, Corner.BOTTOM_LEFT, 0.2f, -0.4f))
    }

    @Test
    fun `corners stay inside the image`() {
        assertEquals(full, CropGeometry.drag(full, Corner.TOP_LEFT, -0.5f, -0.5f))
    }

    @Test
    fun `rect never gets smaller than the minimum size`() {
        val dragged = CropGeometry.drag(full, Corner.TOP_LEFT, 0.99f, 0.99f)

        assertEquals(1f - CropGeometry.MIN_SIZE, dragged.left, 1e-6f)
        assertEquals(1f - CropGeometry.MIN_SIZE, dragged.top, 1e-6f)
    }

    @Test
    fun `converts between detector pixels and normalized rects`() {
        val norm = CropGeometry.fromDetector(CropRect(10, 40, 90, 140), gridWidth = 100, gridHeight = 200)

        assertEquals(NormRect(0.1f, 0.2f, 0.9f, 0.7f), norm)
        assertEquals(PixelRect(108, 432, 972, 1512), CropGeometry.toPixels(norm, width = 1080, height = 2160))
    }
}
