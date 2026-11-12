package com.example.traductorlsa.ui.camera

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.traductorlsa.dataset.CapturaCruda
import com.example.traductorlsa.dataset.MetaSesion
import com.example.traductorlsa.dataset.SesionGuiada
import com.example.traductorlsa.dataset.TomaPlanificada
import com.example.traductorlsa.dataset.instruccionCondicion
import com.example.traductorlsa.ml.CLASE_NADA
import com.example.traductorlsa.model.NormPoint
import com.example.traductorlsa.ui.overlay.OverlayData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withContext

enum class CameraScreenMode { TRANSLATE, TRAINING }

/**
 * Lo que la cámara necesita del teléfono: el dataset, el progreso de la sesión guiada y
 * las imágenes. Es una interfaz para que [CameraViewModel] se pruebe en la JVM con un
 * almacén en memoria; la de verdad es [AlmacenDelTelefono]. Todo puede tocar disco: el
 * ViewModel lo llama fuera del hilo de la UI.
 */
interface AlmacenDeTomas {
    /** Guarda la toma en el dataset. Devuelve el motivo si no se pudo, o null. */
    fun guardar(etiqueta: String, frames: List<List<Float>>, captura: CapturaCruda?, sesion: MetaSesion?): String?
    /** Saca la última toma y devuelve su etiqueta (null si no había). Tira si no pudo. */
    fun quitarUltima(): String?
    /** Tomas por seña. Tira si el dataset no se puede leer. */
    fun conteoPorSena(): Map<String, Int>
    fun senasOficiales(): List<TrainingWordOption>
    fun imagenDe(etiqueta: String): String?
    fun guardarProgreso(sesion: SesionGuiada)
    fun indiceGuardado(codigo: String): Int
    fun tomasDeCodigo(codigo: String): Int
}

/**
 * El estado de la cámara y lo que hace con cada evento del pipeline (T-A20, segunda
 * etapa). Antes todo esto eran ~30 variables y lambdas dentro de CameraScreen, que no se
 * podían probar sin un teléfono; acá cada caso tiene su test (CameraViewModelTest).
 *
 * Los recursos nativos (cámara, MediaPipe, TFLite) no viven acá sino en
 * [PipelineDeCamara]: tienen que cerrarse cuando la pantalla sale, en el hilo de la cámara.
 *
 * Los eventos del pipeline llegan desde el hilo de la cámara, igual que antes: el estado
 * es de Compose (mutableStateOf), que tolera escrituras desde otro hilo.
 */
class CameraViewModel(
    val modo: CameraScreenMode,
    private val almacen: AlmacenDeTomas,
    private val planilla: List<TomaPlanificada>,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    val entrenando: Boolean get() = modo == CameraScreenMode.TRAINING

    // ---- Traducción ----
    /** Las últimas señas reconocidas, en orden. Es lo que muestra el pie y repite la voz. */
    var frase by mutableStateOf(listOf<String>())
        private set
    var ultimaLectura by mutableStateOf<UltimaLectura>(UltimaLectura.Ninguna)
        private set
    /** La captura terminó y la app espera que se bajen las manos para leer la siguiente. */
    var esperandoBajarManos by mutableStateOf(false)
        private set

    // ---- Manos (el overlay y el encuadre) ----
    var overlay by mutableStateOf(OverlayData())
        private set
    private var ultimaManoEn = 0L
    val hayManos: Boolean get() = overlay.hands.isNotEmpty()

    fun alVerManos(manos: List<List<NormPoint>>, ancho: Int, alto: Int, rotacion: Int, frontal: Boolean, ahoraMs: Long) {
        ultimaManoEn = ahoraMs
        overlay = OverlayData(ancho, alto, manos, rotacion, frontal)
    }

    /** El motor avisa solo cuando hay manos: si hace 600 ms que no llegan, se borran. */
    fun olvidarManosViejas(ahoraMs: Long) {
        if (ahoraMs - ultimaManoEn > 600 && overlay.hands.isNotEmpty()) overlay = overlay.copy(hands = emptyList())
    }

    // ---- Captura en curso (los dos modos) ----
    var frameCount by mutableStateOf(0)
        private set
    // GestureEngine ajusta su objetivo de cuadros según los fps que mide (10, 15 o 20),
    // así que la barra sigue ese número y no uno fijo.
    var frameTarget by mutableStateOf(15)
        private set

    // ---- Entrenamiento ----
    val oficiales = mutableStateListOf<TrainingWordOption>()
    val propias = mutableStateListOf<TrainingWordOption>()
    var senaElegida by mutableStateOf<TrainingWordOption?>(null)
        private set
    // Se lee una vez al entrar y después se ajusta en memoria: releer el JSON en cada
    // captura sería leer el dataset entero varias veces por minuto.
    var conteoPorSena by mutableStateOf<Map<String, Int>>(emptyMap())
        private set
    var recienGuardada by mutableStateOf(false)
        private set
    // Después de guardar: false mientras se espera que bajen las manos, true cuando ya se
    // puede hacer la siguiente seña (lo avisa GestureEngine.onListoParaCapturar).
    var listoParaSiguiente by mutableStateOf(false)
        private set
    var mostrarSelector by mutableStateOf(false)
    var mostrarReferencia by mutableStateOf(false)

    // ---- Sesión guiada (protocolo de captura, docs/captura/) ----
    val hayPlanilla: Boolean get() = planilla.isNotEmpty()
    val codigos: List<String> get() = SesionGuiada.codigos(planilla)
    var sesion by mutableStateOf<SesionGuiada?>(null)
        private set
    var elegirParticipante by mutableStateOf(false)
    var mostrarPausa by mutableStateOf(false)
    var sesionTerminada by mutableStateOf<SesionGuiada?>(null)

    val todasLasSenas: List<TrainingWordOption> get() = oficiales + propias

    private val canalDeMensajes = Channel<String>(Channel.BUFFERED)
    /** Avisos para el snackbar. */
    val mensajes: Flow<String> = canalDeMensajes.receiveAsFlow()

    private fun avisar(texto: String) {
        canalDeMensajes.trySend(texto)
    }

    /** Catálogo de señas y, en entrenamiento, cuántas tomas tiene cada una. */
    suspend fun cargar() {
        val senas = withContext(io) { almacen.senasOficiales() }
        oficiales.clear()
        oficiales += senas
        if (entrenando) {
            conteoPorSena = try {
                withContext(io) { almacen.conteoPorSena() }
            } catch (e: Exception) {
                // Con el dataset roto, avisar ya: cada captura va a fallar hasta resolverlo.
                avisar(e.message ?: "No se pudo leer el dataset")
                emptyMap()
            }
        }
    }

    /**
     * Llegando desde el dataset la seña ya viene elegida: se resuelve en cuanto termina de
     * cargar el catálogo y el selector no llega a abrirse. Si no está en words.json es una
     * seña propia.
     */
    fun resolverSenaInicial(senaInicial: String?) {
        if (!entrenando || senaInicial == null || senaElegida != null || oficiales.isEmpty()) return
        senaElegida = oficiales.firstOrNull { it.label.equals(senaInicial, ignoreCase = true) }
            ?: TrainingWordOption(senaInicial, almacen.imagenDe(senaInicial), isOfficial = false).also { propias += it }
    }

    /** En entrenamiento, sin seña elegida ni nada abierto, se abre el selector. */
    fun abrirSelectorSiHaceFalta(senaInicial: String?) {
        if (entrenando && senaElegida == null && senaInicial == null && sesion == null &&
            !elegirParticipante && sesionTerminada == null
        ) mostrarSelector = true
    }

    fun elegirSena(opcion: TrainingWordOption) {
        senaElegida = opcion
        mostrarSelector = false
    }

    /** Una seña nueva escrita a mano. Si el nombre no sirve, avisa y no cambia nada. */
    fun crearSena(nombre: String) {
        val normalizado = normalizeTrainingLabel(nombre)
        if (normalizado.isBlank()) {
            avisar("Escribí un nombre válido para la seña.")
            return
        }
        senaElegida = todasLasSenas.firstOrNull { it.label.equals(normalizado, ignoreCase = true) }
            ?: TrainingWordOption(normalizado, imageAssetPath = null, isOfficial = false).also { propias += it }
        mostrarSelector = false
    }

    // ---- Eventos del pipeline ----

    /**
     * Una seña clasificada en el modo traducción. Devuelve la seña a decir en voz alta, o
     * null: la voz dice exactamente lo que la pantalla agrega, nunca "Unknown", una
     * predicción dudosa ni la misma palabra repetida en cada ciclo.
     */
    fun alPredecir(sena: String, confianza: Float, confianzaMinima: Float): String? {
        if (entrenando) return null
        val sinNombre = sena == "Unknown" || sena == "Sin datos"
        esperandoBajarManos = true
        return when {
            // "nada" no se dice ni se escribe, pero se avisa en el pie: también sale cuando
            // una seña real no se entendió, y callarse dejaba a la persona sin saber que
            // tenía que repetir.
            sena == CLASE_NADA || sinNombre -> {
                ultimaLectura = UltimaLectura.SinSena
                null
            }
            confianza >= confianzaMinima -> {
                ultimaLectura = UltimaLectura.Reconocida
                if (frase.lastOrNull() == sena) null
                else {
                    frase = (frase + sena).takeLast(6)
                    sena
                }
            }
            else -> {
                ultimaLectura = UltimaLectura.Dudosa(confianza)
                null
            }
        }
    }

    fun alProgresoDeCaptura(cuadros: Int, objetivo: Int) {
        frameCount = cuadros
        if (objetivo > 0) frameTarget = objetivo
        if (cuadros > 0) {
            recienGuardada = false
            ultimaLectura = UltimaLectura.Ninguna
            esperandoBajarManos = false
        }
    }

    fun alListoParaCapturar() {
        listoParaSiguiente = true
        esperandoBajarManos = false
    }

    fun limpiarFrase() {
        frase = emptyList()
        ultimaLectura = UltimaLectura.Ninguna
    }

    /**
     * Guarda la toma recién capturada en el modo entrenamiento. Si no se pudo guardar, la
     * sesión no avanza ni se cuenta la muestra: antes avanzaba igual y la toma se perdía
     * sin que nadie se enterara (auditoría, TL-C5).
     *
     * [captura] se toma de GestureEngine apenas termina la captura, antes de llamar acá:
     * la siguiente la reemplaza.
     */
    suspend fun guardarToma(secuencia: List<FloatArray>, captura: CapturaCruda?) {
        // Durante la pausa la cámara sigue viendo manos: no se guarda nada hasta que la
        // persona toque Continuar.
        if (!entrenando || mostrarPausa) return
        val elegida = senaElegida ?: return
        if (secuencia.isEmpty()) return
        val sesionActual = sesion
        val error = withContext(io) {
            try {
                almacen.guardar(elegida.label, secuencia.map { it.toList() }, captura, sesionActual?.meta())
            } catch (e: Exception) {
                e.message ?: e.javaClass.simpleName
            }
        }
        if (error != null) {
            avisar("No se pudo guardar la toma: $error")
            return
        }
        if (sesionActual != null) irA(sesionActual.avanzar())
        conteoPorSena = conteoPorSena + (elegida.label to (conteoPorSena[elegida.label] ?: 0) + 1)
        // La confirmación la da el panel, que queda en pantalla hasta la captura siguiente.
        recienGuardada = true
        listoParaSiguiente = false
    }

    /** Saca la última toma. En la sesión guiada, vuelve a esa toma para repetirla. */
    suspend fun deshacer() {
        val quitada = try {
            withContext(io) { almacen.quitarUltima() }
        } catch (e: Exception) {
            avisar("No se pudo deshacer: ${e.message ?: e.javaClass.simpleName}")
            null
        } ?: return
        sesion?.let {
            irA(it.retroceder())
            mostrarPausa = false
        }
        conteoPorSena = conteoPorSena + (quitada to ((conteoPorSena[quitada] ?: 1) - 1).coerceAtLeast(0))
        recienGuardada = false
    }

    // ---- Sesión guiada ----

    fun abrirSesionGuiada() {
        elegirParticipante = true
        mostrarSelector = false
    }

    /** Arranca (o retoma) la sesión de [codigo]. */
    suspend fun elegirCodigo(codigo: String) {
        elegirParticipante = false
        mostrarSelector = false
        // Retoma donde quedó esa persona si la app se cerró a mitad de sesión.
        var s = SesionGuiada.de(planilla, codigo, withContext(io) { almacen.indiceGuardado(codigo) })
        // Si la app perdió la posición (reinstalada, datos borrados) pero esa persona ya
        // grabó la sesión 1 completa, se arranca en la robustez en vez de hacerle repetir
        // las 106 tomas.
        val inicioB = s.primeraDelBloque("B")
        if (s.indice == 0 && inicioB != null && withContext(io) { almacen.tomasDeCodigo(codigo) } >= inicioB) {
            s = s.irA(inicioB)
        }
        irA(s)
        // Al retomar en una condición de robustez, el cartel dice qué cambiar.
        mostrarPausa = s.actual?.let { instruccionCondicion(it.condicion) != null } ?: false
    }

    fun salirDeSesion() {
        sesion = null
        senaElegida = null
        mostrarPausa = false
    }

    /** Mueve la sesión y deja elegida la seña que toca, con la pausa si corresponde. */
    private fun irA(nueva: SesionGuiada) {
        sesion = nueva
        almacen.guardarProgreso(nueva)
        val toma = nueva.actual
        if (toma == null) {
            sesionTerminada = nueva
            sesion = null
            senaElegida = null
            return
        }
        senaElegida = opcionPara(toma.clase)
        mostrarPausa = nueva.pausaAntes
    }

    /** La seña de la planilla como opción del selector; "nada" no está en words.json. */
    private fun opcionPara(clase: String): TrainingWordOption =
        todasLasSenas.firstOrNull { it.label == clase }
            ?: TrainingWordOption(clase, almacen.imagenDe(clase), isOfficial = false).also { propias += it }
}
