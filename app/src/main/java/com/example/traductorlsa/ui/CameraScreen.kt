package com.example.traductorlsa.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.os.Environment
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
import com.example.traductorlsa.detection.HandTrackerImpl
import com.example.traductorlsa.settings.ajustesSenar
import com.example.traductorlsa.settings.repositorioAjustes
import com.example.traductorlsa.features.FeatureBuilderImpl
import com.example.traductorlsa.features.SequenceBufferImpl
import com.example.traductorlsa.ml.GestureEngine
import com.example.traductorlsa.ml.LabelProviderImpl
import com.example.traductorlsa.ml.TFLiteClassifier
import com.example.traductorlsa.model.NormPoint
import com.example.traductorlsa.model.PredictionResult
import com.example.traductorlsa.speech.SpeechManager
import com.example.traductorlsa.ui.screens.DictionaryAssetImage
import com.example.traductorlsa.ui.screens.nombreParaMostrar
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
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

// ---------- modelos/estado simples ----------
data class OverlayData(
    val imgW: Int = 0,
    val imgH: Int = 0,
    val hands: List<List<NormPoint>> = emptyList(),
    val rotationDeg: Int = 0,
    val isFront: Boolean = true
)

data class TrainingSample(
    val label: String,
    val seq: List<List<Float>> // T x D
)

data class TrainingWordOption(
    val label: String,
    val imageAssetPath: String?,
    val isOfficial: Boolean
)

// Archivo Ãºnico y ruta
const val DATASET_FILE_NAME = "lsa_samples.json"

/**
 * Muestras que se busca juntar por sena antes de reentrenar.
 *
 * Es un valor de trabajo, no una medida: el numero real sale del script de
 * entrenamiento en Python, que es el que sabe cuantas repeticiones por clase
 * necesita. Se cambia aca y se actualizan las tres pantallas a la vez.
 */
internal const val MUESTRAS_OBJETIVO = 20

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

internal fun normalizeTrainingLabel(input: String): String {
    val lower = input.trim().lowercase(Locale.getDefault())
    val noAccents = Normalizer.normalize(lower, Normalizer.Form.NFD)
        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")

    return noAccents
        .replace("[^a-z0-9_ ]".toRegex(), "")
        .replace("\\s+".toRegex(), "_")
        .trim('_')
}

internal fun displayTrainingLabel(label: String): String =
    label.replace("_", " ").replace("\\s+".toRegex(), " ").trim()

internal fun findTrainingWordImageInAssets(context: Context, word: String): String? {
    val base = normalizeTrainingLabel(word)
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

    // La sensibilidad se toma al entrar a la pantalla y no en vivo: recrear el
    // detector recarga los 8 MB del modelo, y el deslizador de Ajustes dispara
    // en cada movimiento. Cambiarla tiene efecto la próxima vez que se abre.
    val handTracker = remember { HandTrackerImpl(context, ajustes.sensibilidadDeteccion) }
    val featureBuilder = remember { FeatureBuilderImpl() }
    val sequenceBuffer = remember { SequenceBufferImpl() }
    val labelProvider = remember { LabelProviderImpl(context) }
    val classifier = remember { TFLiteClassifier(context, labelProvider) }
    val speechManager = remember { SpeechManager(context) }

    val engine = remember {
        GestureEngine(
            handTracker = handTracker,
            featureBuilder = featureBuilder,
            sequenceBuffer = sequenceBuffer,
            classifier = classifier,
            labelProvider = labelProvider
        )
    }

    LaunchedEffect(ajustes.velocidadVoz, ajustes.tonoVoz, ajustes.variante) {
        speechManager.configurar(ajustes.velocidadVoz, ajustes.tonoVoz, ajustes.variante.locale)
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
    // Cuando la prediccion no llega al minimo de Ajustes, la pantalla lo dice en
    // vez de quedarse muda. -1 significa que no hay ninguna descartada.
    var confianzaDescartada by remember { mutableStateOf(-1f) }
    val repoAjustes = repositorioAjustes()

    LaunchedEffect(trainingMode) {
        if (trainingMode) {
            conteoPorSena = withContext(Dispatchers.IO) { loadDatasetCountsByLabel(context) }
        }
    }

    fun deshacerUltimaMuestra() {
        scope.launch {
            val quitada = withContext(Dispatchers.IO) { removeLastDatasetSample(context) }
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
        if (trainingMode && selectedWord == null && senaInicial == null) showWordPicker = true
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

                if (valida) {
                    confianzaDescartada = -1f
                    val last = recentTranslations.lastOrNull()
                    if (last != prediction.gesture) {
                        currentTranslation = prediction.gesture
                        recentTranslations = (recentTranslations + prediction.gesture).takeLast(6)
                        // La voz dice exactamente lo que la pantalla muestra. Antes
                        // el speak estaba fuera de este if: decia "Unknown" y las
                        // predicciones de baja confianza en voz alta, y repetia la
                        // misma palabra en cada ciclo de captura.
                        if (ajustes.leerEnVozAlta) speechManager.speak(prediction.gesture)
                    }
                } else {
                    // Sin nombre no hay porcentaje que valga la pena mostrar.
                    confianzaDescartada = if (sinNombre) 0f else prediction.confidence
                }
            }
        }

        engine.onCaptureProgress = { count, target ->
            frameCount = count
            if (target > 0) frameTarget = target
            if (count > 0) {
                recienGuardada = false
                confianzaDescartada = -1f
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
            if (trainingMode) {
                val selected = selectedWord
                if (selected != null && seq.isNotEmpty()) {
                    val savedFrames = seq.map { it.toList() }
                    tUsed = savedFrames.size
                    dUsed = if (savedFrames.isNotEmpty()) savedFrames[0].size else 0
                    appendSamplesByDate(
                        context = context,
                        samples = listOf(TrainingSample(label = selected.label, seq = savedFrames)),
                        T = tUsed,
                        D = dUsed
                    )
                    sessionSavedCount += 1
                    conteoPorSena = conteoPorSena.toMutableMap().apply {
                        this[selected.label] = (this[selected.label] ?: 0) + 1
                    }
                    // La confirmacion la da el panel, que ademas queda en pantalla
                    // hasta la captura siguiente. Un snackbar encima repetiria el
                    // mismo mensaje y taparia el visor justo cuando hay que mirar.
                    recienGuardada = true
                }
            }
        }

        onDispose {
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
    DisposableEffect(Unit) {
        onDispose {
            speechManager.release()
            handTracker.close()
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
        if (!hasPermission) return@LaunchedEffect
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

        val analyzer = FrameAnalyzer(context) { bitmap, rotationDeg, ts ->
            engine.process(bitmap, rotationDeg, ts)
        }
        camController.setImageAnalysisAnalyzer(executor) { imageProxy ->
            analyzer.analyze(imageProxy)
        }
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
            com.example.traductorlsa.ui.overlay.HandLandmarksOverlay(overlay = overlayState.value)
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
                if (senaElegida != null) InsigniaDeProgreso(muestrasDeLaSena)
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
                frameCount = frameCount,
                confianzaDescartada = confianzaDescartada,
                onRepetir = {
                    val texto = recentTranslations.joinToString(" ") { nombreParaMostrar(it) }
                    if (texto.isNotBlank()) speechManager.speak(texto)
                },
                onLimpiar = {
                    recentTranslations = emptyList()
                    currentTranslation = ""
                    confianzaDescartada = -1f
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

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

        if (trainingMode && showReference && selectedWord?.imageAssetPath != null) {
            TrainingReferenceDialog(
                label = selectedWord!!.label,
                assetPath = selectedWord!!.imageAssetPath!!,
                onDismiss = { showReference = false }
            )
        }

        SnackbarHost(hostState = snack, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrainingPanel(
    visible: Boolean,
    top3: List<PredictionResult>,
    allLabels: List<String>,
    customLabels: List<String>,
    onPick: (String) -> Unit,
    onClose: () -> Unit,
    onCreateAndPick: (String) -> Unit
) {
    if (!visible) return
    var addMode by remember { mutableStateOf(false) }
    var newLabel by remember { mutableStateOf("") }
    var showAll by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var search by remember { mutableStateOf("") }
    val filtered = remember(allLabels, search) {
        val q = search.lowercase()
        if (q.isBlank()) allLabels else allLabels.filter { it.lowercase().contains(q) }
    }

    if (showAll) {
        ModalBottomSheet(onDismissRequest = { showAll = false }, sheetState = sheetState) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Elegí una etiqueta", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(value = search, onValueChange = { search = it }, placeholder = { Text("Buscar") }, modifier = Modifier.fillMaxWidth())
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    items(filtered) { label ->
                        ListItem(headlineContent = { Text(label) }, modifier = Modifier.clickable { onPick(label); showAll = false })
                    }
                }
            }
        }
    }

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f))) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (addMode) "Nueva etiqueta:" else "Seleccioná¡ la etiqueta:", color = Color.White)
                Row {
                    IconButton(onClick = { showAll = true }) { Icon(Icons.Default.List, null, tint = Color.White) }
                    IconButton(onClick = { addMode = !addMode }) { Icon(Icons.Default.Add, null, tint = Color.White) }
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, null, tint = Color.White) }
                }
            }
            if (addMode) {
                OutlinedTextField(value = newLabel, onValueChange = { newLabel = it }, modifier = Modifier.fillMaxWidth())
                Button(onClick = { onCreateAndPick(newLabel); newLabel = ""; addMode = false }) { Text("Guardar") }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (0 until 3).forEach { i ->
                        val pred = top3.getOrNull(i)
                        Button(onClick = { pred?.let { onPick(it.gesture) } }, enabled = pred != null, modifier = Modifier.weight(1f)) {
                            Text(pred?.gesture ?: "â€”", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Cabecera de las dos camaras: volver, titulo y una insignia a la derecha.
 *
 * Una sola linea. Cada franja que ocupa la interfaz obliga a la persona a
 * alejarse de la camara, y de lejos las manos le quedan chicas al detector.
 */
@Composable
private fun CabeceraCamara(
    titulo: String,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    insignia: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(Color(0xB8080B14), Color(0x00080B14)))
            )
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BotonRedondo(
            icono = Icons.Default.ArrowBack,
            descripcion = "Volver",
            onClick = onVolver,
            tamano = 38.dp,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.92f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(10.dp))
        insignia()
    }
}

/** Muestras guardadas de la sena que se esta entrenando, con su barra. */
@Composable
private fun InsigniaDeProgreso(muestras: Int) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.13f))
            .padding(start = 13.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(muestras.toString()) }
                withStyle(SpanStyle(color = Color.White.copy(alpha = 0.5f))) { append("/$MUESTRAS_OBJETIVO") }
            },
            color = Color.White,
            fontSize = 13.sp,
        )
        Spacer(Modifier.width(8.dp))
        LinearProgressIndicator(
            progress = { (muestras.toFloat() / MUESTRAS_OBJETIVO).coerceIn(0f, 1f) },
            modifier = Modifier.width(38.dp).height(4.dp).clip(RoundedCornerShape(999.dp)),
            color = SenarAzul300,
            trackColor = Color.White.copy(alpha = 0.20f),
            strokeCap = StrokeCap.Round,
        )
    }
}

/** Interruptor de voz, a mano durante la conversacion y no enterrado en Ajustes. */
@Composable
private fun PildoraDeVoz(activa: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (activa) SenarAzul600 else Color.White.copy(alpha = 0.14f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (activa) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
            contentDescription = if (activa) "Apagar la voz" else "Encender la voz",
            tint = Color.White,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text("Voz", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

/**
 * Degradado inferior en lugar de una tarjeta.
 *
 * Es el recurso de los subtitulos: deja el texto legible sobre cualquier video
 * sin recortarle un tercio de pantalla al visor.
 */
@Composable
private fun VeloInferior(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(190.dp)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0x00060910), Color(0xBD060910), Color(0xF0060910))
                )
            )
    )
}

@Composable
private fun BotonRedondo(
    icono: ImageVector,
    descripcion: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primario: Boolean = false,
    tamano: Dp = 42.dp,
) {
    Box(
        modifier
            .size(tamano)
            .clip(CircleShape)
            .background(if (primario) SenarAzul600 else Color.White.copy(alpha = 0.13f))
            .then(
                if (primario) Modifier
                else Modifier.border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icono,
            contentDescription = descripcion,
            tint = Color.White,
            modifier = Modifier.size(tamano * 0.42f),
        )
    }
}

/** Rotulo de estado: un punto de color y una linea en versalitas. */
@Composable
private fun RotuloDeEstado(color: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(
            text = texto,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            color = Color.White.copy(alpha = 0.68f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Guia de encuadre: cuatro esquinas y nada mas.
 *
 * Sin esto la camara puede estar apuntando al techo y la muestra se guarda
 * igual, con lo cual entra ruido al dataset. Van solo las esquinas para no
 * tapar a la persona justo mientras esta senando; se ponen azules cuando el
 * detector encuentra manos, que es la senal de que el encuadre sirve.
 */
@Composable
private fun GuiaDeEncuadre(
    encuadreCorrecto: Boolean,
    modifier: Modifier = Modifier,
) {
    val color = if (encuadreCorrecto) SenarAzul300 else Color.White.copy(alpha = 0.30f)
    Canvas(modifier) {
        val largo = 30.dp.toPx()
        val grosor = 3.dp.toPx()
        val m = grosor / 2f
        val ancho = size.width - m
        val alto = size.height - m

        fun linea(x1: Float, y1: Float, x2: Float, y2: Float) {
            drawLine(color, Offset(x1, y1), Offset(x2, y2), grosor, StrokeCap.Round)
        }

        linea(m, m + largo, m, m); linea(m, m, m + largo, m)
        linea(ancho - largo, m, ancho, m); linea(ancho, m, ancho, m + largo)
        linea(m, alto - largo, m, alto); linea(m, alto, m + largo, alto)
        linea(ancho - largo, alto, ancho, alto); linea(ancho, alto, ancho, alto - largo)
    }
}

/**
 * Miniatura de referencia, fija en pantalla.
 *
 * Antes era un boton que abria un modal, y ademas se apagaba cuando la sena no
 * tenia imagen. Un modal no se puede mirar mientras se sena, que es justo
 * cuando hace falta: aca queda de reojo y se toca para agrandarla.
 */
@Composable
private fun ReferenciaFija(
    assetPath: String?,
    onAmpliar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (assetPath == null) return

    Box(
        modifier
            .size(58.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SenarBlanco.copy(alpha = 0.86f))
            .clickable(onClick = onAmpliar)
            .padding(4.dp)
    ) {
        DictionaryAssetImage(
            assetPath = assetPath,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
    }
}

/**
 * Pie del modo entrenamiento.
 *
 * Dos lineas y los controles en redondo, alineados con el texto. Nombra los
 * estados que GestureEngine ya tenia y no mostraba; los dos que faltaban son
 * que bajar las manos en plena captura la descarta entera, y que despues de
 * guardar hay que bajarlas para que arranque la siguiente.
 */
@Composable
private fun PieDeCaptura(
    haySena: Boolean,
    muestrasDeLaSena: Int,
    frameCount: Int,
    frameTarget: Int,
    hayManos: Boolean,
    recienGuardada: Boolean,
    onCambiarSena: () -> Unit,
    onGirarCamara: () -> Unit,
    onDeshacer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val grabando = frameCount > 0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 18.dp, end = 18.dp, bottom = 22.dp),
    ) {
        RotuloDeEstado(
            color = when {
                !haySena -> Color.White.copy(alpha = 0.40f)
                grabando || recienGuardada -> SenarAzul600
                else -> Color.White.copy(alpha = 0.40f)
            },
            texto = when {
                !haySena -> "SIN SEÑA ELEGIDA"
                grabando -> "GRABANDO · CUADRO $frameCount DE $frameTarget"
                recienGuardada -> "MUESTRA $muestrasDeLaSena GUARDADA"
                hayManos -> "TE VEO"
                else -> "ESPERANDO"
            },
        )

        Row(
            modifier = Modifier.padding(top = 7.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = when {
                        !haySena -> "Elegí una seña para empezar"
                        grabando -> "No bajes las manos"
                        recienGuardada -> "Bajá las manos"
                        else -> "Mostrá la seña frente a la cámara"
                    },
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 24.sp,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (grabando || recienGuardada) {
                    LinearProgressIndicator(
                        progress = {
                            if (recienGuardada) 1f
                            else (frameCount.toFloat() / frameTarget.coerceAtLeast(1)).coerceIn(0f, 1f)
                        },
                        modifier = Modifier
                            .padding(top = 9.dp)
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = if (recienGuardada) SenarAzul600 else SenarAzul500,
                        trackColor = Color.White.copy(alpha = 0.20f),
                        strokeCap = StrokeCap.Round,
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (recienGuardada) {
                    BotonRedondo(
                        icono = Icons.Default.Undo,
                        descripcion = "Deshacer la última muestra",
                        onClick = onDeshacer,
                    )
                }
                BotonRedondo(
                    icono = Icons.Default.SwapHoriz,
                    descripcion = "Cambiar de seña",
                    onClick = onCambiarSena,
                )
                BotonRedondo(
                    icono = Icons.Default.Cameraswitch,
                    descripcion = "Girar la cámara",
                    onClick = onGirarCamara,
                )
            }
        }
    }
}

/**
 * Pie del modo traduccion.
 *
 * La frase se lee como subtitulo: azul-300 para lo ya dicho, que es el color de
 * quien sena en todo el sistema, y la ultima palabra en blanco y negrita. Se
 * distingue por peso y color y no por tamano, asi la linea nunca salta de alto.
 */
@Composable
private fun PieDeTraduccion(
    frase: List<String>,
    hayManos: Boolean,
    frameCount: Int,
    confianzaDescartada: Float,
    onRepetir: () -> Unit,
    onLimpiar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val descartada = confianzaDescartada >= 0f
    val porcentaje = (confianzaDescartada * 100).toInt()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 18.dp, end = 18.dp, bottom = 22.dp),
    ) {
        RotuloDeEstado(
            color = when {
                descartada -> SenarAmbar
                frase.isNotEmpty() -> SenarAzul600
                else -> Color.White.copy(alpha = 0.40f)
            },
            texto = when {
                descartada && confianzaDescartada > 0f -> "NO LA RECONOCÍ · $porcentaje%"
                descartada -> "NO LA RECONOCÍ"
                frameCount > 0 -> "LEYENDO LA SEÑA…"
                frase.isNotEmpty() -> "TRADUCIENDO"
                hayManos -> "TE VEO"
                else -> "ESPERANDO SEÑAS"
            },
        )

        if (descartada) {
            Text(
                text = "Repetila un poco más despacio.",
                fontSize = 12.5.sp,
                color = Color.White.copy(alpha = 0.58f),
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        if (frase.isEmpty() && !descartada) {
            Text(
                text = if (hayManos) "Hacé la seña" else "Ubicate en el recuadro",
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.82f),
                modifier = Modifier.padding(top = 7.dp),
            )
            Text(
                text = "Torso y las dos manos. Arranca sola.",
                fontSize = 12.5.sp,
                color = Color.White.copy(alpha = 0.50f),
                modifier = Modifier.padding(top = 3.dp),
            )
        } else {
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = buildAnnotatedString {
                        frase.forEachIndexed { i, palabra ->
                            if (i > 0) {
                                withStyle(SpanStyle(color = Color.White.copy(alpha = 0.28f))) {
                                    append("  ·  ")
                                }
                            }
                            val ultima = i == frase.lastIndex && !descartada
                            withStyle(
                                SpanStyle(
                                    color = if (ultima) Color.White else SenarAzul300,
                                    fontWeight = if (ultima) FontWeight.Bold else FontWeight.Medium,
                                )
                            ) {
                                append(nombreParaMostrar(palabra))
                            }
                        }
                    },
                    fontSize = 21.sp,
                    lineHeight = 28.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                if (frase.isNotEmpty()) {
                    Spacer(Modifier.width(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BotonRedondo(
                            icono = Icons.Default.VolumeUp,
                            descripcion = "Repetir en voz alta",
                            onClick = onRepetir,
                            primario = true,
                        )
                        BotonRedondo(
                            icono = Icons.Default.Delete,
                            descripcion = "Borrar la frase",
                            onClick = onLimpiar,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrainingWordPickerSheet(
    words: List<TrainingWordOption>,
    conteoPorSena: Map<String, Int>,
    onDismiss: () -> Unit,
    onSelect: (TrainingWordOption) -> Unit,
    onCreateWord: (String) -> Unit
) {
    var busqueda by remember { mutableStateOf("") }
    var modoNueva by remember { mutableStateOf(false) }
    var nombreNuevo by remember { mutableStateOf("") }
    var filtro by remember { mutableStateOf(FiltroDeSenas.FALTAN) }

    fun muestrasDe(w: TrainingWordOption) = conteoPorSena[w.label] ?: 0

    val visibles = remember(words, busqueda, filtro, conteoPorSena) {
        val texto = busqueda.trim().lowercase(Locale.getDefault())
        words
            .filter { nombreParaMostrar(it.label).lowercase(Locale.getDefault()).contains(texto) }
            .filter {
                when (filtro) {
                    FiltroDeSenas.FALTAN -> muestrasDe(it) < MUESTRAS_OBJETIVO
                    FiltroDeSenas.TODAS -> true
                    FiltroDeSenas.MIAS -> !it.isOfficial
                }
            }
            // Primero lo que falta: la sena sin muestras es la que hay que grabar.
            .sortedWith(compareBy({ muestrasDe(it) }, { nombreParaMostrar(it.label) }))
    }

    val faltan = words.count { muestrasDe(it) < MUESTRAS_OBJETIVO }
    val mias = words.count { !it.isOfficial }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SenarPapel) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp)
        ) {
            if (!modoNueva) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Elegí la seña a entrenar",
                        style = MaterialTheme.typography.headlineSmall,
                        color = SenarGrafito900,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(12.dp))
                    OutlinedButton(
                        onClick = { modoNueva = true },
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, SenarBorde),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = SenarBlanco),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = SenarAzul600, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Nueva", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = SenarAzul600)
                    }
                }

                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = SenarGrafito300)
                    },
                    placeholder = { Text("Buscar entre ${words.size} señas", color = SenarGrafito300) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SenarBlanco,
                        unfocusedContainerColor = SenarBlanco,
                        unfocusedBorderColor = SenarBorde,
                    ),
                )

                Spacer(Modifier.height(13.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChipDeFiltro("Faltan muestras · $faltan", filtro == FiltroDeSenas.FALTAN) { filtro = FiltroDeSenas.FALTAN }
                    ChipDeFiltro("Todas · ${words.size}", filtro == FiltroDeSenas.TODAS) { filtro = FiltroDeSenas.TODAS }
                    if (mias > 0) {
                        ChipDeFiltro("Mías · $mias", filtro == FiltroDeSenas.MIAS) { filtro = FiltroDeSenas.MIAS }
                    }
                }

                Spacer(Modifier.height(6.dp))
                if (visibles.isEmpty()) {
                    Text(
                        text = if (filtro == FiltroDeSenas.FALTAN) {
                            "Todas las señas llegaron a las $MUESTRAS_OBJETIVO muestras."
                        } else {
                            "No hay señas que coincidan con la búsqueda."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = SenarGrafito500,
                        modifier = Modifier.padding(vertical = 28.dp),
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                        items(visibles) { word ->
                            FilaDeSenaAEntrenar(
                                nombre = nombreParaMostrar(word.label),
                                assetPath = word.imageAssetPath,
                                muestras = muestrasDe(word),
                                propia = !word.isOfficial,
                                onClick = { onSelect(word) },
                            )
                            HorizontalDivider(color = SenarBordeSuave)
                        }
                    }
                }
            } else {
                Text(
                    text = "Agregar una seña",
                    style = MaterialTheme.typography.headlineSmall,
                    color = SenarGrafito900,
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = nombreNuevo,
                    onValueChange = { nombreNuevo = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    label = { Text("Nombre de la seña") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SenarBlanco,
                        unfocusedContainerColor = SenarBlanco,
                        unfocusedBorderColor = SenarBorde,
                    ),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Usá únicamente una seña real de LSA. Va a quedar disponible para entrenar aunque todavía no tenga dibujo de referencia.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SenarGrafito500,
                )
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { modoNueva = false },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, SenarBorde),
                    ) {
                        Text("Cancelar", color = SenarGrafito500, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = { onCreateWord(nombreNuevo); nombreNuevo = "" },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SenarAzul600),
                    ) {
                        Text("Agregar", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

private enum class FiltroDeSenas { FALTAN, TODAS, MIAS }

/**
 * Fila del selector.
 *
 * Muestra el dibujo del diccionario y cuantas muestras tiene la sena, que es el
 * dato con el que se elige. La hoja anterior repetia "Seña oficial" en las 24
 * filas y escondia el numero.
 */
@Composable
private fun FilaDeSenaAEntrenar(
    nombre: String,
    assetPath: String?,
    muestras: Int,
    propia: Boolean,
    onClick: () -> Unit,
) {
    val completa = muestras >= MUESTRAS_OBJETIVO

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(SenarBlanco)
                .border(1.dp, SenarBordeSuave, RoundedCornerShape(13.dp))
                .padding(3.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (assetPath != null) {
                DictionaryAssetImage(
                    assetPath = assetPath,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Icon(
                    Icons.Default.PanTool, contentDescription = null,
                    tint = SenarGrafito300, modifier = Modifier.size(19.dp),
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = nombre,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SenarGrafito900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (propia) {
                    Spacer(Modifier.width(7.dp))
                    Surface(shape = RoundedCornerShape(999.dp), color = SenarAzul100) {
                        Text(
                            text = "Tuya",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SenarAzul700,
                        )
                    }
                }
            }
            Spacer(Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { (muestras.toFloat() / MUESTRAS_OBJETIVO).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .width(74.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = if (completa) SenarAzul600 else SenarAzul300,
                    trackColor = SenarBordeSuave,
                    strokeCap = StrokeCap.Round,
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    text = when {
                        muestras == 0 -> "sin muestras"
                        completa -> "completa"
                        else -> "$muestras de $MUESTRAS_OBJETIVO"
                    },
                    fontSize = 12.sp,
                    color = when {
                        muestras == 0 -> SenarGrafito300
                        completa -> SenarAzul600
                        else -> SenarGrafito500
                    },
                )
            }
        }

        if (completa) {
            Box(
                Modifier.size(20.dp).clip(CircleShape).background(SenarAzul600),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = SenarBlanco, modifier = Modifier.size(13.dp))
            }
        } else {
            Icon(
                Icons.Default.ChevronRight, contentDescription = null,
                tint = SenarPista, modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun TrainingReferenceDialog(
    label: String,
    assetPath: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember(assetPath) { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(assetPath) {
        bitmap = withContext(Dispatchers.IO) {
            try {
                context.assets.open(assetPath).use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        ElevatedCard {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(displayTrainingLabel(label), style = MaterialTheme.typography.titleLarge)
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = displayTrainingLabel(label),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 220.dp, max = 420.dp)
                    )
                } else {
                    Text("No se pudo cargar la imagen de referencia.")
                }
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Cerrar")
                }
            }
        }
    }
}

fun datasetFile(context: Context): File {
    val base = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
    return File(base, DATASET_FILE_NAME)
}

fun loadOrInitDataset(context: Context, T: Int, D: Int): JSONObject {
    val f = datasetFile(context)
    if (!f.exists() || f.length() == 0L) return JSONObject().apply { put("t", T); put("d", D); put("by_date", JSONObject()) }
    return try {
        JSONObject(f.readText()).apply { if (!has("by_date")) put("by_date", JSONObject()) }
    } catch (e: Exception) {
        JSONObject().apply { put("t", T); put("d", D); put("by_date", JSONObject()) }
    }
}

fun appendSamplesByDate(context: Context, samples: List<TrainingSample>, T: Int, D: Int): File {
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val root = loadOrInitDataset(context, T, D)
    val byDate = root.getJSONObject("by_date")
    val arr = if (byDate.has(today)) byDate.getJSONArray(today) else JSONArray()
    samples.forEach { s ->
        val seqJson = JSONArray()
        s.seq.forEach { f -> val fArr = JSONArray(); f.forEach { v -> fArr.put(v) }; seqJson.put(fArr) }
        arr.put(JSONObject().put("label", s.label).put("seq", seqJson))
    }
    byDate.put(today, arr)
    val out = datasetFile(context)
    out.writeText(root.toString())
    return out
}

fun removeLastDatasetSample(context: Context): String? {
    val file = datasetFile(context)
    if (!file.exists() || file.length() == 0L) return null

    return try {
        val root = JSONObject(file.readText())
        val byDate = root.optJSONObject("by_date") ?: return null
        val dates = mutableListOf<String>()
        byDate.keys().forEach { dates += it }

        val latestDate = dates.sortedDescending().firstOrNull { date ->
            (byDate.optJSONArray(date)?.length() ?: 0) > 0
        } ?: return null

        val entries = byDate.optJSONArray(latestDate) ?: return null
        if (entries.length() == 0) return null

        val removedLabel = entries.optJSONObject(entries.length() - 1)?.optString("label")
        entries.remove(entries.length() - 1)
        if (entries.length() == 0) {
            byDate.remove(latestDate)
        } else {
            byDate.put(latestDate, entries)
        }

        file.writeText(root.toString())
        removedLabel
    } catch (_: Exception) {
        null
    }
}

fun shareDatasetJsonFile(context: Context): Boolean {
    try {
        val file = datasetFile(context)
        if (!file.exists() || file.length() == 0L) return false
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir dataset JSON"))
        return true
    } catch (_: Exception) {
        return false
    }
}

fun loadDatasetCountsByLabel(context: Context): Map<String, Int> {
    val file = datasetFile(context)
    if (!file.exists() || file.length() == 0L) return emptyMap()

    return try {
        val root = JSONObject(file.readText())
        val byDate = root.optJSONObject("by_date") ?: return emptyMap()
        val counts = linkedMapOf<String, Int>()

        byDate.keys().forEach { date ->
            val items = byDate.optJSONArray(date) ?: JSONArray()
            for (i in 0 until items.length()) {
                val label = items.optJSONObject(i)?.optString("label").orEmpty()
                if (label.isNotBlank()) {
                    counts[label] = (counts[label] ?: 0) + 1
                }
            }
        }

        counts.toList().sortedBy { displayTrainingLabel(it.first) }.toMap()
    } catch (_: Exception) {
        emptyMap()
    }
}
