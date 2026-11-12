package com.example.traductorlsa.ui.camera

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import com.clerk.api.Clerk
import com.example.traductorlsa.dataset.CapturaCruda
import com.example.traductorlsa.dataset.DatasetV1
import com.example.traductorlsa.dataset.DatasetV2
import com.example.traductorlsa.dataset.MetaSesion
import com.example.traductorlsa.dataset.Participante
import com.example.traductorlsa.dataset.SesionGuiada
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/*
 * Lo que la cámara y la pantalla del dataset necesitan del dataset del teléfono:
 * seudónimo, compartir, conteo por seña y el almacén de tomas del CameraViewModel.
 * Salió de ui/CameraScreen.kt (T-A20).
 */

/**
 * Seudonimo de la cuenta activa, o null si no hay sesion.
 *
 * Se lee del StateFlow en el momento de guardar y no por composicion, para que
 * la lambda de captura no se quede con un valor viejo. El id crudo de Clerk no
 * sale de esta funcion: al dataset solo llega el seudonimo.
 */
fun participanteActual(): String? = Participante.idSeudonimo(Clerk.userFlow.value?.id)

/**
 * Comparte el v1 y el v2 juntos: el v2 (landmarks crudos) es el que sirve para el
 * contrato nuevo, y separado del v1 seria facil olvidarlo. Antes de que el v1 salga
 * del telefono se le sacan los ids de cuenta crudos; si no se puede leer, no se comparte.
 */
fun shareDatasetJsonFile(context: Context): Boolean {
    try {
        val v1 = DatasetV1.archivo(context)
        DatasetV1.prepararParaCompartir(v1)
        val archivos = listOf(v1, DatasetV2.archivo(context)).filter { it.exists() && it.length() > 0L }
        if (archivos.isEmpty()) return false
        val uris = ArrayList(archivos.map { FileProvider.getUriForFile(context, "${context.packageName}.provider", it) })
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir dataset"))
        return true
    } catch (e: Exception) {
        Log.e("Dataset", "No se pudo compartir el dataset: ${e.message}")
        return false
    }
}

/** Muestras por seña, ordenadas para mostrar. Con el dataset roto tira [DatasetIlegible]. */
fun loadDatasetCountsByLabel(context: Context): Map<String, Int> =
    DatasetV1.conteoPorEtiqueta(DatasetV1.archivo(context))
        .toList().sortedBy { displayTrainingLabel(it.first) }.toMap()

/**
 * El [AlmacenDeTomas] de verdad: el dataset v1 y v2 del teléfono, el progreso de la
 * sesión guiada en preferencias y las imágenes de assets. Cada toma se escribe en los dos
 * datasets; si el v2 falla, se avisa (el v1 ya quedó guardado).
 */
class AlmacenDelTelefono(context: Context) : AlmacenDeTomas {
    private val context = context.applicationContext

    override fun guardar(etiqueta: String, frames: List<List<Float>>, captura: CapturaCruda?, sesion: MetaSesion?): String? {
        // Se lee al guardar y no antes: si la persona cambió de cuenta, vale la de ahora.
        val participante = participanteActual()
        DatasetV1.agregar(DatasetV1.archivo(context), etiqueta, frames, participante,
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
        if (captura != null && !DatasetV2.guardarMuestra(context, etiqueta, participante, captura, sesion)) {
            return "la toma se guardó en el dataset v1 pero no en el v2 (landmarks crudos)"
        }
        return null
    }

    override fun quitarUltima(): String? =
        DatasetV1.quitarUltima(DatasetV1.archivo(context)).also { if (it != null) DatasetV2.quitarUltima(context) }

    override fun conteoPorSena(): Map<String, Int> = loadDatasetCountsByLabel(context)
    override fun senasOficiales(): List<TrainingWordOption> = loadTrainingWordOptions(context)
    override fun imagenDe(etiqueta: String): String? = findTrainingWordImageInAssets(context, etiqueta)
    override fun guardarProgreso(sesion: SesionGuiada) = ProgresoSesion.guardar(context, sesion)
    override fun indiceGuardado(codigo: String): Int = ProgresoSesion.indice(context, codigo)
    override fun tomasDeCodigo(codigo: String): Int = DatasetV2.contarDeCodigo(context, codigo)
}
