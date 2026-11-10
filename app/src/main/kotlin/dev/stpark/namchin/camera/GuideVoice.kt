package dev.stpark.namchin.camera

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/** 한국어 음성 안내. 이어폰이 연결돼 있으면 이어폰으로 나간다. 한국어 음성이 없으면 조용히 무시. */
class GuideVoice(context: Context) : TextToSpeech.OnInitListener, AutoCloseable {
    @Volatile private var ready = false

    /** 초기화 전에 들어온 마지막 안내. 준비되면 말한다. */
    @Volatile private var pending: String? = null
    private val tts = TextToSpeech(context.applicationContext, this)

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            Log.w(TAG, "TTS 초기화 실패: $status")
            return
        }
        val result = tts.setLanguage(Locale.KOREAN)
        ready = result >= TextToSpeech.LANG_AVAILABLE
        if (!ready) Log.w(TAG, "한국어 TTS 사용 불가: $result")
        pending?.let { if (ready) speak(it) }
        pending = null
    }

    /** 앞의 안내를 끊고 최신 안내를 말한다. */
    fun speak(text: String) {
        if (ready) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID) else pending = text
    }

    /** 말하던 것을 바로 멈춘다 (음성 끄기, 화면 이탈). */
    fun stop() {
        pending = null
        if (ready) tts.stop()
    }

    override fun close() {
        tts.stop()
        tts.shutdown()
    }

    private companion object {
        const val TAG = "GuideVoice"
        const val UTTERANCE_ID = "guide"
    }
}
