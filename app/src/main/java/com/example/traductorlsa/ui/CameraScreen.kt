package com.example.traductorlsa.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.traductorlsa.settings.ajustesSenar
import com.example.traductorlsa.settings.repositorioAjustes
import com.example.traductorlsa.ui.camera.AlmacenDelTelefono
import com.example.traductorlsa.ui.camera.AvisoModeloInvalido
import com.example.traductorlsa.ui.camera.BotonRedondo
import com.example.traductorlsa.ui.camera.CabeceraCamara
import com.example.traductorlsa.ui.camera.CameraScreenMode
import com.example.traductorlsa.ui.camera.CameraViewModel
import com.example.traductorlsa.ui.camera.DialogoElegirParticipante
import com.example.traductorlsa.ui.camera.DialogoPausa
import com.example.traductorlsa.ui.camera.DialogoSesionTerminada
import com.example.traductorlsa.ui.camera.FijarVertical
import com.example.traductorlsa.ui.camera.GuiaDeEncuadre
import com.example.traductorlsa.ui.camera.InsigniaDeProgreso
import com.example.traductorlsa.ui.camera.MantenerPantallaEncendida
import com.example.traductorlsa.ui.camera.PieDeCaptura
import com.example.traductorlsa.ui.camera.PieDeTraduccion
import com.example.traductorlsa.ui.camera.PildoraDeVoz
import com.example.traductorlsa.ui.camera.PipelineDeCamara
import com.example.traductorlsa.ui.camera.ReferenciaFija
import com.example.traductorlsa.ui.camera.TiraSesionGuiada
import com.example.traductorlsa.ui.camera.TrainingReferenceDialog
import com.example.traductorlsa.ui.camera.TrainingWordPickerSheet
import com.example.traductorlsa.ui.camera.VeloInferior
import com.example.traductorlsa.ui.camera.VincularCamara
import com.example.traductorlsa.ui.camera.cargarPlanilla
import com.example.traductorlsa.ui.camera.displayTrainingLabel
import com.example.traductorlsa.ui.camera.estadoTraduccion
import com.example.traductorlsa.ui.camera.rememberPipelineDeCamara
import com.example.traductorlsa.ui.overlay.HandLandmarksOverlay
import com.example.traductorlsa.ui.screens.nombreParaMostrar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/*
 * La pantalla de cámara de los dos modos (traducir y entrenar). Acá queda la interfaz y
 * el cableado: el estado y lo que pasa con cada evento están en CameraViewModel, y los
 * recursos nativos en PipelineDeCamara (T-A20).
 */

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
    val ajustes by ajustesSenar()
    val repoAjustes = repositorioAjustes()
    val vm: CameraViewModel = viewModel(key = mode.name) {
        CameraViewModel(mode, AlmacenDelTelefono(context), cargarPlanilla(context))
    }
    val entrenando = vm.entrenando
    var isFrontCamera by rememberSaveable { mutableStateOf(ajustes.camaraFrontal) }

    FijarVertical()
    MantenerPantallaEncendida()

    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPermission = it }
    LaunchedEffect(Unit) { if (!hasPermission) launcher.launch(Manifest.permission.CAMERA) }

    val pipeline = rememberPipelineDeCamara(ajustes.sensibilidadDeteccion)
    LaunchedEffect(ajustes.velocidadVoz, ajustes.tonoVoz, ajustes.variante) {
        pipeline.speechManager.configurar(ajustes.velocidadVoz, ajustes.tonoVoz, ajustes.variante.locale)
    }
    VincularCamara(pipeline, hasPermission, isFrontCamera, ajustes.calidad)

    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(vm) { vm.mensajes.collect { snack.showSnackbar(it) } }
    LaunchedEffect(vm) { vm.cargar() }
    LaunchedEffect(senaInicial, vm.oficiales.size) { vm.resolverSenaInicial(senaInicial) }
    LaunchedEffect(vm.senaElegida) { vm.abrirSelectorSiHaceFalta(senaInicial) }

    // Después de cada seña la app espera que se bajen las manos; si siguen arriba un rato,
    // el pie lo pide.
    var pedirBajarManos by remember { mutableStateOf(false) }
    LaunchedEffect(vm.esperandoBajarManos) {
        pedirBajarManos = false
        if (vm.esperandoBajarManos) {
            delay(1500)
            pedirBajarManos = true
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(250)
            vm.olvidarManosViejas(SystemClock.uptimeMillis())
        }
    }

    ConectarMotor(pipeline, vm, leerEnVozAlta = { ajustes.leerEnVozAlta }, confianzaMinima = { ajustes.confianzaMinima }) {
        // El guardado sale del hilo de la cámara (TL-A5); la captura se toma ya, porque la
        // siguiente la reemplaza.
        secuencia ->
        val captura = pipeline.engine.ultimaCaptura
        scope.launch { vm.guardarToma(secuencia, captura) }
    }

    // Con un modelo que no corresponde no se abre la cámara: se dice por qué (X3).
    if (pipeline.classifier.problemas.isNotEmpty()) {
        AvisoModeloInvalido(
            problemas = pipeline.classifier.problemas,
            esPaquete = pipeline.classifier.modeloInstalado != null,
            onVolver = onBack,
        )
        return
    }

    Box(Modifier.fillMaxSize()) {
        VistaPrevia(pipeline, isFrontCamera)
        if (ajustes.mostrarLandmarks) HandLandmarksOverlay(overlay = vm.overlay)

        val senaElegida = vm.senaElegida
        val muestrasDeLaSena = senaElegida?.let { vm.conteoPorSena[it.label] ?: 0 } ?: 0

        // El velo va debajo del encuadre: al revés le apagaba las esquinas de abajo, que
        // son justo las que dicen hasta dónde llega el cuadro.
        VeloInferior(Modifier.align(Alignment.BottomCenter))

        // El encuadre ocupa casi todo lo que queda entre la cabecera y el pie. Las señas de
        // LSA se hacen con las dos manos y toman del torso para arriba: cuanto más grande es
        // el recuadro, más cerca puede estar la persona y más grandes le quedan las manos.
        if (!entrenando || senaElegida != null) {
            GuiaDeEncuadre(
                encuadreCorrecto = vm.hayManos,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 62.dp, bottom = 126.dp),
            )
        }

        if (entrenando) {
            CabeceraCamara(
                titulo = senaElegida?.let { "Entrenando ${nombreParaMostrar(it.label)}" } ?: "Modo entrenamiento",
                onVolver = onBack,
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                if (vm.sesion == null && vm.hayPlanilla) {
                    TextButton(onClick = { vm.abrirSesionGuiada() }) { Text("Guiada", color = Color.White) }
                }
                if (senaElegida != null) InsigniaDeProgreso(muestrasDeLaSena)
            }

            vm.sesion?.let { s ->
                TiraSesionGuiada(
                    sesion = s,
                    onSalir = { vm.salirDeSesion() },
                    modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 62.dp),
                )
            }

            if (senaElegida?.imageAssetPath != null) {
                ReferenciaFija(
                    assetPath = senaElegida.imageAssetPath,
                    onAmpliar = { vm.mostrarReferencia = true },
                    modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 118.dp, end = 16.dp),
                )
            }

            PieDeCaptura(
                haySena = senaElegida != null,
                muestrasDeLaSena = muestrasDeLaSena,
                frameCount = vm.frameCount,
                frameTarget = vm.frameTarget,
                hayManos = vm.hayManos,
                recienGuardada = vm.recienGuardada,
                listoParaSiguiente = vm.listoParaSiguiente,
                onCambiarSena = { vm.mostrarSelector = true },
                onGirarCamara = { isFrontCamera = !isFrontCamera },
                onDeshacer = { scope.launch { vm.deshacer() } },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        } else {
            CabeceraCamara(titulo = "Traducir señas", onVolver = onBack, modifier = Modifier.align(Alignment.TopCenter)) {
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
                frase = vm.frase,
                hayManos = vm.hayManos,
                estado = estadoTraduccion(vm.ultimaLectura, vm.frameCount, vm.hayManos, vm.frase.isNotEmpty(), pedirBajarManos),
                onRepetir = {
                    val texto = vm.frase.joinToString(" ") { nombreParaMostrar(it) }
                    if (texto.isNotBlank()) pipeline.speechManager.speak(texto)
                },
                onLimpiar = { vm.limpiarFrase() },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        DialogosDeEntrenamiento(vm, onCodigo = { scope.launch { vm.elegirCodigo(it) } })

        SnackbarHost(hostState = snack, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp))
    }
}

/**
 * Conecta los eventos del motor con el ViewModel. Los callbacks solo llaman al ViewModel,
 * que no cambia mientras la pantalla vive, así que el efecto se arma una sola vez.
 */
@Composable
private fun ConectarMotor(
    pipeline: PipelineDeCamara,
    vm: CameraViewModel,
    leerEnVozAlta: () -> Boolean,
    confianzaMinima: () -> Float,
    onTomaCapturada: (List<FloatArray>) -> Unit,
) {
    DisposableEffect(pipeline, vm) {
        val engine = pipeline.engine
        engine.onHands = { manos, w, h, rot, frontal -> vm.alVerManos(manos, w, h, rot, frontal, SystemClock.uptimeMillis()) }
        engine.onPrediction = { p ->
            // Pasa por nombreParaMostrar igual que el subtítulo: el id que devuelve el
            // modelo es "por favor", no "Por favor".
            val decir = vm.alPredecir(p.gesture, p.confidence, confianzaMinima())
            if (decir != null && leerEnVozAlta()) pipeline.speechManager.speak(nombreParaMostrar(decir))
        }
        engine.onCaptureProgress = { cuadros, objetivo -> vm.alProgresoDeCaptura(cuadros, objetivo) }
        // Copia acá, en el hilo de la cámara: el guardado corre después, en otro hilo.
        engine.onTopPredictions = { _, secuencia -> if (vm.entrenando) onTomaCapturada(secuencia.map { it.copyOf() }) }
        engine.onListoParaCapturar = { vm.alListoParaCapturar() }
        onDispose {
            engine.onHands = null
            engine.onPrediction = null
            engine.onCaptureProgress = null
            engine.onTopPredictions = null
            engine.onListoParaCapturar = null
        }
    }
}

@Composable
private fun VistaPrevia(pipeline: PipelineDeCamara, frontal: Boolean) {
    AndroidView(
        modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = if (frontal) -1f else 1f },
        factory = { ctx ->
            PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
                controller = pipeline.camController
                scaleX = if (frontal) -1f else 1f
            }
        },
        update = { pv ->
            pv.controller = pipeline.camController
            pv.scaleX = if (frontal) -1f else 1f
        },
    )
}

/** Los diálogos y hojas del modo entrenamiento: sesión guiada, selector y referencia. */
@Composable
private fun DialogosDeEntrenamiento(vm: CameraViewModel, onCodigo: (String) -> Unit) {
    if (!vm.entrenando) return
    if (vm.elegirParticipante) {
        DialogoElegirParticipante(
            codigos = vm.codigos,
            onElegir = onCodigo,
            onCancelar = { vm.elegirParticipante = false },
        )
    }
    vm.sesion?.let { s -> if (vm.mostrarPausa) DialogoPausa(s) { vm.mostrarPausa = false } }
    vm.sesionTerminada?.let { s -> DialogoSesionTerminada(s) { vm.sesionTerminada = null } }

    if (vm.mostrarSelector) {
        TrainingWordPickerSheet(
            words = vm.todasLasSenas.sortedBy { displayTrainingLabel(it.label) },
            conteoPorSena = vm.conteoPorSena,
            onDismiss = { vm.mostrarSelector = false },
            onSelect = { vm.elegirSena(it) },
            onCreateWord = { vm.crearSena(it) },
        )
    }

    val referencia = vm.senaElegida
    val imagen = referencia?.imageAssetPath
    if (vm.mostrarReferencia && referencia != null && imagen != null) {
        TrainingReferenceDialog(label = referencia.label, assetPath = imagen, onDismiss = { vm.mostrarReferencia = false })
    }
}
