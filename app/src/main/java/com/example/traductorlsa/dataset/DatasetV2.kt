package com.example.traductorlsa.dataset

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import java.io.File
import java.util.UUID

/**
 * El dataset v2 en disco: una muestra por línea (JSON Lines), al lado del
 * `lsa_samples.json` v1 y en la misma carpeta, así lo comparte el mismo botón Exportar.
 *
 * Convive con el v1 a propósito. El v1 lo siguen leyendo la pantalla del dataset y
 * `training_model.py`; migrarlo dos días antes de una sesión de captura era arriesgar
 * los datos por nada. Cada muestra nueva se escribe en los dos.
 *
 * Agregar es O(1): no se reescribe el archivo entero en cada muestra, como hace el v1.
 */
object DatasetV2 {
    const val NOMBRE = "lsa_samples_v2.jsonl"

    fun archivo(context: Context): File {
        val base = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        return File(base, NOMBRE)
    }

    fun agregar(context: Context, linea: String) {
        synchronized(AlmacenSeguro.candado) { archivo(context).appendText(linea + "\n") }
    }

    /**
     * Guarda una muestra con sus frames crudos. Devuelve false si no se pudo: quien llama
     * no tiene que seguir como si se hubiera guardado (auditoría, TL-C5).
     */
    fun guardarMuestra(
        context: Context,
        etiqueta: String,
        participantId: String?,
        captura: CapturaCruda,
        sesion: MetaSesion?,
    ): Boolean {
        return try {
            val versionApp = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
            } catch (_: Exception) { "?" }
            agregar(
                context,
                lineaMuestraV2(
                    uuid = UUID.randomUUID().toString(),
                    tsEpochMs = System.currentTimeMillis(),
                    etiqueta = etiqueta,
                    participantId = participantId,
                    captura = captura,
                    sesion = sesion,
                    dispositivo = "${Build.MANUFACTURER} ${Build.MODEL}",
                    android = Build.VERSION.RELEASE ?: "?",
                    versionApp = versionApp,
                ),
            )
            true
        } catch (e: Exception) {
            Log.e("DatasetV2", "No se pudo guardar la muestra v2: ${e.message}")
            false
        }
    }

    /** Cuántas muestras de la sesión guiada tiene un código de planilla (P1, P2…). */
    fun contarDeCodigo(context: Context, codigo: String): Int {
        val f = archivo(context)
        if (!f.exists()) return 0
        val marca = "\"sesion\":{\"codigo\":\"$codigo\""
        return f.useLines { lineas -> lineas.count { it.contains(marca) } }
    }

    /**
     * Saca la última muestra, para el botón Deshacer. Devuelve false si no había ninguna.
     * Reescribe el archivo, pero de forma atómica y dejando la versión anterior en `.bak`:
     * antes un corte a mitad de `writeText` podía dejar el v2 vacío (auditoría, TL-C5).
     */
    fun quitarUltima(context: Context): Boolean = quitarUltima(archivo(context))

    fun quitarUltima(f: File): Boolean {
        synchronized(AlmacenSeguro.candado) {
            if (!f.exists()) return false
            val lineas = f.readLines().filter { it.isNotBlank() }
            if (lineas.isEmpty()) return false
            AlmacenSeguro.reescribirLineas(f, lineas.dropLast(1))
            return true
        }
    }
}
