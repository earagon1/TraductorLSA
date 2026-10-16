package com.example.traductorlsa.federado

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.concurrent.Executors

/**
 * Solo en el build debug: entrena la cabeza del modelo en el teléfono con las muestras
 * propias y muestra los tiempos (T-F11/T-F14). Se abre desde la PC con
 *
 *   adb shell am start -n com.example.traductorlsa/.federado.EntrenamientoLocalActivity --es codigo P1 --ez auto true
 *
 * El resultado queda en files/federado/resultado_<ts>.json y en logcat (SENAR_FED).
 */
class EntrenamientoLocalActivity : ComponentActivity() {

    private val hilo = Executors.newSingleThreadExecutor()
    private var estado by mutableStateOf("Listo para entrenar con las muestras de la sesión guiada.")
    private var corriendo by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val codigo = intent.getStringExtra("codigo") ?: "P1"
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(
                        Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text("Entrenamiento local de la cabeza", style = MaterialTheme.typography.titleLarge)
                        Text("Participante: $codigo. Entrena con la tanda 1 y evalúa con la tanda 2.")
                        Button(onClick = { correr(codigo) }, enabled = !corriendo) { Text("Entrenar con mis muestras") }
                        Text(estado)
                    }
                }
            }
        }
        if (intent.getBooleanExtra("auto", false)) correr(codigo)
    }

    private fun correr(codigo: String) {
        corriendo = true
        hilo.execute {
            estado = try {
                val r = EntrenamientoLocal.correr(this, codigo) { msg -> runOnUiThread { estado = msg } }
                Log.i("SENAR_FED", r.toString())
                "Listo.\n\n" + r.toString(2)
            } catch (e: Exception) {
                Log.e("SENAR_FED", "falló", e)
                "Error: ${e.message}"
            }
            runOnUiThread { corriendo = false }
        }
    }

    override fun onDestroy() {
        hilo.shutdown()
        super.onDestroy()
    }
}
