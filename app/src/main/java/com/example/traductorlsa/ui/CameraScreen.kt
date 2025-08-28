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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
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

// ---------- modelos simples ----------
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
// -------------------------------------

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val overlayState = remember { mutableStateOf(OverlayData()) }
    var lastHandsAt by remember { mutableStateOf(0L) }
    var translatedText by rememberSaveable { mutableStateOf("") }
    var currentPrediction by remember { mutableStateOf<PredictionResult?>(null) }

    // métricas (se ocultan en training)
    var captureTime by remember { mutableStateOf(0L) }
    var inferTime by remember { mutableStateOf(0L) }
    var fpsValue by remember { mutableStateOf(0f) }
    var targetFramesUsed by remember { mutableStateOf(15) }

    // ---- TRAINING STATE ----
    var trainingMode by rememberSaveable { mutableStateOf(true) } // podés dejarlo en false si querés
    val collected = remember { mutableStateListOf<TrainingSample>() }
    var top3 by remember { mutableStateOf<List<PredictionResult>>(emptyList()) }
    var lastSeq by remember { mutableStateOf<List<List<Float>>>(emptyList()) }
    var tUsed by remember { mutableStateOf(0) }
    var dUsed by remember { mutableStateOf(0) }
    // visibilidad de botones: aparecen SOLO cuando terminó una predicción
    var showChoices by remember { mutableStateOf(false) }
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

    var frameCount by remember { mutableStateOf(0) }

    // ---------- Callbacks del engine ----------
    DisposableEffect(engine) {
        engine.onHands = { hands, w, h, rot, isFront ->
            lastHandsAt = SystemClock.uptimeMillis()
            overlayState.value = OverlayData(w, h, hands, rot, isFront)
        }

        engine.onPrediction = { prediction ->
            currentPrediction = prediction
            if (!trainingMode) {
                // Sólo hablar y acumular texto fuera de training
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
            if (trainingMode) {
                // Mientras se está capturando, ocultar opciones
                if (count > 0) showChoices = false
            }
        }

        engine.onCaptureStats = { cap, inf, fps, newTarget ->
            if (!trainingMode) { // 🔕 ocultar métricas si está training
                if (cap > 0) {
                    captureTime = cap
                    fpsValue = fps
                    targetFramesUsed = newTarget
                }
                if (inf > 0) inferTime = inf
            }
        }

        // 👉 Top-3 + features normalizados: acá sabemos que TERMINÓ la predicción
        engine.onTopPredictions = { preds, seq ->
            if (trainingMode) {
                top3 = preds
                lastSeq = seq.map { it.toList() }
                tUsed = seq.size
                dUsed = if (seq.isNotEmpty()) seq[0].size else 0
                showChoices = true // ← mostrar botones recién ahora
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
    // -----------------------------------------

    // Limpieza del overlay si se “van” las manos
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
    LaunchedEffect(hasPermission, lifecycleOwner) {
        if (!hasPermission) return@LaunchedEffect
        cameraManager.configure(
            lensFacingFront = true,
            analysisStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST,
            targetSize = Size(640, 480)
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

    // ================= UI =================
    Box(Modifier.fillMaxSize()) {

        // Preview
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                PreviewView(it).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    controller = camController
                    scaleX = -1f // espejo frontal
                }
            }
        )

        // Overlay landmarks
        com.example.traductorlsa.ui.overlay.HandLandmarksOverlay(overlay = overlayState.value)

        // Barra superior: switch + acciones dataset (sólo en training)
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
                        // limpiar UI de opciones al salir/entrar
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
                    Button(
                        onClick = {
                            if (collected.isEmpty()) {
                                Toast.makeText(context, "No hay muestras para exportar", Toast.LENGTH_SHORT).show()
                            } else {
                                val json = buildJson(collected, tUsed, dUsed)
                                val file = saveJson(context.getExternalFilesDir(null) ?: context.filesDir, json)
                                Toast.makeText(context, "Exportado: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Exportar JSON") }
                }
            }
        }

        // Indicador de captura (podemos ocultarlo si querés)
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

        // Métricas: 🔕 se ocultan en Training
        if (!trainingMode && (captureTime > 0 || inferTime > 0)) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (captureTime > 0) {
                    Text("⏱️ Captura: ${captureTime} ms", color = Color.White)
                    Text("📸 FPS efectivos: ${"%.1f".format(fpsValue)}", color = Color.White)
                    Text("🎯 targetFrames: $targetFramesUsed", color = Color.Yellow)
                }
                if (inferTime > 0) {
                    Text("🤖 Inferencia: ${inferTime} ms", color = Color.White)
                }
            }
        }

        // Panel inferior: cambia por modo
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            if (trainingMode) {
                TrainingPanel(
                    visible = showChoices,    // ← solo cuando hay predicción lista
                    top3 = top3,
                    onPick = { label ->
                        if (lastSeq.isEmpty()) {
                            Toast.makeText(context, "No hay secuencia disponible aún", Toast.LENGTH_SHORT).show()
                        } else {
                            collected += TrainingSample(label = label, seq = lastSeq)
                            // ocultar hasta la próxima predicción
                            showChoices = false
                            top3 = emptyList()
                            Toast.makeText(context, "Agregada muestra: $label (T=$tUsed, D=$dUsed)", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onClose = {
                        // cerrar manualmente el panel (por ejemplo si ninguna predicción aplica)
                        showChoices = false
                        top3 = emptyList()
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
    }
}

// ---------- UI de Training (3 botones + X para cerrar) ----------
@Composable
private fun TrainingPanel(
    visible: Boolean,
    top3: List<PredictionResult>,
    onPick: (String) -> Unit,
    onClose: () -> Unit
) {
    if (!visible) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.75f))
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header con título + X (arriba-derecha)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Seleccioná la etiqueta correcta:", color = Color.White)
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Cerrar",
                        tint = Color.White
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            if (top3.isEmpty()) {
                Text("Esperando predicción…", color = Color.White.copy(alpha = 0.7f))
            } else {
                top3.forEach { pred ->
                    val pct = (pred.confidence * 100).coerceIn(0f, 100f)
                    Button(
                        onClick = { onPick(pred.gesture) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("${pred.gesture}  (${String.format(Locale.US, "%.1f", pct)}%)")
                    }
                }
            }
        }
    }
}
// -----------------------------------------------

// ---------- Helpers JSON ----------
private fun buildJson(samples: List<TrainingSample>, T: Int, D: Int): String {
    val sb = StringBuilder()
    sb.append("{\"t\":").append(T).append(",\"d\":").append(D).append(",\"samples\":[")
    samples.forEachIndexed { i, s ->
        if (i > 0) sb.append(',')
        sb.append("{\"label\":\"")
            .append(s.label.replace("\"", "\\\""))
            .append("\",\"seq\":[")
        s.seq.forEachIndexed { ti, frame ->
            if (ti > 0) sb.append(',')
            sb.append('[')
            frame.forEachIndexed { di, v ->
                if (di > 0) sb.append(',')
                sb.append(v)
            }
            sb.append(']')
        }
        sb.append("]}")
    }
    sb.append("]}")
    return sb.toString()
}

private fun saveJson(baseDir: File, json: String): File {
    val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val file = File(baseDir, "lsa_samples_$ts.json")
    file.writeText(json)
    return file
}
// -----------------------------------
