package com.example.traductorlsa.speech

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class SpeechManager(context: Context) {

    private var tts: TextToSpeech? = null
    private var ready = false

    // Los ajustes pueden llegar antes de que el motor esté listo, así que se
    // guardan y se aplican en cuanto inicializa.
    private var velocidad = 1.0f
    private var tono = 1.0f
    private var locale: Locale = Locale("es", "AR")

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ready = true
                aplicar()
                Log.d("SpeechManager", "TTS inicializado")
            } else {
                Log.e("SpeechManager", "Error inicializando TTS: status=$status")
            }
        }
    }

    /** Velocidad, tono y variante del español, desde Ajustes. */
    fun configurar(velocidad: Float, tono: Float, locale: Locale) {
        this.velocidad = velocidad
        this.tono = tono
        this.locale = locale
        if (ready) aplicar()
    }

    private fun aplicar() {
        val motor = tts ?: return
        val res = motor.setLanguage(locale)
        if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
            // El dispositivo no tiene esa variante instalada: caemos a español a secas.
            Log.w("SpeechManager", "Variante $locale no disponible, se usa español neutro")
            motor.setLanguage(Locale("es"))
        }
        motor.setSpeechRate(velocidad)
        motor.setPitch(tono)
    }

    fun speak(text: String) {
        if (!ready || text.isBlank() || text == "-" || text == "Sin datos" || text == "Unknown") {
            Log.d("SpeechManager", "No se pronuncia: $text")
            return
        }
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
