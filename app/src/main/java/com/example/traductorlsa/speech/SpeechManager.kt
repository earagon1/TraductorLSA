package com.example.traductorlsa.speech

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.*

class SpeechManager(context: Context) {

    private var tts: TextToSpeech? = null
    private var ready = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val res = tts?.setLanguage(Locale("es", "MX"))
                ready = res == TextToSpeech.LANG_AVAILABLE || res == TextToSpeech.LANG_COUNTRY_AVAILABLE
                Log.d("SpeechManager", "TTS inicializado. Ready=$ready")
            } else {
                Log.e("SpeechManager", "Error inicializando TTS: status=$status")
            }
        }
        tts?.setSpeechRate(1.0f)  // velocidad normal (1.0f = default)
        tts?.setPitch(1.0f)       // tono normal
    }

    fun speak(text: String) {
        if (!ready || text.isBlank() || text == "-" || text == "Sin datos" || text == "Unknown") {
            Log.d("SpeechManager", "⚠️ No se pronuncia: $text")
            return
        }
        Log.d("SpeechManager", "🔊 Hablando: $text")
        val params = Bundle()

        params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "prediction_id")

    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        ready = false
    }
}
