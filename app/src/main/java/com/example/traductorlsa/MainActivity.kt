package com.example.traductorlsa

import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.traductorlsa.ui.LsaTranslatorApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Con targetSdk 36 Android 15 ya dibuja de borde a borde por su cuenta;
        // esto hace que se comporte igual en las versiones anteriores. Cada
        // pantalla se encarga de respetar las barras con windowInsetsPadding.
        enableEdgeToEdge()

        // Sube el volumen de música al máximo (para TextToSpeech)
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)

        setContent {
            // Entry point de toda la UI Compose
            LsaTranslatorApp()
        }
    }
}
