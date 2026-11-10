package dev.stpark.namchin.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 설정 화면에서 바꾸는 값. 앱을 다시 켜도 유지된다. */
data class AppSettings(
    val shutterSound: Boolean = true,
    val grid: Boolean = false,
    /** 개발용 분석 속도·일치도 표시 */
    val debugInfo: Boolean = false,
)

interface KeyValueStore {
    fun getBoolean(key: String, default: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
}

private class SharedPreferencesStore(private val prefs: SharedPreferences) : KeyValueStore {
    override fun getBoolean(key: String, default: Boolean) = prefs.getBoolean(key, default)
    override fun putBoolean(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()
}

class AppSettingsStore(private val store: KeyValueStore) {
    private val defaults = AppSettings()
    private val _settings = MutableStateFlow(
        AppSettings(
            shutterSound = store.getBoolean(KEY_SHUTTER_SOUND, defaults.shutterSound),
            grid = store.getBoolean(KEY_GRID, defaults.grid),
            debugInfo = store.getBoolean(KEY_DEBUG_INFO, defaults.debugInfo),
        ),
    )
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun update(settings: AppSettings) {
        store.putBoolean(KEY_SHUTTER_SOUND, settings.shutterSound)
        store.putBoolean(KEY_GRID, settings.grid)
        store.putBoolean(KEY_DEBUG_INFO, settings.debugInfo)
        _settings.value = settings
    }

    companion object {
        private const val KEY_SHUTTER_SOUND = "shutter_sound"
        private const val KEY_GRID = "grid"
        private const val KEY_DEBUG_INFO = "debug_info"

        @Volatile private var instance: AppSettingsStore? = null

        fun get(context: Context): AppSettingsStore = instance ?: synchronized(this) {
            instance ?: AppSettingsStore(
                SharedPreferencesStore(context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)),
            ).also { instance = it }
        }
    }
}
