package com.example.traductorlsa.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.SystemClock
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageAnalysis
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

import androidx.compose.material3.OutlinedTextField

import android.content.Context

import androidx.compose.material.icons.filled.List
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import kotlinx.coroutines.launch

import org.json.JSONObject

import android.content.Intent
import androidx.core.content.FileProvider

import android.os.Environment
import org.json.JSONArray

import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration

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
// -------------------------------------------


// Archivo único y ruta
private const val DATASET_FILE_NAME = "lsa_samples.json"

private fun loadAllLabels(context: Context): List<String> {
    // 1) words.json con "word_ids": [...]
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
    } catch (_: Exception) {
        // seguimos al fallback
    }

    // 2) Fallback a labels.txt (uno por línea)
    return try {
        context.assets.open("labels.txt").bufferedReader().use { br ->
            br.readLines().map { it.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
        }
    } catch (_: Exception) {
        emptyList()
    }
}

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val overlayState = remember { mutableStateOf(OverlayData()) }
    var lastHandsAt by remember { mutableStateOf(0L) }
    var translatedText by rememberSaveable { mutableStateOf("") }
    var currentPrediction by remember { mutableStateOf<PredictionResult?>(null) }

    // métricas (ocultas en training)
    var captureTime by remember { mutableStateOf(0L) }
    var inferTime by remember { mutableStateOf(0L) }
    var fpsValue by remember { mutableStateOf(0f) }
    var targetFramesUsed by remember { mutableStateOf(15) }

    // ---- TRAINING STATE ----
    var trainingMode by rememberSaveable { mutableStateOf(true) }
    val collected = remember { mutableStateListOf<TrainingSample>() }
    var top3 by remember { mutableStateOf<List<PredictionResult>>(emptyList()) }
    var lastSeq by remember { mutableStateOf<List<List<Float>>>(emptyList()) }
    var tUsed by remember { mutableStateOf(0) }
    var dUsed by remember { mutableStateOf(0) }
    var showChoices by remember { mutableStateOf(false) }

    // Etiquetas personalizadas cacheadas para reusar
    val customLabels = remember { mutableStateListOf<String>() }
    // ------------------------

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

    // Snackbar para "Deshacer"
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // Guardamos el índice del último agregado para poder revertirlo
    var lastAddedIndex by remember { mutableStateOf<Int?>(null) }

    var frameCount by remember { mutableStateOf(0) }

    val allBaseLabels = remember { mutableStateListOf<String>() }
    LaunchedEffect(Unit) {
        allBaseLabels.clear()
        allBaseLabels += loadAllLabels(context)   // ← ahora lee words.json
    }

    // -------- Callbacks del engine --------
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

        // Top-3 + features normalizados → listo para etiquetar
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
    // -------------------------------------

    // Limpia overlay si se “van” las manos
    LaunchedEffect(Unit) {
        while (true) {
            delay(250)
            val stale = SystemClock.uptimeMillis() - lastHandsAt > 600
            if (stale && overlayState.value.hands.isNotEmpty()) {
                overlayState.value = overlayState.value.copy(hands = emptyList())
            }
        }
    }

    // Cámara
    // ... (imports y variables de estado previos)

    // Obtenemos la configuración actual para detectar la orientación de inicio
    val configuration = LocalConfiguration.current

    // Cámara
    LaunchedEffect(hasPermission, lifecycleOwner) {
        if (!hasPermission) return@LaunchedEffect

        // Detectamos si estamos en modo Portrait (Vertical) al iniciar
        val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

        // Si es vertical, invertimos las dimensiones para pedir 480x640
        // Si es horizontal, mantenemos 640x480
        val targetSize = if (isPortrait) Size(480, 640) else Size(640, 480)

        cameraManager.configure(
            lensFacingFront = true,
            analysisStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST,
            targetSize = targetSize
        )

        // Sincronizamos también el engine, aunque tu código ya lo hace por defecto
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

    // ================= UI =================
    Box(Modifier.fillMaxSize()) {

        // Preview
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                // 🔁 Espejo a nivel Compose para que NO se pierda en recomposiciones
                .graphicsLayer { scaleX = -1f },

            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    controller = camController
                    // (opcional) también acá por si el OEM ignora el graphicsLayer
                    scaleX = -1f
                }
            },

            update = { pv ->
                // Rebindeos del controller pueden resetear transforms: lo re-aplicamos
                pv.controller = camController
                pv.scaleX = -1f
            }
        )


        // Overlay landmarks
        com.example.traductorlsa.ui.overlay.HandLandmarksOverlay(overlay = overlayState.value)

        // Barra superior
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Modo Training", color = Color.White)
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = trainingMode,
                    onCheckedChange = {
                        trainingMode = it
                        showChoices = false
                        top3 = emptyList()
                    }
                )
            }

            if (trainingMode) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Muestras: ${collected.size}", color = Color.Yellow)
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = {
                            collected.clear()
                            Toast.makeText(context, "Muestras limpiadas", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Limpiar") }

                    Spacer(Modifier.width(8.dp))
                    // Exportar JSON → append al archivo único del día
                    Button(
                        onClick = {
                            if (collected.isEmpty()) {
                                Toast.makeText(context, "No hay muestras para exportar", Toast.LENGTH_SHORT).show()
                            } else {
                                val f = appendSamplesByDate(context, collected, tUsed, dUsed)
                                Toast.makeText(context, "Guardado en: ${f.absolutePath}", Toast.LENGTH_LONG).show()
                                // (opcional) limpiar el buffer de sesión
                                collected.clear()
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Exportar JSON") }

                    Spacer(Modifier.width(8.dp))

                    // Compartir → también hace append previo y luego abre el chooser
                    Button(
                        onClick = {
                            if (collected.isEmpty()) {
                                Toast.makeText(context, "No hay muestras para compartir", Toast.LENGTH_SHORT).show()
                            } else {
                                exportAndShareJson(context, collected, tUsed, dUsed)
                                // (opcional) limpiar buffer
                                collected.clear()
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Compartir") }
                }
            }
            if (trainingMode && collected.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = {
                        if (collected.isNotEmpty()) {
                            collected.removeAt(collected.lastIndex)
                            Toast.makeText(context, "Se eliminó la última muestra", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Deshacer último") }
            }
        }

        // Indicador de captura (puedo ocultarlo en training si querés)
        if (frameCount > 0) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = { (frameCount / 15f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(8.dp),
                    color = Color.Cyan,
                )
                Text(
                    text = "Capturando: $frameCount / 15",
                    color = Color.White,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Métricas (ocultas en training) — omitidas aquí para simplificar

        // Panel inferior
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
                        if (lastSeq.isEmpty()) {
                            Toast.makeText(context, "No hay secuencia disponible aún", Toast.LENGTH_SHORT).show()
                        } else {
                            val sample = TrainingSample(label = label /* o newLabel */, seq = lastSeq)
                            collected += sample
// recordá el índice exacto que agregaste
                            val idx = collected.lastIndex
                            lastAddedIndex = idx

// ocultar panel hasta la próxima predicción (como ya hacías)
                            showChoices = false
                            top3 = emptyList()

// Snackbar con acción "Deshacer"
                            scope.launch {
                                val res = snack.showSnackbar(
                                    message = "Muestra guardada: ${sample.label}",
                                    actionLabel = "Deshacer",
                                    withDismissAction = true,
                                    duration = SnackbarDuration.Short
                                )
                                if (res == SnackbarResult.ActionPerformed) {
                                    // si no hubo otra inserción en el medio y el índice sigue válido, borramos esa muestra
                                    if (lastAddedIndex != null && lastAddedIndex!! in collected.indices) {
                                        collected.removeAt(lastAddedIndex!!)
                                        Toast.makeText(context, "Se deshizo la última muestra", Toast.LENGTH_SHORT).show()
                                    }
                                    lastAddedIndex = null
                                } else {
                                    // No se deshizo; limpiamos el puntero
                                    lastAddedIndex = null
                                }
                            }
                        }
                    },
                    onClose = {
                        showChoices = false
                        top3 = emptyList()
                    },
                    onCreateAndPick = { newLabel ->
                        if (newLabel.isBlank()) {
                            Toast.makeText(context, "La etiqueta no puede estar vacía", Toast.LENGTH_SHORT).show()
                        } else if (lastSeq.isEmpty()) {
                            Toast.makeText(context, "No hay secuencia disponible aún", Toast.LENGTH_SHORT).show()
                        } else {
                            if (customLabels.none { it.equals(newLabel, ignoreCase = true) }) {
                                customLabels += newLabel
                            }
                            val sample = TrainingSample(label = newLabel, seq = lastSeq)
                            collected += sample
// recordá el índice exacto que agregaste
                            val idx = collected.lastIndex
                            lastAddedIndex = idx

// ocultar panel hasta la próxima predicción (como ya hacías)
                            showChoices = false
                            top3 = emptyList()

// Snackbar con acción "Deshacer"
                            scope.launch {
                                val res = snack.showSnackbar(
                                    message = "Muestra guardada: ${sample.label}",
                                    actionLabel = "Deshacer",
                                    withDismissAction = true,
                                    duration = SnackbarDuration.Short
                                )
                                if (res == SnackbarResult.ActionPerformed) {
                                    // si no hubo otra inserción en el medio y el índice sigue válido, borramos esa muestra
                                    if (lastAddedIndex != null && lastAddedIndex!! in collected.indices) {
                                        collected.removeAt(lastAddedIndex!!)
                                        Toast.makeText(context, "Se deshizo la última muestra", Toast.LENGTH_SHORT).show()
                                    }
                                    lastAddedIndex = null
                                } else {
                                    // No se deshizo; limpiamos el puntero
                                    lastAddedIndex = null
                                }
                            }
                        }
                    }
                )
            } else {
                com.example.traductorlsa.ui.widgets.PredictionCard(currentPrediction)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Texto traducido:", color = Color.White.copy(alpha = 0.7f))
                        Text(
                            text = translatedText.ifEmpty { "Esperando señas..." },
                            color = Color.White
                        )
                    }
                }
            }
        }
        // Snackbar (aparece centrado-abajo)
        SnackbarHost(
            hostState = snack,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )
    }
}

// ---------- UI Training: Top-3 (grid 1×3) + X + “Agregar” + Chips ----------
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

    // Sheet de “todas las etiquetas”
    var showAll by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val scope = rememberCoroutineScope()
    var search by remember { mutableStateOf("") }
    val filtered = remember(allLabels, search) {
        val q = search.lowercase()
        if (q.isBlank()) allLabels else allLabels.filter { it.lowercase().contains(q) }
    }

    if (showAll) {
        // Abrir ya expandido
        LaunchedEffect(Unit) { scope.launch { sheetState.expand() } }

        ModalBottomSheet(
            onDismissRequest = { showAll = false },
            sheetState = sheetState,
            // un poco translúcido para no tapar 100%
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Elegí una etiqueta", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    singleLine = true,
                    placeholder = { Text("Buscar…") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                // Lista scrolleable y clickeable
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) {
                    items(filtered) { label ->
                        ListItem(
                            headlineContent = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onPick(label)
                                    showAll = false
                                }
                        )
                        Divider()
                    }
                }
                if (filtered.isEmpty()) {
                    Text(
                        "No hay resultados",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // Card principal (Top-3 / Agregar / Chips / Lista)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.65f))
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (addMode) "Nueva etiqueta para esta seña:" else "Seleccioná la etiqueta correcta:",
                    color = Color.White
                )
                Row {
                    IconButton(onClick = { showAll = true }) {
                        Icon(Icons.Filled.List, contentDescription = "Ver todas", tint = Color.White)
                    }
                    IconButton(onClick = { addMode = !addMode }) {
                        Icon(Icons.Filled.Add, contentDescription = "Agregar etiqueta", tint = Color.White)
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = Color.White)
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            if (addMode) {
                OutlinedTextField(
                    value = newLabel,
                    onValueChange = { newLabel = it.trim() },
                    singleLine = true,
                    placeholder = { Text("Ingresá nueva etiqueta…") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            onCreateAndPick(newLabel)
                            newLabel = ""
                            addMode = false
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Guardar") }
                    OutlinedButton(
                        onClick = { newLabel = ""; addMode = false },
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Cancelar") }
                }
            } else {
                // Top-3 en una sola fila
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (0 until 3).forEach { i ->
                        val pred = top3.getOrNull(i)
                        Button(
                            onClick = { pred?.let { onPick(it.gesture) } },
                            enabled = pred != null,
                            modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            val pct = ((pred?.confidence ?: 0f) * 100).coerceIn(0f, 100f)
                            Text(
                                text = if (pred != null)
                                    "${pred.gesture}  (${String.format(Locale.US, "%.1f", pct)}%)"
                                else "—",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = Color.White
                            )
                        }
                    }
                }

                // Chips de etiquetas personalizadas (si hay)
                if (customLabels.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        customLabels.forEach { label ->
                            OutlinedButton(
                                onClick = { onPick(label) },
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.heightIn(min = 36.dp)
                            ) {
                                Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}
// -------------------------------------------------------------------------

// ---------- Helpers JSON ----------

private fun datasetFile(context: Context): File {
    val base = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
    return File(base, DATASET_FILE_NAME)
}

// Cargar JSON actual o iniciar uno nuevo con schema {t, d, by_date:{}}
private fun loadOrInitDataset(context: Context, T: Int, D: Int): JSONObject {
    val f = datasetFile(context)
    if (!f.exists() || f.length() == 0L) {
        return JSONObject().apply {
            put("t", T)
            put("d", D)
            put("by_date", JSONObject())
        }
    }
    return try {
        val obj = JSONObject(f.readText())
        // si no trae campos, los inicializamos
        if (!obj.has("t")) obj.put("t", T)
        if (!obj.has("d")) obj.put("d", D)
        if (!obj.has("by_date")) obj.put("by_date", JSONObject())
        obj
    } catch (e: Exception) {
        // archivo corrupto → reinicio
        JSONObject().apply {
            put("t", T)
            put("d", D)
            put("by_date", JSONObject())
        }
    }
}

// Append de muestras al día actual (YYYY-MM-DD)
private fun appendSamplesByDate(context: Context, samples: List<TrainingSample>, T: Int, D: Int): File {
    if (samples.isEmpty()) return datasetFile(context)

    val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val root = loadOrInitDataset(context, T, D)

    // Si cambió T o D, actualizamos metadatos (o podrías abortar si querés estricta compatibilidad)
    root.put("t", T)
    root.put("d", D)

    val byDate = root.getJSONObject("by_date")
    val arr: JSONArray = if (byDate.has(today)) byDate.getJSONArray(today) else JSONArray()

    // Convertir nuestras muestras a JSON y agregarlas
    samples.forEach { s ->
        val seqJson = JSONArray()
        s.seq.forEach { frame ->
            val fArr = JSONArray()
            frame.forEach { v -> fArr.put(v) }
            seqJson.put(fArr)
        }
        val sampleObj = JSONObject()
            .put("label", s.label)
            .put("seq", seqJson)
        arr.put(sampleObj)
    }

    byDate.put(today, arr)
    root.put("by_date", byDate)

    val out = datasetFile(context)
    out.writeText(root.toString())
    return out
}

// Compartir SIEMPRE el archivo único, asegurando append previo
private fun exportAndShareJson(context: Context, samples: List<TrainingSample>, T: Int, D: Int) {
    if (samples.isEmpty()) {
        Toast.makeText(context, "No hay muestras para exportar", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val file = appendSamplesByDate(context, samples, T, D)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Compartir dataset JSON"))

    } catch (e: Exception) {
        Toast.makeText(context, "Error al exportar/compartir: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
// -----------------------------------
