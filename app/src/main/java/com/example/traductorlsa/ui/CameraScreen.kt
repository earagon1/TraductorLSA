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
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import com.example.traductorlsa.camera.CameraManager
import com.example.traductorlsa.camera.FrameAnalyzer
import com.example.traductorlsa.detection.HandTrackerImpl
import com.example.traductorlsa.features.FeatureBuilderImpl
import com.example.traductorlsa.features.SequenceBufferImpl
import com.example.traductorlsa.ml.GestureEngine
import com.example.traductorlsa.ml.LabelProviderImpl
import com.example.traductorlsa.ml.TFLiteClassifier
import com.example.traductorlsa.model.NormPoint
import com.example.traductorlsa.model.PredictionResult
import com.example.traductorlsa.speech.SpeechManager
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

private fun loadAllLabels(context: Context): List<String> {
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

private fun normalizeTrainingLabel(input: String): String {
    val lower = input.trim().lowercase(Locale.getDefault())
    val noAccents = Normalizer.normalize(lower, Normalizer.Form.NFD)
        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")

    return noAccents
        .replace("[^a-z0-9_ ]".toRegex(), "")
        .replace("\\s+".toRegex(), "_")
        .trim('_')
}

private fun displayTrainingLabel(label: String): String =
    label.replace("_", " ").replace("\\s+".toRegex(), " ").trim()

private fun findTrainingWordImageInAssets(context: Context, word: String): String? {
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

private fun loadTrainingWordOptions(context: Context): List<TrainingWordOption> =
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
fun TrainingCaptureScreen(navController: NavHostController) {
    CameraScreen(mode = CameraScreenMode.TRAINING, onBack = { navController.popBackStack() })
}

@Composable
fun CameraScreen(
    mode: CameraScreenMode = CameraScreenMode.TRANSLATE,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val trainingMode = mode == CameraScreenMode.TRAINING
    var isFrontCamera by rememberSaveable { mutableStateOf(true) }

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

    val handTracker = remember { HandTrackerImpl(context) }
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

    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var frameCount by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        officialWords.clear()
        officialWords += loadTrainingWordOptions(context)
    }

    LaunchedEffect(trainingMode, selectedWord) {
        if (trainingMode && selectedWord == null) showWordPicker = true
    }

    DisposableEffect(engine, trainingMode, selectedWord) {
        engine.onHands = { hands, w, h, rot, isFront ->
            lastHandsAt = SystemClock.uptimeMillis()
            overlayState.value = OverlayData(w, h, hands, rot, isFront)
        }

        engine.onPrediction = { prediction ->
            if (!trainingMode) {
                speechManager.speak(prediction.gesture)
                if (prediction.confidence > 0.5f &&
                    prediction.gesture != "Unknown" &&
                    prediction.gesture != "Sin datos"
                ) {
                    val last = recentTranslations.lastOrNull()
                    if (last != prediction.gesture) {
                        currentTranslation = prediction.gesture
                        recentTranslations = (recentTranslations + prediction.gesture).takeLast(3)
                    }
                }
            }
        }

        engine.onCaptureProgress = { count, _ ->
            frameCount = count
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
                    scope.launch {
                        val autoDismiss = launch {
                            delay(2000)
                            snack.currentSnackbarData?.dismiss()
                        }
                        val result = snack.showSnackbar(
                            message = "Muestra guardada para ${displayTrainingLabel(selected.label)}",
                            actionLabel = "Deshacer",
                            duration = SnackbarDuration.Indefinite
                        )
                        autoDismiss.cancel()
                        if (result == SnackbarResult.ActionPerformed) {
                            val removed = removeLastDatasetSample(context)
                            if (removed != null) {
                                sessionSavedCount = (sessionSavedCount - 1).coerceAtLeast(0)
                                val undoDismiss = launch {
                                    delay(2000)
                                    snack.currentSnackbarData?.dismiss()
                                }
                                snack.showSnackbar(
                                    message = "Se eliminÃ³ la Ãºltima muestra de ${displayTrainingLabel(removed)}",
                                    duration = SnackbarDuration.Indefinite
                                )
                                undoDismiss.cancel()
                            }
                        }
                    }
                }
            }
        }

        onDispose {
            engine.onHands = null
            engine.onPrediction = null
            engine.onCaptureProgress = null
            engine.onCaptureStats = null
            engine.onTopPredictions = null
            speechManager.release()
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
    LaunchedEffect(hasPermission, lifecycleOwner, isFrontCamera) {
        if (!hasPermission) return@LaunchedEffect
        val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        val targetSize = if (isPortrait) Size(480, 640) else Size(640, 480)

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

        com.example.traductorlsa.ui.overlay.HandLandmarksOverlay(overlay = overlayState.value)

        // Barra superior
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
            }

            if (trainingMode) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar entrenamiento", tint = Color.White)
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color.Black.copy(alpha = 0.40f)
                ) {
                    IconButton(onClick = { isFrontCamera = !isFrontCamera }) {
                        Icon(
                            Icons.Default.Cameraswitch,
                            contentDescription = if (isFrontCamera) "Cambiar a cámara trasera" else "Cambiar a cámara frontal",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        if (trainingMode && selectedWord != null && !overlayState.value.hands.isNotEmpty() && frameCount == 0) {
            TrainingCenterHint()
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            if (trainingMode) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TrainingCapturePanel(
                        selectedWord = selectedWord,
                        sessionSavedCount = sessionSavedCount,
                        frameCount = frameCount,
                        hasHands = overlayState.value.hands.isNotEmpty(),
                        onShowReference = { showReference = true },
                        onChangeWord = { showWordPicker = true }
                    )
                }
            } else {
                com.example.traductorlsa.ui.widgets.PredictionCard(
                    currentTranslation = currentTranslation,
                    recentTranslations = recentTranslations
                )
            }
        }

        if (trainingMode && showWordPicker) {
            TrainingWordPickerSheet(
                words = (officialWords + customWords).sortedBy { displayTrainingLabel(it.label) },
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

@Composable
private fun TrainingCapturePanel(
    selectedWord: TrainingWordOption?,
    sessionSavedCount: Int,
    frameCount: Int,
    hasHands: Boolean,
    onShowReference: () -> Unit,
    onChangeWord: () -> Unit
) {
    var showHelp by remember { mutableStateOf(false) }
    val trainingWord = selectedWord?.let { displayTrainingLabel(it.label).uppercase(Locale.getDefault()) } ?: "SIN SEÑA"

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 560.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFF111827).copy(alpha = 0.92f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = buildAnnotatedString {
                        append("Entrenamiento: ")
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        append(trainingWord)
                        pop()
                    },
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Box {
                    TextButton(
                        onClick = { showHelp = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text("?", color = Color.White)
                    }
                    DropdownMenu(
                        expanded = showHelp,
                        onDismissRequest = { showHelp = false }
                    ) {
                        val helpText = if (selectedWord?.isOfficial == true) {
                            "Usá la imagen de referencia si necesitás recordar cómo se hace la seña."
                        } else {
                            "Usá una seña real de LSA. Si todavía no hay referencia oficial, practicá con una seña válida."
                        }
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = helpText,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            onClick = { showHelp = false }
                        )
                    }
                }
            }

            if (frameCount > 0) {
                LinearProgressIndicator(
                    progress = { (frameCount / 15f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = Color(0xFF34D399),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onChangeWord,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (selectedWord == null) "Elegir seña" else "Cambiar")
                }

                Button(
                    onClick = onShowReference,
                    enabled = selectedWord?.imageAssetPath != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Ver referencia")
                }
            }

            Text(
                text = "$sessionSavedCount muestras guardadas en esta sesión.",
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun TrainingCenterHint() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.Black.copy(alpha = 0.45f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PanTool,
                    contentDescription = null,
                    tint = Color.White
                )
                Text(
                    text = "Mostrá la mano frente a la cámara",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrainingWordPickerSheet(
    words: List<TrainingWordOption>,
    onDismiss: () -> Unit,
    onSelect: (TrainingWordOption) -> Unit,
    onCreateWord: (String) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var addMode by remember { mutableStateOf(false) }
    var newWord by remember { mutableStateOf("") }
    val filtered = remember(words, search) {
        val query = search.trim().lowercase()
        words.filter { displayTrainingLabel(it.label).lowercase().contains(query) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Elegí la seña a entrenar", style = MaterialTheme.typography.titleMedium)

            if (!addMode) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Buscar seña") }
                )

                OutlinedButton(onClick = { addMode = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Agregar nueva seña")
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filtered) { word ->
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(word) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(displayTrainingLabel(word.label), style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        if (word.isOfficial) "Seña oficial" else "Nueva seña propuesta",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                if (word.imageAssetPath != null) {
                                    Icon(Icons.Default.Image, contentDescription = null)
                                }
                            }
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = newWord,
                    onValueChange = { newWord = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Nombre de la nueva seña") }
                )
                Text(
                    "Esta palabra quedará disponible para entrenamiento aunque todavía no tenga una imagen oficial.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Usá únicamente una seña real de LSA.",
                    style = MaterialTheme.typography.bodySmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { addMode = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = { onCreateWord(newWord) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Guardar")
                    }
                }
            }
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
