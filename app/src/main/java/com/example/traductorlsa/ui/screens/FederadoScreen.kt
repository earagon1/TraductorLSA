package com.example.traductorlsa.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.traductorlsa.federado.ClienteFederado
import com.example.traductorlsa.federado.ModeloInstalado
import com.example.traductorlsa.federado.OperacionesFederadas
import com.example.traductorlsa.federado.ResultadoFederado
import com.example.traductorlsa.ui.components.Aire
import com.example.traductorlsa.ui.components.AyudaAjuste
import com.example.traductorlsa.ui.components.PantallaSenar
import com.example.traductorlsa.ui.components.SeccionAjustes
import com.example.traductorlsa.ui.components.TituloDePagina
import com.example.traductorlsa.ui.participanteActual
import com.example.traductorlsa.ui.theme.SenarGrafito500
import com.example.traductorlsa.ui.theme.SenarGrafito900
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val PREFS = "federado"

/**
 * Aprendizaje federado con el servidor de la red local (plan/federado_completo.md).
 *
 * Traducir nunca usa la red: esta pantalla es el único lugar donde la app habla con el
 * servidor, y solo cuando la persona toca un botón. Antes de participar o aportar hay
 * que aceptar qué sale del teléfono, en palabras simples.
 */
@Composable
fun FederadoScreen(navController: NavHostController) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var direccion by remember { mutableStateOf(prefs.getString("servidor", "") ?: "") }
    var acepta by remember { mutableStateOf(prefs.getBoolean("consentimiento", false)) }
    var ocupado by remember { mutableStateOf(false) }
    var progreso by remember { mutableStateOf("") }
    var modelo by remember { mutableStateOf(descripcionModelo(context)) }
    val registro = remember { mutableStateListOf<String>() }
    val scope = rememberCoroutineScope()
    val participante = participanteActual()

    fun correr(nombre: String, conServidor: Boolean = true, accion: (ClienteFederado) -> ResultadoFederado) {
        if (conServidor && direccion.isBlank()) { registro.add(0, "Escribí la dirección del servidor"); return }
        if (conServidor) prefs.edit().putString("servidor", direccion.trim()).apply()
        ocupado = true
        progreso = "$nombre…"
        scope.launch {
            val texto = withContext(Dispatchers.IO) {
                try {
                    val r = accion(ClienteFederado(direccion.ifBlank { "127.0.0.1" }))
                    guardarEnLog(context, nombre, r)
                    r.resumen
                } catch (e: Exception) {
                    "No se pudo: ${e.message ?: e.javaClass.simpleName}. ¿La PC y el teléfono están en la misma Wi-Fi?"
                }
            }
            registro.add(0, "${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())} · $nombre: $texto")
            modelo = descripcionModelo(context)
            ocupado = false
            progreso = ""
        }
    }

    PantallaSenar(onAvatar = { }) {
        TituloDePagina(texto = "Aprendizaje federado", onVolver = { navController.popBackStack() })

        SeccionAjustes("MODELO CON EL QUE TRADUCÍS") {
            Column(Modifier.padding(16.dp)) {
                Text(modelo, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = SenarGrafito900)
                Aire(6)
                AyudaAjuste("Traducir funciona siempre sin internet, con el modelo que tengas instalado.")
                if (ModeloInstalado.activo(context) != null) {
                    TextButton(onClick = {
                        ModeloInstalado.volverAFabrica(context)
                        modelo = descripcionModelo(context)
                        registro.add(0, "Volviste al modelo de fábrica")
                    }, enabled = !ocupado) { Text("Volver al modelo de fábrica") }
                }
                if (ModeloInstalado.activo(context)?.tienePersonal == true) {
                    TextButton(onClick = {
                        ModeloInstalado.activo(context)?.quitarPersonal()
                        ModeloInstalado.guardarQuierePersonalizar(context, false)
                        modelo = descripcionModelo(context)
                        registro.add(0, "Quitaste la personalización: traducís con el modelo compartido")
                    }, enabled = !ocupado) { Text("Quitar la personalización") }
                }
            }
        }

        SeccionAjustes("SERVIDOR") {
            Column(Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = direccion, onValueChange = { direccion = it },
                    label = { Text("Dirección (la muestra la PC, ej. 192.168.0.10:8765)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !ocupado,
                )
                Aire(10)
                OutlinedButton(onClick = {
                    correr("Probar conexión") { c ->
                        val e = c.estado()
                        ResultadoFederado("conectado. Modelo v${e.optInt("paquete")}, ronda ${e.optInt("ronda")} " +
                            "(${e.optInt("recibidos")} de ${e.optInt("quorum")} ajustes)", e)
                    }
                }, enabled = !ocupado) { Text("Probar conexión") }
            }
        }

        SeccionAjustes("PARTICIPAR") {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = acepta, onCheckedChange = {
                        acepta = it
                        prefs.edit().putBoolean("consentimiento", it).apply()
                    }, enabled = !ocupado)
                    Text("Acepto participar. Al entrenar sale del teléfono solo un ajuste del modelo, sin mis tomas. " +
                        "Si toco «Aportar mis tomas», también salen los puntos de mis manos (nunca imágenes), con un " +
                        "código que no dice quién soy.", style = MaterialTheme.typography.bodySmall, color = SenarGrafito500)
                }
                if (participante == null) {
                    AyudaAjuste("Para participar tenés que iniciar sesión: las tomas se asocian a tu código seudónimo.")
                }
                val puede = acepta && participante != null && !ocupado
                Button(onClick = {
                    correr("Buscar actualización") { c ->
                        val r = OperacionesFederadas.actualizar(context, c)
                        // Si había elegido personalizar y llegó una cabeza nueva, se rehace sola:
                        // la personal vieja era sobre la versión anterior.
                        val m = ModeloInstalado.activo(context)
                        if (participante != null && m != null && !m.tienePersonal && ModeloInstalado.quierePersonalizar(context)) {
                            val p = OperacionesFederadas.personalizar(context, participante) { t -> progreso = t }
                            ResultadoFederado("${r.resumen}. ${p.resumen}", r.detalle.put("personalizacion", p.detalle))
                        } else r
                    }
                }, enabled = !ocupado, modifier = Modifier.fillMaxWidth()) { Text("Buscar actualización") }
                Button(onClick = {
                    correr("Entrenar en esta ronda") { c ->
                        OperacionesFederadas.participar(context, c, participante ?: error("sin sesión")) { p -> progreso = p }
                    }
                }, enabled = puede, modifier = Modifier.fillMaxWidth()) { Text("Entrenar en esta ronda") }
                OutlinedButton(onClick = {
                    correr("Aportar mis tomas") { c -> OperacionesFederadas.aportar(context, c, participante ?: error("sin sesión")) }
                }, enabled = puede, modifier = Modifier.fillMaxWidth()) { Text("Aportar mis tomas") }
                OutlinedButton(onClick = {
                    correr("Personalizar", conServidor = false) { _ ->
                        OperacionesFederadas.personalizar(context, participante ?: error("sin sesión")) { p -> progreso = p }
                    }
                }, enabled = participante != null && !ocupado, modifier = Modifier.fillMaxWidth()) {
                    Text("Personalizar con mis tomas")
                }
                AyudaAjuste("Personalizar ajusta el modelo a tu forma de señar, solo en este teléfono: no se envía nada.")
                if (progreso.isNotEmpty()) Text(progreso, style = MaterialTheme.typography.bodySmall, color = SenarGrafito500)
            }
        }

        if (registro.isNotEmpty()) {
            SeccionAjustes("LO ÚLTIMO") {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    registro.take(8).forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = SenarGrafito900) }
                }
            }
        }
        Aire(24)
    }
}

private fun descripcionModelo(context: Context): String {
    val m = ModeloInstalado.activo(context) ?: return "Modelo de fábrica"
    val v = ModeloInstalado.versionCabeza(context)
    return "Modelo v${m.paquete} del servidor${if (v > 0) ", ajuste de las rondas v$v" else ""}" +
        (if (m.tienePersonal) ", personalizado con tus tomas" else "") + " · ${m.clases.count { it != "nada" }} señas"
}

/** Una línea JSON por operación en files/federado/operaciones.jsonl, para el informe del piloto. */
private fun guardarEnLog(context: Context, nombre: String, r: ResultadoFederado) {
    val dir = File(context.filesDir, "federado").apply { mkdirs() }
    File(dir, "operaciones.jsonl").appendText(
        org.json.JSONObject().put("t_ms", System.currentTimeMillis()).put("operacion", nombre)
            .put("resumen", r.resumen).put("detalle", r.detalle).toString() + "\n")
}
