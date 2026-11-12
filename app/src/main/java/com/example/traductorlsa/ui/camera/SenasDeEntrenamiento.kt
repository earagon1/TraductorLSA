package com.example.traductorlsa.ui.camera

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.traductorlsa.ui.CameraScreen
import com.example.traductorlsa.ui.screens.nombreParaMostrar
import java.text.Normalizer
import java.util.*
import org.json.JSONObject

/*
 * Las señas que se pueden entrenar: etiquetas, nombres y sus imágenes de referencia.
 * Salió de ui/CameraScreen.kt (T-A20).
 */

data class TrainingWordOption(
    val label: String,
    val imageAssetPath: String?,
    val isOfficial: Boolean
)


/**
 * Muestras que se busca juntar por sena antes de reentrenar.
 *
 * Es un valor de trabajo, no una medida: el numero real sale del script de
 * entrenamiento en Python, que es el que sabe cuantas repeticiones por clase
 * necesita. Se cambia aca y se actualizan las tres pantallas a la vez.
 */
internal const val MUESTRAS_OBJETIVO = 20

/** Modo y delegado de MediaPipe en la cámara (ver la medición en CameraScreen). */
internal const val CONFIG_MEDIAPIPE = "IMAGEN_GPU"

internal fun loadAllLabels(context: Context): List<String> {
    try {
        context.assets.open("words.json").bufferedReader().use { br ->
            val json = JSONObject(br.readText())
            val arr = json.optJSONArray("word_ids")
            if (arr != null && arr.length() > 0) {
                val out = ArrayList<String>(arr.length())
                for (i in 0 until arr.length()) {
                    val v = arr.optString(i)?.trim()
                    if (!v.isNullOrEmpty()) out += v
                }
                return out.distinct().sorted()
            }
        }
    } catch (_: Exception) {}

    return try {
        context.assets.open("labels.txt").bufferedReader().use { br ->
            br.readLines().map { it.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
        }
    } catch (_: Exception) {
        emptyList()
    }
}

/**
 * Lleva una etiqueta escrita a mano al formato de id del modelo.
 *
 * Minusculas, sin tildes ni signos, y CON espacios: "Por Favor" queda
 * "por favor", igual que la carpeta en frame_actions/ y la entrada de
 * words.json. Los espacios NO se convierten en guion bajo; eso es solo para
 * armar nombres de archivo, y de eso se ocupa `archivoDeSena`.
 */
internal fun normalizeTrainingLabel(input: String): String {
    val lower = input.trim().lowercase(Locale.getDefault())
    val noAccents = Normalizer.normalize(lower, Normalizer.Form.NFD)
        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")

    return noAccents
        .replace("[^a-z0-9_ ]".toRegex(), "")
        .replace("\\s+".toRegex(), " ")
        .trim()
}

/**
 * Normaliza una etiqueta para ORDENAR, no para mostrar.
 *
 * No pone tildes ni signos, asi que "como estas" queda igual
 * y no "¿Cómo estás?". Para texto visible o hablado va
 * `nombreParaMostrar`, que es el unico lugar donde vive esa traduccion.
 */
internal fun displayTrainingLabel(label: String): String =
    label.replace("_", " ").replace("\\s+".toRegex(), " ").trim()

/**
 * Nombre base del archivo del dibujo de una sena.
 *
 * Los ids llevan espacio ("por favor") pero los assets no, asi que se
 * reemplazan por guion bajo: dictionary/por_favor.jpg.
 */
internal fun archivoDeSena(label: String): String =
    normalizeTrainingLabel(label).replace(' ', '_')

internal fun findTrainingWordImageInAssets(context: Context, word: String): String? {
    val base = archivoDeSena(word)
    val exts = listOf("jpg", "jpeg", "png", "webp")
    val candidates = exts.map { "dictionary/$base.$it" } + exts.map { "$base.$it" }

    for (path in candidates) {
        try {
            context.assets.open(path).use { }
            return path
        } catch (_: Exception) {
        }
    }
    return null
}

internal fun loadTrainingWordOptions(context: Context): List<TrainingWordOption> =
    loadAllLabels(context).map { label ->
        TrainingWordOption(
            label = label,
            imageAssetPath = findTrainingWordImageInAssets(context, label),
            isOfficial = true
        )
    }
