package dev.stpark.namchin.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 설정 화면에서 바꾸는 값. 앱을 다시 켜도 유지된다. */
data class AppSettings(
    val shutterSound: Boolean = true,
    /** 격자선 (기류: "무조건 격자선부터 켜세요") */
    val grid: Boolean = true,
    /** 개발용 분석 속도·일치도 표시 */
    val debugInfo: Boolean = false,
    /** 카메라를 켤 때 배율. 1보다 크면 확대 촬영 안내도 한다 */
    val defaultZoom: Float = 1f,
    /** 레퍼런스 없이 찍을 때 인스타 4:5로 저장 (아니면 3:4) */
    val instaRatio: Boolean = false,
    /** 인물사진 모드(배경 흐림). 지원하는 폰에서만 실제로 켜진다 */
    val portrait: Boolean = false,
    /** 연속 촬영 장수 (자동 셔터·셔터 길게 누르기) */
    val burstCount: Int = DEFAULT_BURST,
) {
    companion object {
        const val DEFAULT_BURST = 3

        /** 고를 수 있는 연속 촬영 장수 (최대 10장) */
        val BURST_CHOICES = listOf(3, 5, 10)
    }
}

interface KeyValueStore {
    fun getBoolean(key: String, default: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun getFloat(key: String, default: Float): Float
    fun putFloat(key: String, value: Float)
    fun getInt(key: String, default: Int): Int
    fun putInt(key: String, value: Int)
}

private class SharedPreferencesStore(private val prefs: SharedPreferences) : KeyValueStore {
    override fun getBoolean(key: String, default: Boolean) = prefs.getBoolean(key, default)
    override fun putBoolean(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()
    override fun getFloat(key: String, default: Float) = prefs.getFloat(key, default)
    override fun putFloat(key: String, value: Float) = prefs.edit().putFloat(key, value).apply()
    override fun getInt(key: String, default: Int) = prefs.getInt(key, default)
    override fun putInt(key: String, value: Int) = prefs.edit().putInt(key, value).apply()
}

class AppSettingsStore(private val store: KeyValueStore) {
    private val defaults = AppSettings()
    private val _settings = MutableStateFlow(
        AppSettings(
            shutterSound = store.getBoolean(KEY_SHUTTER_SOUND, defaults.shutterSound),
            grid = store.getBoolean(KEY_GRID, defaults.grid),
            debugInfo = store.getBoolean(KEY_DEBUG_INFO, defaults.debugInfo),
            defaultZoom = store.getFloat(KEY_DEFAULT_ZOOM, defaults.defaultZoom),
            instaRatio = store.getBoolean(KEY_INSTA_RATIO, defaults.instaRatio),
            portrait = store.getBoolean(KEY_PORTRAIT, defaults.portrait),
            burstCount = store.getInt(KEY_BURST_COUNT, defaults.burstCount)
                .takeIf { it in AppSettings.BURST_CHOICES } ?: AppSettings.DEFAULT_BURST,
        ),
    )
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun update(settings: AppSettings) {
        store.putBoolean(KEY_SHUTTER_SOUND, settings.shutterSound)
        store.putBoolean(KEY_GRID, settings.grid)
        store.putBoolean(KEY_DEBUG_INFO, settings.debugInfo)
        store.putFloat(KEY_DEFAULT_ZOOM, settings.defaultZoom)
        store.putBoolean(KEY_INSTA_RATIO, settings.instaRatio)
        store.putBoolean(KEY_PORTRAIT, settings.portrait)
        store.putInt(KEY_BURST_COUNT, settings.burstCount)
        _settings.value = settings
    }

    companion object {
        private const val KEY_SHUTTER_SOUND = "shutter_sound"
        private const val KEY_GRID = "grid"
        private const val KEY_PORTRAIT = "portrait"
        private const val KEY_BURST_COUNT = "burst_count"
        private const val KEY_DEBUG_INFO = "debug_info"
        private const val KEY_DEFAULT_ZOOM = "default_zoom"
        private const val KEY_INSTA_RATIO = "insta_ratio"

        @Volatile private var instance: AppSettingsStore? = null

        fun get(context: Context): AppSettingsStore = instance ?: synchronized(this) {
            instance ?: AppSettingsStore(
                SharedPreferencesStore(context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)),
            ).also { instance = it }
        }
    }
}
