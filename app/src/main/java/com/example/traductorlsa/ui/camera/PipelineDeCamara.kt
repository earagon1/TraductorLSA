package com.example.traductorlsa.ui.camera

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Size
import androidx.camera.core.ImageAnalysis
import androidx.camera.view.LifecycleCameraController
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.example.traductorlsa.camera.CameraManager
import com.example.traductorlsa.camera.FrameAnalyzer
import com.example.traductorlsa.detection.HandTracker
import com.example.traductorlsa.detection.TrackerPerezoso
import com.example.traductorlsa.detection.trackerDesdeConfig
import com.example.traductorlsa.features.FeatureBuilderImpl
import com.example.traductorlsa.ml.GestureEngine
import com.example.traductorlsa.ml.LabelProviderImpl
import com.example.traductorlsa.ml.Metricas
import com.example.traductorlsa.ml.TFLiteClassifier
import com.example.traductorlsa.settings.CalidadAnalisis
import com.example.traductorlsa.speech.SpeechManager
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Los recursos nativos de la cámara: CameraX, MediaPipe, TFLite, la voz y el hilo donde
 * corren. Duran lo que dura la pantalla y se cierran con ella (ver [rememberPipelineDeCamara]).
 * Salieron de ui/CameraScreen.kt (T-A20); el estado de la pantalla está en [CameraViewModel].
 */
class PipelineDeCamara internal constructor(
    val camController: LifecycleCameraController,
    internal val cameraManager: CameraManager,
    internal val executor: ExecutorService,
    // Se pone al salir de la pantalla: el analizador deja pasar los frames que CameraX
    // ya tenía en camino sin procesarlos.
    internal val pantallaCerrada: AtomicBoolean,
    internal val handTracker: HandTracker,
    val classifier: TFLiteClassifier,
    val speechManager: SpeechManager,
    val engine: GestureEngine,
)

/**
 * Crea el pipeline y lo cierra cuando la pantalla sale.
 *
 * La sensibilidad se toma al entrar a la pantalla y no en vivo: recrear el detector
 * recarga los 8 MB del modelo, y el deslizador de Ajustes dispara en cada movimiento.
 * Cambiarla tiene efecto la próxima vez que se abre.
 */
@Composable
fun rememberPipelineDeCamara(sensibilidad: Float): PipelineDeCamara {
    val context = LocalContext.current
    // Banco de medición (solo debug, prefs "banco" escritas desde la PC): con qué delegado
    // corre MediaPipe y, si "comparar" está puesto, otra configuración que corre sobre el
    // mismo frame para medir tiempo y diferencia de landmarks.
    val banco = remember {
        if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0)
            context.getSharedPreferences("banco", Context.MODE_PRIVATE) else null
    }
    val pipeline = remember {
        val camController = LifecycleCameraController(context)
        // MediaPipe en GPU (con vuelta automática a CPU si el teléfono no la tiene): en el
        // Galaxy A55 baja de 99 a 70 ms por frame con los mismos landmarks (misma cantidad
        // de manos en el 99,7 % de los frames, puntos a 0,2 % del ancho; 2026-10-18). Se
        // crea en el hilo de la cámara, que es donde corre.
        val config = banco?.getString("principal", null) ?: CONFIG_MEDIAPIPE
        val handTracker = TrackerPerezoso { trackerDesdeConfig(context, sensibilidad, config) }
        val labelProvider = LabelProviderImpl(context)
        val classifier = TFLiteClassifier(context, labelProvider)
        PipelineDeCamara(
            camController = camController,
            cameraManager = CameraManager(context, camController),
            executor = Executors.newSingleThreadExecutor(),
            pantallaCerrada = AtomicBoolean(false),
            handTracker = handTracker,
            classifier = classifier,
            speechManager = SpeechManager(context),
            engine = GestureEngine(
                handTracker = handTracker,
                featureBuilder = FeatureBuilderImpl(),
                classifier = classifier,
                labelProvider = labelProvider,
            ),
        )
    }

    DisposableEffect(pipeline) {
        val engine = pipeline.engine
        val comparar = banco?.getString("comparar", null)
        if (comparar != null) {
            // "entrada" = "jpeg80": el comparador ve el mismo frame pasado por JPEG de calidad
            // 80, como el pipeline anterior (YuvToRgbConverter). Mide qué cambió al sacarlo.
            if (banco.getString("entrada", null) == "jpeg80") {
                engine.entradaComparador = { bmp ->
                    val bytes = ByteArrayOutputStream().also { bmp.compress(Bitmap.CompressFormat.JPEG, 80, it) }.toByteArray()
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
            }
            val archivo = File(context.filesDir, "metricas/comparacion_mp.jsonl").apply { parentFile?.mkdirs() }
            engine.comparador = TrackerPerezoso { trackerDesdeConfig(context, sensibilidad, comparar) }
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
                runCatching { pipeline.executor.execute { comparador.close() } }.onFailure { comparador.close() }
            }
            engine.entradaComparador = null
            engine.onComparacion = null
        }
    }

    // Los recursos nativos duran lo que dura la pantalla, no lo que dura la seña elegida:
    // por eso el cierre va en su propio efecto sin claves. Cuando estaba en un efecto con
    // la seña entre sus claves, elegir una seña cerraba el HandLandmarker mientras la
    // cámara le seguía mandando cuadros, y la app se caía.
    //
    // Al salir (auditoría, TL-A1/A2): primero se corta el flujo de frames y después se
    // libera todo en el hilo de la cámara. Como ese hilo es uno solo, el cierre corre
    // después del frame que estuviera procesando, no en paralelo con él. Al final se apaga
    // el executor, que antes quedaba vivo (un hilo por cada vez que se abría la cámara).
    DisposableEffect(Unit) {
        onDispose {
            pipeline.pantallaCerrada.set(true)
            pipeline.camController.clearImageAnalysisAnalyzer()
            pipeline.camController.unbind()
            pipeline.speechManager.release()
            pipeline.executor.execute {
                pipeline.handTracker.close()
                pipeline.classifier.close()
            }
            pipeline.executor.shutdown()
        }
    }
    return pipeline
}

/**
 * Vincula la cámara a la pantalla y le conecta el análisis. Se rehace al cambiar de cámara
 * o de calidad. Con un modelo que no corresponde (X3) no se abre.
 */
@Composable
fun VincularCamara(pipeline: PipelineDeCamara, hayPermiso: Boolean, frontal: Boolean, calidad: CalidadAnalisis) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    LaunchedEffect(hayPermiso, lifecycleOwner, frontal, calidad) {
        if (!hayPermiso || pipeline.classifier.problemas.isNotEmpty()) return@LaunchedEffect
        val vertical = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        val tamano = if (vertical) Size(calidad.ladoCorto, calidad.ladoLargo) else Size(calidad.ladoLargo, calidad.ladoCorto)

        pipeline.cameraManager.configure(
            lensFacingFront = frontal,
            analysisStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST,
            targetSize = tamano,
        )
        pipeline.engine.setCameraFacing(isFront = frontal)
        pipeline.camController.bindToLifecycle(lifecycleOwner)
        pipeline.camController.clearImageAnalysisAnalyzer()

        val analyzer = FrameAnalyzer(context) { bitmap, rotationDeg, ts, convNanos, yaDerecho ->
            pipeline.engine.process(bitmap, rotationDeg, ts, convNanos, yaDerecho)
        }
        pipeline.camController.setImageAnalysisAnalyzer(pipeline.executor) { imageProxy ->
            if (pipeline.pantallaCerrada.get()) imageProxy.close() else analyzer.analyze(imageProxy)
        }
    }
}
