package com.example.traductorlsa.startup

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Deja los assets pesados en la caché del sistema mientras se ve el splash.
 *
 * El README ya midió que el cuello de botella no es la inferencia sino la
 * etapa previa, y el modelo de landmarks pesa casi 8 MB: leerlo acá evita que
 * la primera traducción pague esa lectura de disco.
 *
 * No crea intérpretes ni toca la cámara a propósito: solo lee y descarta, así
 * no queda ningún recurso nativo abierto ni memoria retenida.
 */
object AssetWarmup {

    // Mismo orden de preferencia que TFLiteClassifier: primero el cuantizado,
    // que es el que se carga, y despues el float32 por si algun build no lo
    // incluyera. Entre los dos no llegan a 1 MB, asi que precargar los dos
    // sale mas barato que arriesgarse a que las dos listas se desincronicen.
    private val ASSETS = listOf(
        "words.json",
        "actions_15_opt.tflite",
        "actions_15_f32.tflite",
        "hand_landmarker.task",
    )

    suspend fun precargar(context: Context) {
        val buffer = ByteArray(64 * 1024)
        for (nombre in ASSETS) {
            withContext(Dispatchers.IO) {
                try {
                    context.assets.open(nombre).use { entrada ->
                        while (entrada.read(buffer) > 0) {
                            // Solo calentamos la caché: el contenido se descarta.
                        }
                    }
                } catch (t: Throwable) {
                    Log.w("AssetWarmup", "No se pudo precargar $nombre: ${t.message}")
                }
            }
        }
    }
}
