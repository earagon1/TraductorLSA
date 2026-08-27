package com.example.traductorlsa.settings

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Resolución con la que se analizan los frames.
 *
 * Es el ajuste que más se nota: el README ya midió que el cuello de botella
 * está en la captura y no en la inferencia, así que bajar la resolución sube
 * los cuadros por segundo en dispositivos lentos.
 */
enum class CalidadAnalisis(val etiqueta: String, val ladoCorto: Int, val ladoLargo: Int) {
    FLUIDEZ("Fluidez", 360, 480),
    EQUILIBRIO("Equilibrio", 480, 640),
    DETALLE("Detalle", 720, 960);

    val descripcion: String get() = "$ladoCorto × $ladoLargo"
}

/**
 * Variante del español. Vale para las dos puntas: para leer en voz alta y
 * para reconocer lo que dice la otra persona.
 */
enum class VarianteEspanol(val etiqueta: String, private val pais: String) {
    ARGENTINA("Argentina", "AR"),
    MEXICO("México", "MX"),
    NEUTRO("Neutro", "");

    val locale: Locale get() = if (pais.isEmpty()) Locale("es") else Locale("es", pais)
    val etiquetaBcp47: String get() = if (pais.isEmpty()) "es" else "es-$pais"
}

data class AjustesSenar(
    val calidad: CalidadAnalisis = CalidadAnalisis.EQUILIBRIO,
    val camaraFrontal: Boolean = true,
    val sensibilidadDeteccion: Float = 0.6f,
    val mostrarLandmarks: Boolean = true,
    val leerEnVozAlta: Boolean = true,
    val velocidadVoz: Float = 1.0f,
    val tonoVoz: Float = 1.0f,
    val variante: VarianteEspanol = VarianteEspanol.ARGENTINA,
    val confianzaMinima: Float = 0.7f,
)

/**
 * Guarda los ajustes y los publica como flujo para que las pantallas y el
 * pipeline reaccionen al toque.
 *
 * Usa SharedPreferences a propósito: son nueve valores sueltos y viene con la
 * plataforma. Si algún día hacen falta migraciones o escrituras concurrentes,
 * se cambia por DataStore sin tocar a quien lo consume.
 */
class RepositorioAjustes private constructor(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("ajustes_senar", Context.MODE_PRIVATE)

    private val _estado = MutableStateFlow(leer())
    val estado: StateFlow<AjustesSenar> = _estado.asStateFlow()

    private fun leer() = AjustesSenar(
        calidad = enumPorNombre(prefs.getString(CALIDAD, null), CalidadAnalisis.EQUILIBRIO),
        camaraFrontal = prefs.getBoolean(CAMARA_FRONTAL, true),
        sensibilidadDeteccion = prefs.getFloat(SENSIBILIDAD, 0.6f),
        mostrarLandmarks = prefs.getBoolean(LANDMARKS, true),
        leerEnVozAlta = prefs.getBoolean(VOZ_ALTA, true),
        velocidadVoz = prefs.getFloat(VELOCIDAD, 1.0f),
        tonoVoz = prefs.getFloat(TONO, 1.0f),
        variante = enumPorNombre(prefs.getString(VARIANTE, null), VarianteEspanol.ARGENTINA),
        confianzaMinima = prefs.getFloat(CONFIANZA, 0.7f),
    )

    fun actualizar(cambio: (AjustesSenar) -> AjustesSenar) {
        val nuevo = cambio(_estado.value)
        prefs.edit()
            .putString(CALIDAD, nuevo.calidad.name)
            .putBoolean(CAMARA_FRONTAL, nuevo.camaraFrontal)
            .putFloat(SENSIBILIDAD, nuevo.sensibilidadDeteccion)
            .putBoolean(LANDMARKS, nuevo.mostrarLandmarks)
            .putBoolean(VOZ_ALTA, nuevo.leerEnVozAlta)
            .putFloat(VELOCIDAD, nuevo.velocidadVoz)
            .putFloat(TONO, nuevo.tonoVoz)
            .putString(VARIANTE, nuevo.variante.name)
            .putFloat(CONFIANZA, nuevo.confianzaMinima)
            .apply()
        _estado.value = nuevo
    }

    fun restaurar() = actualizar { AjustesSenar() }

    companion object {
        private const val CALIDAD = "calidad"
        private const val CAMARA_FRONTAL = "camara_frontal"
        private const val SENSIBILIDAD = "sensibilidad"
        private const val LANDMARKS = "landmarks"
        private const val VOZ_ALTA = "voz_alta"
        private const val VELOCIDAD = "velocidad"
        private const val TONO = "tono"
        private const val VARIANTE = "variante"
        private const val CONFIANZA = "confianza"

        @Volatile
        private var instancia: RepositorioAjustes? = null

        fun de(context: Context): RepositorioAjustes =
            instancia ?: synchronized(this) {
                instancia ?: RepositorioAjustes(context).also { instancia = it }
            }

        /** Si el valor guardado ya no existe (por ejemplo tras un rename), vuelve al de fábrica. */
        private inline fun <reified T : Enum<T>> enumPorNombre(nombre: String?, porDefecto: T): T =
            enumValues<T>().firstOrNull { it.name == nombre } ?: porDefecto
    }
}

/** Los ajustes vivos, para leerlos desde cualquier pantalla. */
@Composable
fun ajustesSenar(): State<AjustesSenar> {
    val context = LocalContext.current
    val repo = remember(context) { RepositorioAjustes.de(context) }
    return repo.estado.collectAsStateWithLifecycle()
}

/** El repositorio, para escribir. */
@Composable
fun repositorioAjustes(): RepositorioAjustes {
    val context = LocalContext.current
    return remember(context) { RepositorioAjustes.de(context) }
}
