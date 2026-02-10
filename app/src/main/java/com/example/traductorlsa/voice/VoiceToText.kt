package com.example.traductorlsa.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class VoiceToText(
    context: Context,
    private val onPartial: (String) -> Unit,
    private val onFinal: (String) -> Unit,
    private val onError: (String) -> Unit
) {
    private val appContext = context.applicationContext
    private var recognizer: SpeechRecognizer? = null
    private var isListening = false
    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        private const val TAG = "VoiceToText"
    }

    fun start(localeTag: String = "es-AR") {
        // Aseguramos ejecución en el Main Thread para evitar excepciones del sistema
        mainHandler.post {
            Log.d(TAG, "Iniciando captura. Locale: $localeTag, isListening: $isListening")

            if (isListening) return@post

            if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
                onError("El servicio de reconocimiento no está disponible en este dispositivo.")
                return@post
            }

            if (recognizer == null) {
                recognizer = SpeechRecognizer.createSpeechRecognizer(appContext).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            Log.d(TAG, "onReadyForSpeech: Micrófono abierto")
                        }

                        override fun onBeginningOfSpeech() {
                            Log.d(TAG, "onBeginningOfSpeech: Usuario empezó a hablar")
                        }

                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() {
                            isListening = false
                            Log.d(TAG, "onEndOfSpeech")
                        }

                        override fun onError(error: Int) {
                            isListening = false
                            val message = when (error) {
                                SpeechRecognizer.ERROR_AUDIO -> "Error de audio"
                                SpeechRecognizer.ERROR_CLIENT -> "Error del cliente"
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permisos insuficientes"
                                SpeechRecognizer.ERROR_NETWORK -> "Error de red"
                                SpeechRecognizer.ERROR_NO_MATCH -> "No se encontró coincidencia"
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Servicio ocupado"
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No se detectó voz"
                                else -> "Error desconocido: $error"
                            }
                            Log.e(TAG, "Error STT: $message")
                            onError(message)
                        }

                        override fun onResults(results: Bundle?) {
                            val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                ?.firstOrNull().orEmpty()
                            Log.d(TAG, "Resultado Final: $text")
                            onFinal(text)
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                ?.firstOrNull().orEmpty()
                            if (text.isNotBlank()) {
                                Log.v(TAG, "Parcial: $text")
                                onPartial(text)
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                // Esto ayuda a que el servicio identifique el origen
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
            }

            try {
                isListening = true
                recognizer?.startListening(intent)
            } catch (e: Exception) {
                isListening = false
                onError("Fallo al iniciar el motor: ${e.message}")
            }
        }
    }

    fun stop() {
        mainHandler.post {
            Log.d(TAG, "Deteniendo reconocimiento")
            try {
                recognizer?.stopListening()
                recognizer?.destroy()
            } catch (e: Exception) {
                Log.e(TAG, "Error al destruir recognizer: ${e.message}")
            }
            recognizer = null
            isListening = false
        }
    }
}