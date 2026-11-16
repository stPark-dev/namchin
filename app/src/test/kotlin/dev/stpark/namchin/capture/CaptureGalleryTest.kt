package dev.stpark.namchin.capture

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CaptureGalleryTest {

    @Test
    fun `looks for photos in the same folder the camera saves to`() {
        assertEquals(listOf(CaptureNaming.RELATIVE_PATH + "%"), CaptureGallery.SELECTION_ARGS.toList())
        assertTrue("relative_path" in CaptureGallery.SELECTION)
    }

    @Test
    fun `newest photo comes first`() {
        assertTrue(CaptureGallery.SORT.endsWith("DESC"))
    }
}
