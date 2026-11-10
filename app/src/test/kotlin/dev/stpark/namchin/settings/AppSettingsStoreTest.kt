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
    fun `defaults are shutter sound on and grid off`() {
        assertEquals(AppSettings(shutterSound = true, grid = false), AppSettingsStore(MemoryStore()).settings.value)
    }

    @Test
    fun `updates are visible at once and persisted`() {
        val memory = MemoryStore()
        val store = AppSettingsStore(memory)

        store.update(AppSettings(shutterSound = false, grid = true))

        assertEquals(AppSettings(shutterSound = false, grid = true), store.settings.value)
        assertEquals(AppSettings(shutterSound = false, grid = true), AppSettingsStore(memory).settings.value)
    }
}
