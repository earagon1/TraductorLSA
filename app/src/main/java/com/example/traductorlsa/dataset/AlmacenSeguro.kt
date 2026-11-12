package com.example.traductorlsa.dataset

import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** El dataset existe pero no se puede leer. Nunca se lo reemplaza por uno vacío. */
class DatasetIlegible(mensaje: String) : IOException(mensaje)

/**
 * Escrituras del dataset que no pueden perder lo que ya había (auditoría, TL-C5).
 *
 * Antes, si `lsa_samples.json` no se podía leer (un corte de luz a mitad de escritura,
 * por ejemplo), la lectura devolvía un dataset vacío sin avisar, y la captura siguiente
 * lo pisaba con una sola muestra: se perdía todo. Y `writeText` escribía sobre el archivo
 * real, así que un corte a mitad dejaba justamente ese archivo roto. Ahora:
 *
 *  - [escribirAtomico]: se escribe en un temporal, se sincroniza al disco, la versión
 *    anterior pasa a `.bak` y recién ahí el temporal toma el nombre real. En cualquier
 *    momento queda una versión entera: la nueva, o la anterior en el `.bak`.
 *  - [leerJson]: si el archivo no se puede leer, se aparta con otro nombre (cuarentena,
 *    no se borra) y se usa el `.bak`. Si tampoco hay `.bak` legible, falla con
 *    [DatasetIlegible]: es preferible que la app no guarde a que empiece de cero.
 *  - [candado]: un solo candado para el v1 y el v2, que se leen y reescriben desde la
 *    cámara, desde «Deshacer» y desde «Exportar».
 *
 * Solo java.io y org.json: se testea en la JVM (AlmacenSeguroTest).
 */
object AlmacenSeguro {
    val candado = Any()

    fun respaldo(archivo: File) = File(archivo.parentFile, archivo.name + ".bak")

    private fun temporal(archivo: File) = File(archivo.parentFile, archivo.name + ".tmp")

    fun escribirAtomico(archivo: File, contenido: String) {
        archivo.parentFile?.mkdirs()
        val tmp = temporal(archivo)
        FileOutputStream(tmp).use { salida ->
            salida.write(contenido.toByteArray(Charsets.UTF_8))
            salida.fd.sync()
        }
        if (archivo.exists()) {
            val bak = respaldo(archivo)
            bak.delete()
            // renameTo puede fallar entre sistemas de archivos raros: copiar es más lento
            // pero deja el mismo resultado.
            if (!archivo.renameTo(bak)) archivo.copyTo(bak, overwrite = true)
        }
        if (!tmp.renameTo(archivo)) {
            tmp.copyTo(archivo, overwrite = true)
            tmp.delete()
        }
    }

    /**
     * El JSON del archivo, el de su `.bak` si el archivo está roto o falta, o [nuevo]()
     * si no existe ninguno de los dos. Un archivo roto se renombra a
     * `<nombre>.corrupto-<fecha>` antes de seguir, así queda para revisarlo.
     */
    fun leerJson(archivo: File, nuevo: () -> JSONObject): JSONObject {
        val bak = respaldo(archivo)
        if (!archivo.exists() || archivo.length() == 0L) {
            // Sin archivo pero con .bak: se cortó justo entre el paso a .bak y el renombre.
            return if (bak.exists() && bak.length() > 0L) parsear(bak) ?: nuevo() else nuevo()
        }
        parsear(archivo)?.let { return it }
        val apartado = cuarentena(archivo)
        parsear(bak)?.let { return it }
        throw DatasetIlegible(
            "El dataset no se puede leer y no hay copia buena. Se apartó en ${apartado.name}: " +
                "exportalo desde la PC (adb pull) antes de seguir grabando."
        )
    }

    /** Reemplaza las líneas de un archivo JSON Lines de forma atómica (v2, al deshacer). */
    fun reescribirLineas(archivo: File, lineas: List<String>) =
        escribirAtomico(archivo, lineas.joinToString("") { it + "\n" })

    private fun parsear(f: File): JSONObject? = try {
        if (f.exists() && f.length() > 0L) JSONObject(f.readText()) else null
    } catch (_: JSONException) {
        null
    }

    private fun cuarentena(archivo: File): File {
        val sello = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        var destino = File(archivo.parentFile, "${archivo.name}.corrupto-$sello")
        var i = 1
        while (destino.exists()) destino = File(archivo.parentFile, "${archivo.name}.corrupto-$sello-${i++}")
        if (!archivo.renameTo(destino)) {
            archivo.copyTo(destino, overwrite = false)
            archivo.delete()
        }
        return destino
    }
}
