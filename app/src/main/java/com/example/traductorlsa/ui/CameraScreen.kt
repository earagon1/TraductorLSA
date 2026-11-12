package com.example.traductorlsa.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.util.Log
import android.os.SystemClock
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageAnalysis
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import com.example.traductorlsa.camera.CameraManager
import com.example.traductorlsa.camera.FrameAnalyzer
import com.example.traductorlsa.dataset.DatasetV1
import com.example.traductorlsa.dataset.DatasetV2
import com.example.traductorlsa.dataset.Participante
import com.example.traductorlsa.dataset.SesionGuiada
import com.example.traductorlsa.dataset.instruccionCondicion
import com.example.traductorlsa.ui.camera.DialogoElegirParticipante
import com.example.traductorlsa.ui.camera.DialogoPausa
import com.example.traductorlsa.ui.camera.DialogoSesionTerminada
import com.example.traductorlsa.ui.camera.EstadoTraduccion
import com.example.traductorlsa.ui.camera.FijarVertical
import com.example.traductorlsa.ui.camera.MantenerPantallaEncendida
import com.example.traductorlsa.ui.camera.ProgresoSesion
import com.example.traductorlsa.ui.camera.TiraSesionGuiada
import com.example.traductorlsa.ui.camera.UltimaLectura
import com.example.traductorlsa.ui.camera.cargarPlanilla
import com.example.traductorlsa.ui.camera.estadoTraduccion
import com.example.traductorlsa.detection.TrackerPerezoso
import com.example.traductorlsa.detection.trackerDesdeConfig
import com.example.traductorlsa.settings.ajustesSenar
import com.example.traductorlsa.settings.repositorioAjustes
import com.example.traductorlsa.features.FeatureBuilderImpl
import com.example.traductorlsa.ml.CLASE_NADA
import com.example.traductorlsa.ml.GestureEngine
import com.example.traductorlsa.ml.LabelProviderImpl
import com.example.traductorlsa.ml.Metricas
import com.example.traductorlsa.ml.TFLiteClassifier
import com.example.traductorlsa.model.NormPoint
import com.example.traductorlsa.model.PredictionResult
import com.example.traductorlsa.speech.SpeechManager
import com.example.traductorlsa.ui.screens.DictionaryAssetImage
import com.example.traductorlsa.ui.screens.nombreParaMostrar
import com.example.traductorlsa.ui.camera.AvisoModeloInvalido
import com.example.traductorlsa.ui.overlay.HandLandmarksOverlay
import com.example.traductorlsa.ui.theme.SenarAzul300
import com.example.traductorlsa.ui.theme.SenarAzul600
import com.example.traductorlsa.ui.theme.SenarBlanco
import com.example.traductorlsa.ui.theme.SenarGrafito500
import com.example.traductorlsa.ui.theme.SenarGrafito900
import com.example.traductorlsa.ui.theme.SenarAzul100
import com.example.traductorlsa.ui.theme.SenarAzul700
import com.example.traductorlsa.ui.theme.SenarBorde
import com.example.traductorlsa.ui.theme.SenarBordeSuave
import com.example.traductorlsa.ui.theme.SenarGrafito300
import com.example.traductorlsa.ui.theme.SenarPapel
import com.example.traductorlsa.ui.theme.SenarPista
import com.example.traductorlsa.ui.theme.SenarAmbar
import com.example.traductorlsa.ui.theme.SenarAzul500
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import com.example.traductorlsa.ui.components.ChipDeFiltro
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.clerk.api.Clerk
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import com.example.traductorlsa.ui.camera.BotonRedondo
import com.example.traductorlsa.ui.camera.CONFIG_MEDIAPIPE
import com.example.traductorlsa.ui.camera.CabeceraCamara
import com.example.traductorlsa.ui.camera.GuiaDeEncuadre
import com.example.traductorlsa.ui.camera.InsigniaDeProgreso
import com.example.traductorlsa.ui.camera.PieDeCaptura
import com.example.traductorlsa.ui.camera.PieDeTraduccion
import com.example.traductorlsa.ui.camera.PildoraDeVoz
import com.example.traductorlsa.ui.camera.ReferenciaFija
import com.example.traductorlsa.ui.camera.TrainingReferenceDialog
import com.example.traductorlsa.ui.camera.TrainingWordOption
import com.example.traductorlsa.ui.camera.TrainingWordPickerSheet
import com.example.traductorlsa.ui.camera.VeloInferior
import com.example.traductorlsa.ui.camera.displayTrainingLabel
import com.example.traductorlsa.ui.camera.findTrainingWordImageInAssets
import com.example.traductorlsa.ui.camera.loadDatasetCountsByLabel
import com.example.traductorlsa.ui.camera.loadTrainingWordOptions
import com.example.traductorlsa.ui.camera.normalizeTrainingLabel
import com.example.traductorlsa.ui.camera.participanteActual
import com.example.traductorlsa.ui.overlay.OverlayData



enum class CameraScreenMode { TRANSLATE, TRAINING }

@Composable
fun TranslateSignScreen(navController: NavHostController) {
    CameraScreen(mode = CameraScreenMode.TRANSLATE, onBack = { navController.popBackStack() })
}

@Composable
fun TrainingCaptureScreen(navController: NavHostController, senaInicial: String? = null) {
    CameraScreen(
        mode = CameraScreenMode.TRAINING,
        onBack = { navController.popBackStack() },
        senaInicial = senaInicial,
    )
}

@Composable
fun CameraScreen(
    mode: CameraScreenMode = CameraScreenMode.TRANSLATE,
    onBack: () -> Unit = {},
    /** Etiqueta a entrenar cuando se llega desde el dataset. Null abre el selector. */
    senaInicial: String? = null,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val trainingMode = mode == CameraScreenMode.TRAINING
    val ajustes by ajustesSenar()
    var isFrontCamera by rememberSaveable { mutableStateOf(ajustes.camaraFrontal) }

    val overlayState = remember { mutableStateOf(OverlayData()) }
    var lastHandsAt by remember { mutableStateOf(0L) }
    var currentTranslation by rememberSaveable { mutableStateOf("") }
    var recentTranslations by rememberSaveable { mutableStateOf(listOf<String>()) }

    // mÃ©tricas
    var captureTime by remember { mutableStateOf(0L) }
    var inferTime by remember { mutableStateOf(0L) }
    var fpsValue by remember { mutableStateOf(0f) }
    var targetFramesUsed by remember { mutableStateOf(15) }

    // ---- TRAINING STATE ----
    val officialWords = remember { mutableStateListOf<TrainingWordOption>() }
    val customWords = remember { mutableStateListOf<TrainingWordOption>() }
    var selectedWord by remember { mutableStateOf<TrainingWordOption?>(null) }
    var sessionSavedCount by remember { mutableStateOf(0) }
    var tUsed by remember { mutableStateOf(0) }
    var dUsed by remember { mutableStateOf(0) }
    var showWordPicker by remember { mutableStateOf(false) }
    var showReference by remember { mutableStateOf(false) }

    // ---- SESIÓN GUIADA (protocolo de captura, docs/captura/) ----
    val planilla = remember { cargarPlanilla(context) }
    var sesion by remember { mutableStateOf<SesionGuiada?>(null) }
    var elegirParticipante by remember { mutableStateOf(false) }
    var mostrarPausa by remember { mutableStateOf(false) }
    var sesionTerminada by remember { mutableStateOf<SesionGuiada?>(null) }

    FijarVertical()
    MantenerPantallaEncendida()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) { if (!hasPermission) launcher.launch(Manifest.permission.CAMERA) }

    val camController = remember { LifecycleCameraController(context) }
    val cameraManager = remember { CameraManager(context, camController) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    // Se pone al salir de la pantalla: el analizador deja pasar los frames que CameraX
    // ya tenía en camino sin procesarlos (ver el cierre más abajo).
    val pantallaCerrada = remember { AtomicBoolean(false) }

    // La sensibilidad se toma al entrar a la pantalla y no en vivo: recrear el
    // detector recarga los 8 MB del modelo, y el deslizador de Ajustes dispara
    // en cada movimiento. Cambiarla tiene efecto la próxima vez que se abre.
    // Banco de medición (solo debug, prefs "banco" escritas desde la PC): con qué
    // delegado corre MediaPipe y, si "comparar" está puesto, otra configuración que corre
    // sobre el mismo frame para medir tiempo y diferencia de landmarks.
    val banco = remember {
        if (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0)
            context.getSharedPreferences("banco", android.content.Context.MODE_PRIVATE) else null
    }
    // MediaPipe en GPU (con vuelta automática a CPU si el teléfono no la tiene): en el
    // Galaxy A55 baja de 99 a 70 ms por frame con los mismos landmarks (misma cantidad de
    // manos en el 99,7 % de los frames, puntos a 0,2 % del ancho; 2026-10-18). Se crea en
    // el hilo de la cámara, que es donde corre.
    val handTracker = remember {
        val config = banco?.getString("principal", null) ?: CONFIG_MEDIAPIPE
        TrackerPerezoso { trackerDesdeConfig(context, ajustes.sensibilidadDeteccion, config) }
    }
    val featureBuilder = remember { FeatureBuilderImpl() }
    val labelProvider = remember { LabelProviderImpl(context) }
    val classifier = remember { TFLiteClassifier(context, labelProvider) }
    val speechManager = remember { SpeechManager(context) }

    val engine = remember {
        GestureEngine(
            handTracker = handTracker,
            featureBuilder = featureBuilder,
            classifier = classifier,
            labelProvider = labelProvider
        )
    }

    LaunchedEffect(ajustes.velocidadVoz, ajustes.tonoVoz, ajustes.variante) {
        speechManager.configurar(ajustes.velocidadVoz, ajustes.tonoVoz, ajustes.variante.locale)
    }
    DisposableEffect(engine) {
        val comparar = banco?.getString("comparar", null)
        if (comparar != null) {
            // "entrada" = "jpeg80": el comparador ve el mismo frame pasado por JPEG de calidad
            // 80, como el pipeline anterior (YuvToRgbConverter). Mide qué cambió al sacarlo.
            if (banco.getString("entrada", null) == "jpeg80") {
                engine.entradaComparador = { bmp ->
                    val bytes = java.io.ByteArrayOutputStream()
                        .also { bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, it) }.toByteArray()
                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
            }
            val archivo = java.io.File(context.filesDir, "metricas/comparacion_mp.jsonl").apply { parentFile?.mkdirs() }
            engine.comparador = TrackerPerezoso { trackerDesdeConfig(context, ajustes.sensibilidadDeteccion, comparar) }
            val principal = banco.getString("principal", null) ?: CONFIG_MEDIAPIPE
            engine.onComparacion = { fila ->
                Metricas.agregarLinea(archivo, fila.put("principal", principal)
                    .put("comparado", comparar).put("entrada", banco.getString("entrada", null) ?: "igual").toString())
            }
        }
        onDispose {
            // Se cierra en el hilo de la cámara, que es donde lo usa y donde lo creó (GPU).
            // Si ese hilo ya se apagó (salida de la pantalla), ya no hay frames: va directo.
            val comparador = engine.comparador
            engine.comparador = null
            if (comparador != null) {
                runCatching { executor.execute { comparador.close() } }.onFailure { comparador.close() }
            }
            engine.entradaComparador = null
            engine.onComparacion = null
        }
    }

    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var frameCount by remember { mutableStateOf(0) }
    // GestureEngine ajusta su objetivo de cuadros segun los fps que mide (10, 15 o 20),
    // asi que la barra tiene que seguir ese numero y no uno fijo.
    var frameTarget by remember { mutableStateOf(15) }
    // Cuantas muestras hay guardadas por sena. Se lee una vez al entrar y despues
    // se ajusta en memoria: releer el JSON en cada captura seria leer el dataset
    // entero varias veces por minuto.
    var conteoPorSena by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var recienGuardada by remember { mutableStateOf(false) }
    // Después de guardar: false mientras la app espera que bajen las manos, true cuando
    // ya se puede hacer la siguiente seña (lo avisa GestureEngine.onListoParaCapturar).
    var listoParaSiguiente by remember { mutableStateOf(false) }
    // Qué pasó con la última captura del modo traducción: el pie lo dice en vez de
    // quedarse mudo cuando no se reconoció (ver EstadoTraduccion).
    var ultimaLectura by remember { mutableStateOf<UltimaLectura>(UltimaLectura.Ninguna) }
    // Después de cada seña la app espera que se bajen las manos; si siguen arriba un rato,
    // el pie lo pide.
    var esperandoBajarManos by remember { mutableStateOf(false) }
    var pedirBajarManos by remember { mutableStateOf(false) }
    LaunchedEffect(esperandoBajarManos) {
        pedirBajarManos = false
        if (esperandoBajarManos) {
            delay(1500)
            pedirBajarManos = true
        }
    }
    val repoAjustes = repositorioAjustes()

    LaunchedEffect(trainingMode) {
        if (trainingMode) {
            conteoPorSena = try {
                withContext(Dispatchers.IO) { loadDatasetCountsByLabel(context) }
            } catch (e: Exception) {
                // Con el dataset roto, avisar ya: cada captura va a fallar hasta resolverlo.
                snack.showSnackbar(e.message ?: "No se pudo leer el dataset")
                emptyMap()
            }
        }
    }

    /** La seña de la planilla como opción del selector; "nada" no está en words.json. */
    fun opcionPara(clase: String): TrainingWordOption =
        (officialWords + customWords).firstOrNull { it.label == clase }
            ?: TrainingWordOption(clase, findTrainingWordImageInAssets(context, clase), isOfficial = false)
                .also { customWords += it }

    /** Mueve la sesión y deja elegida la seña que toca, con la pausa si corresponde. */
    fun irA(nueva: SesionGuiada) {
        sesion = nueva
        ProgresoSesion.guardar(context, nueva)
        val toma = nueva.actual
        if (toma == null) {
            sesionTerminada = nueva
            sesion = null
            selectedWord = null
            return
        }
        selectedWord = opcionPara(toma.clase)
        mostrarPausa = nueva.pausaAntes
    }

    fun deshacerUltimaMuestra() {
        scope.launch {
            val quitada = try {
                withContext(Dispatchers.IO) {
                    DatasetV1.quitarUltima(DatasetV1.archivo(context)).also { if (it != null) DatasetV2.quitarUltima(context) }
                }
            } catch (e: Exception) {
                snack.showSnackbar("No se pudo deshacer: ${e.message ?: e.javaClass.simpleName}")
                null
            }
            // En la sesión guiada, deshacer vuelve a la toma anterior para repetirla.
            if (quitada != null) sesion?.let { irA(it.retroceder()); mostrarPausa = false }
            if (quitada != null) {
                sessionSavedCount = (sessionSavedCount - 1).coerceAtLeast(0)
                conteoPorSena = conteoPorSena.toMutableMap().apply {
                    this[quitada] = ((this[quitada] ?: 1) - 1).coerceAtLeast(0)
                }
                recienGuardada = false
            }
        }
    }

    LaunchedEffect(Unit) {
        officialWords.clear()
        officialWords += withContext(Dispatchers.IO) { loadTrainingWordOptions(context) }
    }

    // Llegando desde el dataset la sena ya viene elegida: se resuelve en cuanto
    // termina de cargar el catalogo y el selector no llega a abrirse.
    LaunchedEffect(senaInicial, officialWords.size) {
        if (!trainingMode || senaInicial == null || selectedWord != null) return@LaunchedEffect
        val encontrada = officialWords.firstOrNull { it.label.equals(senaInicial, ignoreCase = true) }
        if (encontrada != null) {
            selectedWord = encontrada
        } else if (officialWords.isNotEmpty()) {
            // Una sena propia: no esta en words.json pero si en el dataset.
            selectedWord = TrainingWordOption(
                label = senaInicial,
                imageAssetPath = findTrainingWordImageInAssets(context, senaInicial),
                isOfficial = false,
            ).also { customWords += it }
        }
    }

    LaunchedEffect(trainingMode, selectedWord) {
        if (trainingMode && selectedWord == null && senaInicial == null && sesion == null &&
            !elegirParticipante && sesionTerminada == null
        ) showWordPicker = true
    }

    DisposableEffect(engine, trainingMode, selectedWord) {
        engine.onHands = { hands, w, h, rot, isFront ->
            lastHandsAt = SystemClock.uptimeMillis()
            overlayState.value = OverlayData(w, h, hands, rot, isFront)
        }

        engine.onPrediction = { prediction ->
            if (!trainingMode) {
                val sinNombre = prediction.gesture == "Unknown" || prediction.gesture == "Sin datos"
                val valida = !sinNombre && prediction.confidence >= ajustes.confianzaMinima

                esperandoBajarManos = true
                if (prediction.gesture == CLASE_NADA || sinNombre) {
                    // "nada" no se dice ni se escribe, pero se avisa en el pie: también
                    // sale cuando una seña real no se entendió, y callarse dejaba a la
                    // persona sin saber que tenía que repetir.
                    ultimaLectura = UltimaLectura.SinSena
                } else if (valida) {
                    ultimaLectura = UltimaLectura.Reconocida
                    val last = recentTranslations.lastOrNull()
                    if (last != prediction.gesture) {
                        currentTranslation = prediction.gesture
                        recentTranslations = (recentTranslations + prediction.gesture).takeLast(6)
                        // La voz dice exactamente lo que la pantalla muestra. Antes
                        // el speak estaba fuera de este if: decia "Unknown" y las
                        // predicciones de baja confianza en voz alta, y repetia la
                        // misma palabra en cada ciclo de captura.
                        // Pasa por nombreParaMostrar igual que el subtitulo: el id
                        // que devuelve el modelo es "por favor", no "Por favor".
                        if (ajustes.leerEnVozAlta) {
                            speechManager.speak(nombreParaMostrar(prediction.gesture))
                        }
                    }
                } else {
                    ultimaLectura = UltimaLectura.Dudosa(prediction.confidence)
                }
            }
        }

        engine.onCaptureProgress = { count, target ->
            frameCount = count
            if (target > 0) frameTarget = target
            if (count > 0) {
                recienGuardada = false
                ultimaLectura = UltimaLectura.Ninguna
                esperandoBajarManos = false
            }
        }

        engine.onCaptureStats = { cap, inf, fps, newTarget ->
            if (!trainingMode) {
                if (cap > 0) {
                    captureTime = cap
                    fpsValue = fps
                    targetFramesUsed = newTarget
                }
                if (inf > 0) inferTime = inf
            }
        }

        engine.onTopPredictions = { _, seq ->
            val sesionActual = sesion
            // Durante la pausa la cámara sigue viendo manos: no se guarda nada hasta
            // que la persona toque Continuar.
            if (trainingMode && !mostrarPausa) {
                val selected = selectedWord
                if (selected != null && seq.isNotEmpty()) {
                    val savedFrames = seq.map { it.toList() }
                    tUsed = savedFrames.size
                    dUsed = if (savedFrames.isNotEmpty()) savedFrames[0].size else 0
                    val participante = participanteActual()
                    // Se toma ya: la captura siguiente la reemplaza.
                    val captura = engine.ultimaCaptura
                    // El disco, fuera del hilo de la cámara (TL-A5). Y si no se pudo guardar,
                    // la sesión no avanza ni se cuenta la muestra: antes avanzaba igual y la
                    // toma se perdía sin que nadie se enterara (TL-C5).
                    scope.launch {
                        val error = withContext(Dispatchers.IO) {
                            try {
                                DatasetV1.agregar(DatasetV1.archivo(context), selected.label, savedFrames, participante,
                                    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
                                if (captura != null && !DatasetV2.guardarMuestra(context, selected.label, participante,
                                        captura, sesionActual?.meta())) {
                                    "la toma se guardó en el dataset v1 pero no en el v2 (landmarks crudos)"
                                } else null
                            } catch (e: Exception) {
                                Log.e("Dataset", "No se pudo guardar la muestra", e)
                                e.message ?: e.javaClass.simpleName
                            }
                        }
                        if (error != null) {
                            snack.showSnackbar("No se pudo guardar la toma: $error")
                            return@launch
                        }
                        if (sesionActual != null) irA(sesionActual.avanzar())
                        sessionSavedCount += 1
                        conteoPorSena = conteoPorSena.toMutableMap().apply {
                            this[selected.label] = (this[selected.label] ?: 0) + 1
                        }
                        // La confirmacion la da el panel, que ademas queda en pantalla
                        // hasta la captura siguiente. Un snackbar encima repetiria el
                        // mismo mensaje y taparia el visor justo cuando hay que mirar.
                        recienGuardada = true
                        listoParaSiguiente = false
                    }
                }
            }
        }

        engine.onListoParaCapturar = {
            listoParaSiguiente = true
            esperandoBajarManos = false
        }

        onDispose {
            engine.onListoParaCapturar = null
            engine.onHands = null
            engine.onPrediction = null
            engine.onCaptureProgress = null
            engine.onCaptureStats = null
            engine.onTopPredictions = null
        }
    }

    // Los recursos nativos duran lo que dura la pantalla, no lo que dura la sena
    // elegida. Antes se cerraban en el efecto de arriba, que tiene selectedWord
    // entre sus claves: al elegir una sena en el selector, Compose lo desechaba y
    // volvia a lanzarlo, cerrando el HandLandmarker mientras la camara le seguia
    // mandando cuadros. La siguiente deteccion contra el objeto nativo ya cerrado
    // tiraba la app abajo. Por eso el cierre va en su propio efecto sin claves.
    //
    // Al salir (auditoría, TL-A1/A2): primero se corta el flujo de frames y después se
    // libera todo en el hilo de la cámara. Como ese hilo es uno solo, el cierre corre
    // después del frame que estuviera procesando, no en paralelo con él; antes el
    // HandLandmarker se cerraba desde la UI con un frame a mitad de camino. Al final se
    // apaga el executor, que antes quedaba vivo (un hilo por cada vez que se abría la cámara).
    DisposableEffect(Unit) {
        onDispose {
            pantallaCerrada.set(true)
            camController.clearImageAnalysisAnalyzer()
            camController.unbind()
            speechManager.release()
            executor.execute {
                handTracker.close()
                classifier.close()
            }
            executor.shutdown()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(250)
            val stale = SystemClock.uptimeMillis() - lastHandsAt > 600
            if (stale && overlayState.value.hands.isNotEmpty()) {
                overlayState.value = overlayState.value.copy(hands = emptyList())
            }
        }
    }

    val configuration = LocalConfiguration.current
    LaunchedEffect(hasPermission, lifecycleOwner, isFrontCamera, ajustes.calidad) {
        if (!hasPermission || classifier.problemas.isNotEmpty()) return@LaunchedEffect
        val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        val corto = ajustes.calidad.ladoCorto
        val largo = ajustes.calidad.ladoLargo
        val targetSize = if (isPortrait) Size(corto, largo) else Size(largo, corto)

        cameraManager.configure(
            lensFacingFront = isFrontCamera,
            analysisStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST,
            targetSize = targetSize
        )
        engine.setCameraFacing(isFront = isFrontCamera)
        camController.bindToLifecycle(lifecycleOwner)
        camController.clearImageAnalysisAnalyzer()

        val analyzer = FrameAnalyzer(context) { bitmap, rotationDeg, ts, convNanos, yaDerecho ->
            engine.process(bitmap, rotationDeg, ts, convNanos, yaDerecho)
        }
        camController.setImageAnalysisAnalyzer(executor) { imageProxy ->
            if (pantallaCerrada.get()) imageProxy.close() else analyzer.analyze(imageProxy)
        }
    }

    // Con un modelo que no corresponde no se abre la cámara: se dice por qué (X3).
    if (classifier.problemas.isNotEmpty()) {
        AvisoModeloInvalido(
            problemas = classifier.problemas,
            esPaquete = classifier.modeloInstalado != null,
            onVolver = onBack,
        )
        return
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { scaleX = if (isFrontCamera) -1f else 1f },
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    controller = camController
                    scaleX = if (isFrontCamera) -1f else 1f
                }
            },
            update = { pv ->
                pv.controller = camController
                pv.scaleX = if (isFrontCamera) -1f else 1f
            }
        )

        if (ajustes.mostrarLandmarks) {
            HandLandmarksOverlay(overlay = overlayState.value)
        }

        val senaElegida = selectedWord
        val muestrasDeLaSena = senaElegida?.let { conteoPorSena[it.label] ?: 0 } ?: 0
        val hayManos = overlayState.value.hands.isNotEmpty()

        // El velo va debajo del encuadre: al reves le apagaba las esquinas de
        // abajo, que son justo las que dicen hasta donde llega el cuadro.
        VeloInferior(Modifier.align(Alignment.BottomCenter))

        // El encuadre ocupa casi todo lo que queda entre la cabecera y el pie.
        // Las senas de LSA se hacen con las dos manos y toman del torso para
        // arriba: cuanto mas grande es el recuadro, mas cerca puede estar la
        // persona y mas grandes le quedan las manos al detector.
        if (!trainingMode || senaElegida != null) {
            GuiaDeEncuadre(
                encuadreCorrecto = hayManos,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 62.dp, bottom = 126.dp),
            )
        }

        if (trainingMode) {
            CabeceraCamara(
                titulo = senaElegida?.let { "Entrenando ${nombreParaMostrar(it.label)}" }
                    ?: "Modo entrenamiento",
                onVolver = onBack,
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                if (sesion == null && planilla.isNotEmpty()) {
                    TextButton(onClick = { elegirParticipante = true; showWordPicker = false }) {
                        Text("Guiada", color = Color.White)
                    }
                }
                if (senaElegida != null) InsigniaDeProgreso(muestrasDeLaSena)
            }

            sesion?.let { s ->
                TiraSesionGuiada(
                    sesion = s,
                    onSalir = { sesion = null; selectedWord = null; mostrarPausa = false },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 62.dp),
                )
            }

            if (senaElegida?.imageAssetPath != null) {
                ReferenciaFija(
                    assetPath = senaElegida.imageAssetPath,
                    onAmpliar = { showReference = true },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 118.dp, end = 16.dp),
                )
            }

            PieDeCaptura(
                haySena = senaElegida != null,
                muestrasDeLaSena = muestrasDeLaSena,
                frameCount = frameCount,
                frameTarget = frameTarget,
                hayManos = hayManos,
                recienGuardada = recienGuardada,
                listoParaSiguiente = listoParaSiguiente,
                onCambiarSena = { showWordPicker = true },
                onGirarCamara = { isFrontCamera = !isFrontCamera },
                onDeshacer = { deshacerUltimaMuestra() },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        } else {
            CabeceraCamara(
                titulo = "Traducir señas",
                onVolver = onBack,
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                BotonRedondo(
                    icono = Icons.Default.Cameraswitch,
                    descripcion = if (isFrontCamera) "Cambiar a cámara trasera" else "Cambiar a cámara frontal",
                    onClick = { isFrontCamera = !isFrontCamera },
                    tamano = 38.dp,
                )
                Spacer(Modifier.width(8.dp))
                PildoraDeVoz(
                    activa = ajustes.leerEnVozAlta,
                    onClick = { repoAjustes.actualizar { it.copy(leerEnVozAlta = !it.leerEnVozAlta) } },
                )
            }

            PieDeTraduccion(
                frase = recentTranslations,
                hayManos = hayManos,
                estado = estadoTraduccion(ultimaLectura, frameCount, hayManos, recentTranslations.isNotEmpty(), pedirBajarManos),
                onRepetir = {
                    val texto = recentTranslations.joinToString(" ") { nombreParaMostrar(it) }
                    if (texto.isNotBlank()) speechManager.speak(texto)
                },
                onLimpiar = {
                    recentTranslations = emptyList()
                    currentTranslation = ""
                    ultimaLectura = UltimaLectura.Ninguna
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        if (trainingMode && elegirParticipante) {
            DialogoElegirParticipante(
                codigos = SesionGuiada.codigos(planilla),
                onElegir = { codigo ->
                    elegirParticipante = false
                    showWordPicker = false
                    // Retoma donde quedó esa persona si la app se cerró a mitad de sesión.
                    var s = SesionGuiada.de(planilla, codigo, ProgresoSesion.indice(context, codigo))
                    // Si la app perdió la posición (reinstalada, datos borrados) pero esa
                    // persona ya grabó la sesión 1 completa, se arranca en la robustez en
                    // vez de hacerle repetir las 106 tomas.
                    val inicioB = s.primeraDelBloque("B")
                    if (s.indice == 0 && inicioB != null && DatasetV2.contarDeCodigo(context, codigo) >= inicioB) {
                        s = s.irA(inicioB)
                    }
                    irA(s)
                    // Al retomar en una condición de robustez, el cartel dice qué cambiar.
                    mostrarPausa = s.actual?.let { instruccionCondicion(it.condicion) != null } ?: false
                },
                onCancelar = { elegirParticipante = false },
            )
        }
        sesion?.let { s -> if (mostrarPausa) DialogoPausa(s) { mostrarPausa = false } }
        sesionTerminada?.let { s -> DialogoSesionTerminada(s) { sesionTerminada = null } }

        if (trainingMode && showWordPicker) {
            TrainingWordPickerSheet(
                words = (officialWords + customWords).sortedBy { displayTrainingLabel(it.label) },
                conteoPorSena = conteoPorSena,
                onDismiss = { showWordPicker = false },
                onSelect = {
                    selectedWord = it
                    showWordPicker = false
                },
                onCreateWord = { rawLabel ->
                    val normalized = normalizeTrainingLabel(rawLabel)
                    if (normalized.isBlank()) {
                        scope.launch { snack.showSnackbar("Escribí un nombre válido para la seña.") }
                        return@TrainingWordPickerSheet
                    }

                    val existing = (officialWords + customWords).firstOrNull {
                        it.label.equals(normalized, ignoreCase = true)
                    }

                    selectedWord = existing ?: TrainingWordOption(
                        label = normalized,
                        imageAssetPath = null,
                        isOfficial = false
                    ).also { customWords += it }
                    showWordPicker = false
                }
            )
        }

        val referencia = selectedWord
        val imagenDeReferencia = referencia?.imageAssetPath
        if (trainingMode && showReference && referencia != null && imagenDeReferencia != null) {
            TrainingReferenceDialog(
                label = referencia.label,
                assetPath = imagenDeReferencia,
                onDismiss = { showReference = false }
            )
        }

        SnackbarHost(hostState = snack, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp))
    }
}






