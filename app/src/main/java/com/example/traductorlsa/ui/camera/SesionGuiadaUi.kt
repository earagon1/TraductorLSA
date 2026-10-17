package com.example.traductorlsa.ui.camera

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.traductorlsa.dataset.SesionGuiada
import com.example.traductorlsa.dataset.TomaPlanificada
import com.example.traductorlsa.dataset.descripcionCompletaCondicion
import com.example.traductorlsa.dataset.instruccionCondicion
import com.example.traductorlsa.dataset.leerPlanilla
import com.example.traductorlsa.ui.screens.nombreParaMostrar

/** La planilla de la sesión de captura que viaja en el APK (`tools/planilla_captura.py`). */
const val ASSET_PLANILLA = "captura/planilla.csv"

fun cargarPlanilla(context: Context): List<TomaPlanificada> = try {
    context.assets.open(ASSET_PLANILLA).bufferedReader().use { leerPlanilla(it.readText()) }
} catch (_: Exception) {
    emptyList()
}

/**
 * Por dónde va cada participante, para retomar si la app se cierra a mitad de la
 * sesión. Se guarda en cada avance: perder la posición obliga a buscarla a ojo en la
 * planilla, con la persona esperando.
 */
object ProgresoSesion {
    private const val PREFS = "sesion_guiada"

    fun indice(context: Context, codigo: String): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("indice_$codigo", 0)

    fun guardar(context: Context, sesion: SesionGuiada) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt("indice_${sesion.codigo}", sesion.indice).apply()
    }
}

/**
 * Pantalla fija en vertical mientras se ve la cámara (decisión D-7): el contrato de
 * features asume una sola geometría, y en horizontal la corrección de aspecto comprime
 * el otro eje. Al salir se devuelve la orientación que tenía la Activity.
 */
@Composable
fun FijarVertical() {
    val activity = LocalContext.current.buscarActivity() ?: return
    DisposableEffect(activity) {
        val anterior = activity.requestedOrientation
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose { activity.requestedOrientation = anterior }
    }
}

private fun Context.buscarActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.buscarActivity()
    else -> null
}

/** Qué seña toca, de qué tanda y cuánto falta. Va arriba del visor, debajo de la cabecera. */
@Composable
fun TiraSesionGuiada(sesion: SesionGuiada, onSalir: () -> Unit, modifier: Modifier = Modifier) {
    val toma = sesion.actual
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black.copy(alpha = 0.62f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val donde = toma?.let {
                when (it.bloque) {
                    "A" -> "tanda ${it.tanda}"
                    "N" -> "sin señas"
                    "B" -> "robustez"
                    else -> "bloque ${it.bloque}"
                }
            } ?: "terminada"
            Text(
                "${sesion.codigo} · $donde · ${sesion.hechas}/${sesion.total}",
                color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp,
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onSalir) { Text("Salir", color = Color.White) }
        }
        if (toma != null) {
            Text(
                "Ahora: ${nombreParaMostrar(toma.clase)}",
                color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
            )
            if (toma.indicacion.isNotBlank()) {
                Text("Sin señar: ${toma.indicacion}", color = Color.White.copy(alpha = 0.85f), fontSize = 15.sp)
            }
            instruccionCondicion(toma.condicion)?.let {
                Text("Condición: $it", color = Color(0xFFFFD36B), fontSize = 15.sp)
            }
        }
    }
}

@Composable
fun DialogoElegirParticipante(codigos: List<String>, onElegir: (String) -> Unit, onCancelar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Sesión guiada") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("¿Quién va a grabar? La app va a mostrar qué seña toca y pasa sola a la siguiente.")
                Text(
                    "Revisá que la cuenta iniciada en la app sea la de esa persona: las muestras se atribuyen a la cuenta.",
                    fontSize = 14.sp,
                )
                Row {
                    codigos.forEach { c ->
                        Button(onClick = { onElegir(c) }) { Text(c) }
                        Spacer(Modifier.width(8.dp))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } },
    )
}

@Composable
fun DialogoPausa(sesion: SesionGuiada, onContinuar: () -> Unit) {
    val toma = sesion.actual ?: return
    val condicion = instruccionCondicion(toma.condicion)
    val texto = when {
        toma.bloque == "B" ->
            "Siguiente condición${condicion?.let { ": $it" } ?: ""}.\n\nCómo tiene que quedar:\n" +
                descripcionCompletaCondicion(toma.condicion).joinToString("\n") { "• $it" } +
                "\n\nSe guarda la seña que pide la app, aunque el modelo reconozca otra: no hace falta " +
                "deshacer por eso. Cuando estés listo o lista, tocá Continuar."
        toma.bloque == "A" -> "Descansá unos minutos: levantate, tomá agua. Cuando estés listo o lista, seguimos con la tanda ${toma.tanda}."
        else -> "Descansá unos minutos. Cuando estés listo o lista, seguimos con las tomas de \"nada\"."
    }
    AlertDialog(
        onDismissRequest = {},
        title = { Text(if (toma.bloque == "B") "Cambio de condición" else "Pausa") },
        text = { Text(texto) },
        confirmButton = { Button(onClick = onContinuar) { Text("Continuar") } },
    )
}

@Composable
fun DialogoSesionTerminada(sesion: SesionGuiada, onCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("¡Listo, ${sesion.codigo}!") },
        text = { Text("Se grabaron las ${sesion.total} tomas. Ahora exportá el dataset desde la pantalla Dataset.") },
        confirmButton = { Button(onClick = onCerrar) { Text("Cerrar") } },
    )
}
