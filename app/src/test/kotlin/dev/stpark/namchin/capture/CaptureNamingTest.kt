package dev.stpark.namchin.capture

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.ZoneId

class CaptureNamingTest {

    @Test
    fun `file name uses local time down to milliseconds`() {
        // 2026-11-10 11:15:00.123 KST
        val epochMs = 1_794_276_900_123L

        assertEquals("NAMCHIN_20261110_111500_123", CaptureNaming.displayName(epochMs, ZoneId.of("Asia/Seoul")))
    }

    @Test
    fun `photos go to their own DCIM folder`() {
        assertEquals("DCIM/남친렌즈", CaptureNaming.RELATIVE_PATH)
    }
}
