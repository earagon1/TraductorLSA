package com.example.traductorlsa.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Environment
import android.os.SystemClock
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageAnalysis
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
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

// Archivo único y ruta
private const val DATASET_FILE_NAME = "lsa_samples.json"

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

    val overlayState = remember { mutableStateOf(OverlayData()) }
    var lastHandsAt by remember { mutableStateOf(0L) }
    var translatedText by rememberSaveable { mutableStateOf("") }
    var currentPrediction by remember { mutableStateOf<PredictionResult?>(null) }

    // métricas
    var captureTime by remember { mutableStateOf(0L) }
    var inferTime by remember { mutableStateOf(0L) }
    var fpsValue by remember { mutableStateOf(0f) }
    var targetFramesUsed by remember { mutableStateOf(15) }

    // ---- TRAINING STATE ----
    val collected = remember { mutableStateListOf<TrainingSample>() }
    var top3 by remember { mutableStateOf<List<PredictionResult>>(emptyList()) }
    var lastSeq by remember { mutableStateOf<List<List<Float>>>(emptyList()) }
    var tUsed by remember { mutableStateOf(0) }
    var dUsed by remember { mutableStateOf(0) }
    var showChoices by remember { mutableStateOf(false) }
    val customLabels = remember { mutableStateListOf<String>() }

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
    var lastAddedIndex by remember { mutableStateOf<Int?>(null) }
    var frameCount by remember { mutableStateOf(0) }
    val allBaseLabels = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        allBaseLabels.clear()
        allBaseLabels += loadAllLabels(context)
    }

    DisposableEffect(engine) {
        engine.onHands = { hands, w, h, rot, isFront ->
            lastHandsAt = SystemClock.uptimeMillis()
            overlayState.value = OverlayData(w, h, hands, rot, isFront)
        }

        engine.onPrediction = { prediction ->
            currentPrediction = prediction
            if (!trainingMode) {
                speechManager.speak(prediction.gesture)
                if (prediction.confidence > 0.5f &&
                    prediction.gesture != "Unknown" &&
                    prediction.gesture != "Sin datos"
                ) {
                    val last = translatedText.split(" ").lastOrNull()
                    if (last != prediction.gesture) {
                        translatedText =
                            if (translatedText.isEmpty()) prediction.gesture
                            else "$translatedText ${prediction.gesture}"
                    }
                }
            }
        }

        engine.onCaptureProgress = { count, _ ->
            frameCount = count
            if (trainingMode && count > 0) showChoices = false
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

        engine.onTopPredictions = { preds, seq ->
            if (trainingMode) {
                top3 = preds
                lastSeq = seq.map { it.toList() }
                tUsed = seq.size
                dUsed = if (seq.isNotEmpty()) seq[0].size else 0
                showChoices = true
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
    LaunchedEffect(hasPermission, lifecycleOwner) {
        if (!hasPermission) return@LaunchedEffect
        val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        val targetSize = if (isPortrait) Size(480, 640) else Size(640, 480)

        cameraManager.configure(
            lensFacingFront = true,
            analysisStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST,
            targetSize = targetSize
        )
        engine.setCameraFacing(isFront = true)
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
                .graphicsLayer { scaleX = -1f },
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    controller = camController
                    scaleX = -1f
                }
            },
            update = { pv ->
                pv.controller = camController
                pv.scaleX = -1f
            }
        )

        com.example.traductorlsa.ui.overlay.HandLandmarksOverlay(overlay = overlayState.value)

        // Barra superior
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
            }

            if (trainingMode) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Muestras: ${collected.size}", color = Color.Yellow)
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { collected.clear() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Limpiar", tint = Color.White)
                    }
                    IconButton(onClick = {
                        if (collected.isNotEmpty()) {
                            appendSamplesByDate(context, collected, tUsed, dUsed)
                            collected.clear()
                            Toast.makeText(context, "Exportado", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Guardar", tint = Color.White)
                    }
                    IconButton(onClick = {
                        if (collected.isNotEmpty()) {
                            exportAndShareJson(context, collected, tUsed, dUsed)
                            collected.clear()
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Compartir", tint = Color.White)
                    }
                }
            } else {
                Text("Traducir Señas", color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
        }

        if (frameCount > 0) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 64.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = { (frameCount / 15f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(0.6f).height(8.dp),
                    color = Color.Cyan,
                )
                Text("Capturando: $frameCount / 15", color = Color.White, modifier = Modifier.padding(top = 4.dp))
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            if (trainingMode) {
                TrainingPanel(
                    visible = showChoices,
                    top3 = top3,
                    allLabels = (allBaseLabels + customLabels).distinct().sorted(),
                    customLabels = customLabels,
                    onPick = { label ->
                        if (lastSeq.isNotEmpty()) {
                            val sample = TrainingSample(label = label, seq = lastSeq)
                            collected += sample
                            lastAddedIndex = collected.lastIndex
                            showChoices = false
                            top3 = emptyList()
                            scope.launch {
                                val res = snack.showSnackbar("Guardado: $label", "Deshacer")
                                if (res == SnackbarResult.ActionPerformed && lastAddedIndex!! in collected.indices) {
                                    collected.removeAt(lastAddedIndex!!)
                                }
                            }
                        }
                    },
                    onClose = {
                        showChoices = false
                        top3 = emptyList()
                    },
                    onCreateAndPick = { newLabel ->
                        if (newLabel.isNotBlank() && lastSeq.isNotEmpty()) {
                            if (customLabels.none { it.equals(newLabel, ignoreCase = true) }) customLabels += newLabel
                            val sample = TrainingSample(label = newLabel, seq = lastSeq)
                            collected += sample
                            lastAddedIndex = collected.lastIndex
                            showChoices = false
                            top3 = emptyList()
                            scope.launch {
                                val res = snack.showSnackbar("Guardado: $newLabel", "Deshacer")
                                if (res == SnackbarResult.ActionPerformed && lastAddedIndex!! in collected.indices) {
                                    collected.removeAt(lastAddedIndex!!)
                                }
                            }
                        }
                    }
                )
            } else {
                com.example.traductorlsa.ui.widgets.PredictionCard(currentPrediction)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Texto traducido:", color = Color.White.copy(alpha = 0.7f))
                        Text(translatedText.ifEmpty { "Esperando señas..." }, color = Color.White)
                    }
                }
            }
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
                OutlinedTextField(value = search, onValueChange = { search = it }, placeholder = { Text("Buscar…") }, modifier = Modifier.fillMaxWidth())
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
                Text(if (addMode) "Nueva etiqueta:" else "Seleccioná la etiqueta:", color = Color.White)
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
                            Text(pred?.gesture ?: "—", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

private fun datasetFile(context: Context): File {
    val base = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
    return File(base, DATASET_FILE_NAME)
}

private fun loadOrInitDataset(context: Context, T: Int, D: Int): JSONObject {
    val f = datasetFile(context)
    if (!f.exists() || f.length() == 0L) return JSONObject().apply { put("t", T); put("d", D); put("by_date", JSONObject()) }
    return try {
        JSONObject(f.readText()).apply { if (!has("by_date")) put("by_date", JSONObject()) }
    } catch (e: Exception) {
        JSONObject().apply { put("t", T); put("d", D); put("by_date", JSONObject()) }
    }
}

private fun appendSamplesByDate(context: Context, samples: List<TrainingSample>, T: Int, D: Int): File {
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

private fun exportAndShareJson(context: Context, samples: List<TrainingSample>, T: Int, D: Int) {
    try {
        val file = appendSamplesByDate(context, samples, T, D)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir dataset JSON"))
    } catch (_: Exception) {}
}
