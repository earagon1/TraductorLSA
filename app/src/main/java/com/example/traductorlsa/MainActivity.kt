package com.example.traductorlsa

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

        // El volumen ya no se sube acá: lo hace SpeechManager, solo cuando la app va a
        // hablar y si está demasiado bajo (auditoría, TL-A6; ver volumenParaHablar).

        setContent {
            // Entry point de toda la UI Compose
            LsaTranslatorApp()
        }
    }
}
