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

/**
 * Reconocimiento de voz, con el procesamiento en el dispositivo por defecto.
 *
 * Que el audio no salga del telefono es la premisa del proyecto, asi que
 * `soloLocal` viene activado y se le pide al sistema `EXTRA_PREFER_OFFLINE`.
 *
 * Es una PREFERENCIA, no una garantia: el servicio de reconocimiento puede
 * ignorarla. Por eso, cuando `soloLocal` esta activo, los errores de red y de
 * servidor no se reportan con su nombre generico sino como lo que casi
 * siempre significan en ese modo -- que falta el paquete de voz del idioma --,
 * y nunca se reintenta contra la nube por atras. Si el reconocimiento tuviera
 * que salir a internet, la usuaria se entera y decide.
 *
 * La forma de verificarlo de verdad es poner el telefono en modo avion y
 * comprobar que sigue transcribiendo.
 */
class VoiceToText(
    context: Context,
    private val onPartial: (String) -> Unit,
    private val onFinal: (String) -> Unit,
    private val onError: (String) -> Unit
) {
    private val appContext = context.applicationContext
    private var recognizer: SpeechRecognizer? = null
    private var isListening = false
    private var soloLocal = true
    private val mainHandler = Handler(Looper.getMainLooper())

    companion object {
        private const val TAG = "VoiceToText"

        // SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED y ERROR_LANGUAGE_UNAVAILABLE
        // llegaron en API 33. Se escriben los valores en lugar de referenciar las
        // constantes para no arrastrar un aviso de lint con minSdk 24: son enteros
        // constantes, y los dispositivos viejos simplemente nunca los emiten.
        private const val ERROR_IDIOMA_NO_SOPORTADO = 12
        private const val ERROR_IDIOMA_NO_DISPONIBLE = 13

        private const val FALTA_PAQUETE =
            "Falta el paquete de voz sin conexion de este idioma. Instalalo desde " +
                "los ajustes de Android, o desactiva «Reconocer la voz sin conexion» " +
                "en Ajustes para usar internet."
    }

    fun start(localeTag: String = "es-AR", soloLocal: Boolean = true) {
        this.soloLocal = soloLocal
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
                            val message = mensajeDeError(error)
                            Log.e(TAG, "Error STT ($error, soloLocal=$soloLocal): $message")
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
                // El pedido de procesamiento en el dispositivo. El servicio
                // puede ignorarlo; onError distingue ese caso.
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, soloLocal)
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

    /**
     * Traduce el codigo de error a algo que se pueda leer en pantalla.
     *
     * Con `soloLocal` activo varios errores no significan lo que dice su
     * nombre. Un "error de red" cuando se pidio procesamiento en el
     * dispositivo no es que falle internet: es que el reconocedor no encontro
     * el modelo local y quiso salir a buscarlo. Decirlo asi evita que parezca
     * que la app esta rota cuando en realidad falta descargar un paquete.
     */
    private fun mensajeDeError(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "Error de audio"
        SpeechRecognizer.ERROR_CLIENT -> "Error del cliente"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permisos insuficientes"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "El servicio de voz está ocupado"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No se detectó voz"
        SpeechRecognizer.ERROR_NO_MATCH -> "No se entendió lo que se dijo"

        ERROR_IDIOMA_NO_SOPORTADO, ERROR_IDIOMA_NO_DISPONIBLE -> FALTA_PAQUETE

        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
        SpeechRecognizer.ERROR_SERVER ->
            if (soloLocal) FALTA_PAQUETE else "No se pudo conectar con el servicio de voz"

        else -> "Error desconocido: $error"
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