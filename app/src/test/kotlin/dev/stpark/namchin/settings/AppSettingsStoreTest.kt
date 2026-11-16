package dev.stpark.namchin.settings

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AppSettingsStoreTest {

    private class MemoryStore : KeyValueStore {
        val values = mutableMapOf<String, Any>()
        override fun getBoolean(key: String, default: Boolean) = values[key] as Boolean? ?: default
        override fun putBoolean(key: String, value: Boolean) {
            values[key] = value
        }
        override fun getFloat(key: String, default: Float) = values[key] as Float? ?: default
        override fun putFloat(key: String, value: Float) {
            values[key] = value
        }
        override fun getInt(key: String, default: Int) = values[key] as Int? ?: default
        override fun putInt(key: String, value: Int) {
            values[key] = value
        }
    }

    @Test
    fun `defaults are shutter sound on, grid on, debug info hidden, 1x, 3 to 4, portrait off`() {
        assertEquals(
            AppSettings(shutterSound = true, grid = true, debugInfo = false, defaultZoom = 1f, instaRatio = false, portrait = false, burstCount = 3),
            AppSettingsStore(MemoryStore()).settings.value,
        )
    }

    @Test
    fun `updates are visible at once and persisted`() {
        val memory = MemoryStore()
        val store = AppSettingsStore(memory)

        val changed = AppSettings(shutterSound = false, grid = false, debugInfo = true, defaultZoom = 2f, instaRatio = true, portrait = true, burstCount = 10)
        store.update(changed)

        assertEquals(changed, store.settings.value)
        assertEquals(changed, AppSettingsStore(memory).settings.value)
    }

    @Test
    fun `burst count is one of 3 5 10, anything else falls back to 3`() {
        assertEquals(listOf(3, 5, 10), AppSettings.BURST_CHOICES)
        val memory = MemoryStore().apply { values["burst_count"] = 42 }

        assertEquals(3, AppSettingsStore(memory).settings.value.burstCount)
    }
}
