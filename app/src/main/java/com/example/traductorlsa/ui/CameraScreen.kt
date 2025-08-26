
package com.example.traductorlsa.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.SystemClock
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageAnalysis
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.traductorlsa.model.PredictionResult
import com.example.traductorlsa.ml.TFLiteClassifier
import com.example.traductorlsa.model.NormPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import androidx.compose.ui.Alignment
import com.example.traductorlsa.speech.SpeechManager


data class OverlayData(
    val imgW: Int = 0,
    val imgH: Int = 0,
    val hands: List<List<NormPoint>> = emptyList(),
    val rotationDeg: Int = 0,
    val isFront: Boolean = true
)

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val overlayState = remember { mutableStateOf(OverlayData()) }
    var lastHandsAt by remember { mutableStateOf(0L) }
    var translatedText by rememberSaveable { mutableStateOf("") }
    var currentPrediction by remember { mutableStateOf<PredictionResult?>(null) }

    var captureTime by remember { mutableStateOf(0L) }
    var inferTime by remember { mutableStateOf(0L) }
    var fpsValue by remember { mutableStateOf(0f) }
    var targetFramesUsed by remember { mutableStateOf(15) }


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

    var frameCount by remember { mutableStateOf(0) }

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


    DisposableEffect(engine) {
        engine.onHands = { hands, w, h, rot, isFront ->
            lastHandsAt = SystemClock.uptimeMillis()
            overlayState.value = OverlayData(w, h, hands, rot, isFront)
        }

        engine.onPrediction = { prediction ->
            currentPrediction = prediction
            speechManager.speak(prediction.gesture) // 🔊 decir palabra

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

        engine.onCaptureProgress = { count, target ->
            frameCount = count
        }

        engine.onCaptureStats = { cap, inf, fps, newTarget ->
            if (cap > 0) {
                captureTime = cap
                fpsValue = fps
                targetFramesUsed = newTarget
            }
            if (inf > 0) inferTime = inf
        }



        onDispose {
            engine.onHands = null
            engine.onPrediction = null
            engine.onCaptureProgress = null
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


    Box(Modifier.fillMaxSize()) {

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                PreviewView(it).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    controller = camController
                    scaleX = -1f  // mantener espejo solo en frontal
                }
            }
        )

        com.example.traductorlsa.ui.overlay.HandLandmarksOverlay(overlay = overlayState.value)

        // 🔵 Indicador de captura
        if (frameCount > 0) {
            Column(
                Modifier.fillMaxWidth().padding(top = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = { frameCount / 15f },
                    modifier = Modifier.fillMaxWidth(0.6f).height(8.dp),
                    color = Color.Cyan,
                )
                Text(
                    text = "Capturando: $frameCount / 15",
                    color = Color.White,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        if (captureTime > 0 || inferTime > 0) {
            Column(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (captureTime > 0) {
                    Text("⏱️ Captura: ${captureTime} ms", color = Color.White)
                    Text("📸 FPS efectivos: ${"%.1f".format(fpsValue)}", color = Color.White)
                    Text("🎯 Nuevo targetFrames ajustado: $targetFramesUsed", color = Color.Yellow)
                }
                if (inferTime > 0) {
                    Text("🤖 Inferencia: ${inferTime} ms", color = Color.White)
                }
            }
        }




        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            com.example.traductorlsa.ui.widgets.PredictionCard(currentPrediction)

            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
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
