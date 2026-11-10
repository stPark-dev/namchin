package dev.stpark.namchin.settings

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AppSettingsStoreTest {

    private class MemoryStore : KeyValueStore {
        val values = mutableMapOf<String, Boolean>()
        override fun getBoolean(key: String, default: Boolean) = values[key] ?: default
        override fun putBoolean(key: String, value: Boolean) {
            values[key] = value
        }
    }

    @Test
    fun `defaults are shutter sound on, grid off, debug info hidden`() {
        assertEquals(
            AppSettings(shutterSound = true, grid = false, debugInfo = false),
            AppSettingsStore(MemoryStore()).settings.value,
        )
    }

    @Test
    fun `updates are visible at once and persisted`() {
        val memory = MemoryStore()
        val store = AppSettingsStore(memory)

        val changed = AppSettings(shutterSound = false, grid = true, debugInfo = true)
        store.update(changed)

        assertEquals(changed, store.settings.value)
        assertEquals(changed, AppSettingsStore(memory).settings.value)
    }
}
