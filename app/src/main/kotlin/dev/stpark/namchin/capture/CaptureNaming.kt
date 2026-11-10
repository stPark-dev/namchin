package dev.stpark.namchin.capture

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object CaptureNaming {
    const val RELATIVE_PATH = "DCIM/남친렌즈"
    private val FORMAT = DateTimeFormatter.ofPattern("'NAMCHIN_'yyyyMMdd_HHmmss_SSS")

    fun displayName(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        FORMAT.format(Instant.ofEpochMilli(epochMs).atZone(zone))
}
