package com.example.traductorlsa.ui.screens

import android.content.Context
import android.content.SharedPreferences
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
import com.example.traductorlsa.federado.Base64Simple
import com.example.traductorlsa.federado.ClienteFederado
import com.example.traductorlsa.federado.ModeloInstalado
import com.example.traductorlsa.federado.OperacionesFederadas
import com.example.traductorlsa.federado.ResultadoFederado
import com.example.traductorlsa.federado.Seguridad
import com.example.traductorlsa.federado.VerificadorDeHuella
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
    var codigo by remember { mutableStateOf(prefs.getString("codigo", "") ?: "") }
    // La huella confirmada para la dirección escrita; null hasta confirmarla.
    var huella by remember(direccion) { mutableStateOf(huellaGuardada(prefs, direccion)) }
    var huellaPorConfirmar by remember { mutableStateOf<ByteArray?>(null) }
    var acepta by remember { mutableStateOf(prefs.getBoolean("consentimiento", false)) }
    var ocupado by remember { mutableStateOf(false) }
    var progreso by remember { mutableStateOf("") }
    var modelo by remember { mutableStateOf(descripcionModelo(context)) }
    val registro = remember { mutableStateListOf<String>() }
    val scope = rememberCoroutineScope()
    val participante = participanteActual()

    fun correr(nombre: String, conServidor: Boolean = true, accion: (ClienteFederado?) -> ResultadoFederado) {
        val h = huella
        if (conServidor) {
            if (direccion.isBlank()) { registro.add(0, "Escribí la dirección del servidor"); return }
            if (h == null) { registro.add(0, "Primero tocá «Probar conexión» y confirmá la huella del servidor"); return }
            prefs.edit().putString("servidor", direccion.trim()).apply()
        }
        ocupado = true
        progreso = "$nombre…"
        scope.launch {
            val texto = withContext(Dispatchers.IO) {
                try {
                    val r = accion(if (conServidor && h != null) ClienteFederado(direccion, h, codigo) else null)
                    guardarEnLog(context, nombre, r)
                    r.resumen
                } catch (e: Exception) {
                    mensajeDeError(e)
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
                    value = direccion, onValueChange = { direccion = it; huellaPorConfirmar = null },
                    label = { Text("Dirección (la muestra la PC, ej. 192.168.0.10:8765)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !ocupado,
                )
                Aire(10)
                OutlinedButton(onClick = {
                    if (huella == null) {
                        // Primera vez con esta dirección: solo el handshake, para ver la huella.
                        ocupado = true
                        progreso = "Buscando el servidor…"
                        scope.launch {
                            val r = withContext(Dispatchers.IO) { runCatching { ClienteFederado.sondearHuella(direccion) } }
                            r.onSuccess { huellaPorConfirmar = it }
                                .onFailure { registro.add(0, "Probar conexión: ${mensajeDeError(it)}") }
                            ocupado = false
                            progreso = ""
                        }
                    } else {
                        correr("Probar conexión") { c ->
                            val e = (c ?: error("sin servidor")).estado()
                            ResultadoFederado("conectado. Modelo v${e.optInt("paquete")}, ronda ${e.optInt("ronda")} " +
                                "(${e.optInt("recibidos")} de ${e.optInt("quorum")} ajustes)", e)
                        }
                    }
                }, enabled = !ocupado) { Text("Probar conexión") }
                huellaPorConfirmar?.let { vista ->
                    Aire(10)
                    Text("Huella del servidor: ${Seguridad.huellaParaMostrar(vista)}",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = SenarGrafito900)
                    AyudaAjuste("Tiene que ser igual a la que muestra la PC al arrancar el servidor. Si no coincide, " +
                        "no la confirmes: podría ser otra computadora haciéndose pasar por el servidor.")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            guardarHuella(prefs, direccion, vista)
                            huella = vista
                            huellaPorConfirmar = null
                            registro.add(0, "Confirmaste la huella del servidor. Tocá «Probar conexión» de nuevo")
                        }) { Text("Es la misma") }
                        OutlinedButton(onClick = { huellaPorConfirmar = null }) { Text("No coincide") }
                    }
                }
                if (huella != null) {
                    TextButton(onClick = {
                        olvidarHuella(prefs, direccion)
                        huella = null
                        registro.add(0, "Olvidaste la huella: la próxima conexión la vas a tener que confirmar")
                    }, enabled = !ocupado) { Text("Olvidar la huella del servidor") }
                }
                Aire(6)
                OutlinedTextField(
                    value = codigo,
                    onValueChange = { codigo = it; prefs.edit().putString("codigo", it.trim()).apply() },
                    label = { Text("Código de participación (te lo da la PC)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = !ocupado,
                    isError = codigo.isNotBlank() && !Seguridad.codigoBienFormado(codigo),
                )
                if (codigo.isNotBlank() && !Seguridad.codigoBienFormado(codigo)) {
                    AyudaAjuste("Son 16 letras y números, como ABCD-EFGH-JKMN-PQRS.")
                }
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
                } else {
                    Text("Tu código seudónimo: $participante", style = MaterialTheme.typography.bodyMedium,
                        color = SenarGrafito900)
                    AyudaAjuste("Pasáselo a quien maneja el servidor para que te dé de alta: a cambio te da el " +
                        "código de participación.")
                }
                val puede = acepta && participante != null && Seguridad.codigoBienFormado(codigo) && !ocupado
                Button(onClick = {
                    correr("Buscar actualización") { c ->
                        val r = OperacionesFederadas.actualizar(context, c ?: error("sin servidor"))
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
                        OperacionesFederadas.participar(context, c ?: error("sin servidor"),
                            participante ?: error("sin sesión")) { p -> progreso = p }
                    }
                }, enabled = puede, modifier = Modifier.fillMaxWidth()) { Text("Entrenar en esta ronda") }
                OutlinedButton(onClick = {
                    correr("Aportar mis tomas") { c ->
                        OperacionesFederadas.aportar(context, c ?: error("sin servidor"), participante ?: error("sin sesión"))
                    }
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

// La huella se guarda por dirección: otra PC (u otra dirección) se confirma de nuevo.
private fun claveHuella(direccion: String): String? =
    runCatching { "huella:" + ClienteFederado.normalizarDireccion(direccion) }.getOrNull()

private fun huellaGuardada(prefs: SharedPreferences, direccion: String): ByteArray? =
    claveHuella(direccion)?.let { prefs.getString(it, null) }?.let { runCatching { Base64Simple.decodificar(it) }.getOrNull() }

private fun guardarHuella(prefs: SharedPreferences, direccion: String, h: ByteArray) {
    claveHuella(direccion)?.let { prefs.edit().putString(it, Base64Simple.codificar(h)).apply() }
}

private fun olvidarHuella(prefs: SharedPreferences, direccion: String) {
    claveHuella(direccion)?.let { prefs.edit().remove(it).apply() }
}

/** Lo que vio la persona cuando algo falló, en palabras: la causa de TLS viene envuelta. */
internal fun mensajeDeError(e: Throwable): String {
    val causas = generateSequence(e) { it.cause }.mapNotNull { it.message }.toList()
    return when {
        causas.any { VerificadorDeHuella.HUELLA_DISTINTA in it } ->
            "la huella del servidor no es la que confirmaste. Si regeneraste el certificado de la PC, tocá " +
                "«Olvidar la huella del servidor» y confirmá la nueva; si no, no sigas: puede ser otra computadora"
        e is ClienteFederado.ErrorServidor && e.codigo == 401 ->
            "el servidor no reconoce tu código de participación. Revisalo o pedí el alta de nuevo"
        e is ClienteFederado.ErrorServidor && e.codigo == 403 ->
            "tu código de participación es de otra persona"
        e is IllegalArgumentException -> e.message ?: "dirección inválida"
        else -> "No se pudo: ${e.message ?: e.javaClass.simpleName}. ¿La PC y el teléfono están en la misma Wi-Fi?"
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
