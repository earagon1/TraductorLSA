package com.example.traductorlsa.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.airbnb.lottie.compose.*
import com.example.traductorlsa.R
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clerk.api.Clerk
import com.clerk.ui.auth.AuthView
import com.clerk.ui.userbutton.UserButton
// CORRECCIÓN 1: Nombre correcto del componente de Clerk
import com.clerk.ui.userprofile.UserProfileView

import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import com.example.traductorlsa.voice.VoiceToText

// Si tenés tu propio Theme (TraductorLSATheme, etc.),
// podés envolver todo esto adentro de ese tema en vez de MaterialTheme.

@Composable
fun LsaTranslatorApp() {
    val navController = rememberNavController()

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AppNavHost(navController = navController)
        }
    }
}

/* ----------------- Destinos de navegación ----------------- */

sealed class AppDestination(val route: String) {
    object Splash : AppDestination("splash")
    object Onboarding : AppDestination("onboarding")
    object AuthEntry : AppDestination("auth_entry")
    object AuthClerk : AppDestination("auth_clerk")
    object Home : AppDestination("home")
    object TranslateSign : AppDestination("translate_sign")
    object TranslateVoice : AppDestination("translate_voice")
    object TrainingHome : AppDestination("training_home")
    object TrainingCapture : AppDestination("training_capture")
    object Dataset : AppDestination("dataset")
    object Dictionary : AppDestination("dictionary")
    object Settings : AppDestination("settings")
    object About : AppDestination("about")
}

/* ----------------- NavHost principal ----------------- */
@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = AppDestination.Splash.route
    ) {
        composable(AppDestination.Splash.route) {
            SplashScreen(
                onFinished = {
                    navController.navigate(AppDestination.Onboarding.route) {
                        popUpTo(AppDestination.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(AppDestination.Onboarding.route) {
            OnboardingScreen(
                onFinish = {
                    navController.navigate(AppDestination.AuthEntry.route) {
                        popUpTo(AppDestination.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        // 👉 acá usamos directamente AuthView en la ruta AuthEntry
        composable(AppDestination.AuthEntry.route) {
            AuthEntryScreen(navController)
        }

        composable(AppDestination.AuthClerk.route) {
            AuthClerkScreen(navController)
        }


        composable(AppDestination.Home.route) {
            HomeScreen(
                onTranslateSign = { navController.navigate(AppDestination.TranslateSign.route) },
                onTranslateVoice = { navController.navigate(AppDestination.TranslateVoice.route) },
                onTraining = { navController.navigate(AppDestination.TrainingHome.route) },
                onDictionary = { navController.navigate(AppDestination.Dictionary.route) },
                onSettings = { navController.navigate(AppDestination.Settings.route) }
            )
        }

        composable(AppDestination.TranslateSign.route) {
            TranslateSignScreen(navController)
        }

        // CORRECCIÓN 2: Ahora la ruta y la función coinciden en argumentos
        composable(AppDestination.TranslateVoice.route) {
            TranslateVoiceScreen(navController)
        }

        composable(AppDestination.TrainingHome.route) { TrainingHomeScreen(navController) }
        composable(AppDestination.TrainingCapture.route) { TrainingCaptureScreen(navController) }
        composable(AppDestination.Dataset.route) { DatasetScreen(navController) }
        composable(AppDestination.Dictionary.route) { DictionaryScreen(navController) }
        composable(AppDestination.Settings.route) { SettingsScreen(navController) }
        composable(AppDestination.About.route) { AboutScreen(navController) }

        composable(AppDestination.TranslateVoice.route) {
            TranslateVoiceScreen(navController)
        }

        composable(AppDestination.TrainingHome.route) {
            TrainingHomeScreen(navController)
        }

        composable(AppDestination.TrainingCapture.route) {
            TrainingCaptureScreen(navController)
        }

        composable(AppDestination.Dataset.route) {
            DatasetScreen(navController)
        }

        composable(AppDestination.Dictionary.route) {
            DictionaryScreen(navController)
        }

        composable(AppDestination.Settings.route) {
            SettingsScreen(navController)
        }

        composable(AppDestination.About.route) {
            AboutScreen(navController)
        }
    }
}


/* ----------------- Splash ----------------- */

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1500) // 1.5s de “presentación”
        onFinished()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.Face, // mano como guiño a LSA
                contentDescription = null,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Traductor LSA",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Lengua de Señas Argentina · Texto · Voz",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/* ----------------- Onboarding ----------------- */

data class OnboardingPage(
    val title: String,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pages = listOf(
        OnboardingPage(
            "Comunicá mejor",
            "Convertí Lengua de Señas Argentina en texto y voz, y la voz en texto."
        ),
        OnboardingPage(
            "Dos modos de uso",
            "Modo para traducir señas y modo para transcribir la voz de la otra persona."
        ),
        OnboardingPage(
            "Privacidad primero",
            "El procesamiento se hace en tu dispositivo. No se suben videos a la nube."
        )
    )

    var currentPage by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Bienvenida") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            Text(
                text = pages[currentPage].title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = pages[currentPage].description,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(32.dp))

            // Indicadores de página
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                pages.indices.forEach { index ->
                    val isSelected = index == currentPage
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (isSelected) 10.dp else 8.dp)
                            .then(
                                Modifier
                                    .background(
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.outline,
                                        shape = RoundedCornerShape(50)
                                    )
                            )
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onFinish) {
                    Text("Saltar")
                }

                Button(
                    onClick = {
                        if (currentPage == pages.lastIndex) {
                            onFinish()
                        } else {
                            currentPage++
                        }
                    }
                ) {
                    Text(if (currentPage == pages.lastIndex) "Empezar" else "Siguiente")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthEntryScreen(navController: NavHostController) {
    val isInitialized by Clerk.isInitialized.collectAsStateWithLifecycle(false)
    val user by Clerk.userFlow.collectAsStateWithLifecycle()

    LaunchedEffect(isInitialized, user) {
        if (isInitialized && user != null) {
            navController.navigate(AppDestination.Home.route) {
                popUpTo(AppDestination.AuthEntry.route) { inclusive = true }
            }
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // TU HEADER + LOTTIE
            Text("SeñAR", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Comunicá en Lengua de Señas Argentina",
                style = MaterialTheme.typography.bodyMedium
            )

            AuthLottieIllustration()

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Invitada
                AuthButton(
                    icon = Icons.Filled.PhoneAndroid,
                    text = "Continuar en este dispositivo",
                    secondaryText = "Usar modo invitada sin cuenta",
                    onClick = {
                        navController.navigate(AppDestination.Home.route) {
                            popUpTo(AppDestination.AuthEntry.route) { inclusive = true }
                        }
                    }
                )

                Spacer(Modifier.height(8.dp))
                Text(
                    "o sincronizá tu progreso con una cuenta",
                    style = MaterialTheme.typography.bodySmall
                )

                // 👉 nuevo botón que abre Clerk
                AuthButton(
                    icon = Icons.Filled.Cloud,
                    text = "Sincronizar con mi cuenta",
                    secondaryText = "Iniciar sesión con Google u otros métodos",
                    onClick = {
                        navController.navigate(AppDestination.AuthClerk.route)
                    }
                )
            }
        }
    }
}




@Composable
private fun AuthLottieIllustration() {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.lsa_login) // nombre de tu json en res/raw
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)   // cuadrado, responsivo
    )
}

@Composable
private fun AuthButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    secondaryText: String,
    onClick: () -> Unit
) {
    ElevatedButton(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = secondaryText,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/* ----------------- Home / Dashboard ----------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onTranslateSign: () -> Unit,
    onTranslateVoice: () -> Unit,
    onTraining: () -> Unit,
    onDictionary: () -> Unit,
    onSettings: () -> Unit
) {
    val user by Clerk.userFlow.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("SeñAR") },
                actions = {
                    if (user != null) {
                        // Placeholder del botón de perfil para futuro Clerk UI
                        IconButton(onClick = { /* TODO: pantalla de perfil / cuenta */ }) {
                            Icon(
                                imageVector = Icons.Filled.Face,
                                contentDescription = "Perfil"
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "¿Qué querés hacer hoy?",
                style = MaterialTheme.typography.titleMedium
            )

            FeatureCard(
                title = "Traducir señas",
                description = "LSA → texto y voz",
                icon = Icons.Filled.Videocam,
                onClick = onTranslateSign
            )

            FeatureCard(
                title = "Traducir voz",
                description = "Voz → texto en pantalla",
                icon = Icons.Filled.Mic,
                onClick = onTranslateVoice
            )

            FeatureCard(
                title = "Modo entrenamiento",
                description = "Grabar nuevas muestras y gestionar el dataset",
                icon = Icons.Filled.School,
                onClick = onTraining
            )

            FeatureCard(
                title = "Diccionario de señas",
                description = "Ver las señas soportadas por el modelo",
                icon = Icons.Filled.MenuBook,
                onClick = onDictionary
            )

            FeatureCard(
                title = "Ajustes",
                description = "Voz, sensibilidad, información de la app",
                icon = Icons.Filled.Settings,
                onClick = onSettings
            )
        }
    }
}

@Composable
private fun FeatureCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/* ----------------- Pantalla Traducción de señas ----------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslateSignScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Traducir señas") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Acá reutilizamos tu lógica actual de cámara + GestureEngine
            CameraScreen()
        }
    }
}

/* ----------------- Pantalla Traducción de voz ----------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable // CORRECCIÓN 3: Eliminada anotación duplicada
fun TranslateVoiceScreen(navController: NavHostController) { // Agregado navController para consistencia
    val context = LocalContext.current
    var recognizedText by remember { mutableStateOf("Presiona el micrófono y habla...") }
    var isRecording by remember { mutableStateOf(false) }

    val vtt = remember {
        VoiceToText(
            context = context,
            onPartial = { recognizedText = it },
            onFinal = {
                recognizedText = it
                isRecording = false
            },
            onError = {
                recognizedText = "Error: $it"
                isRecording = false
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isRecording = true
            vtt.start()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Traducir voz") },
                navigationIcon = {
                    IconButton(onClick = {
                        vtt.stop() // Detener si se vuelve atrás
                        navController.popBackStack()
                    }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = recognizedText,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Button(
                onClick = {
                    if (!isRecording) {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasPermission) {
                            isRecording = true
                            vtt.start()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    } else {
                        isRecording = false
                        vtt.stop()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(if (isRecording) Icons.Default.Stop else Icons.Default.Mic, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isRecording) "Detener" else "Escuchar")
            }
        }
    }
}
/* ----------------- Modo entrenamiento: menú ----------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingHomeScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Modo entrenamiento") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FeatureCard(
                title = "Grabar nuevas muestras",
                description = "Capturar gestos para ampliar el dataset",
                icon = Icons.Filled.Videocam,
                onClick = { navController.navigate(AppDestination.TrainingCapture.route) }
            )

            FeatureCard(
                title = "Ver dataset",
                description = "Etiquetas y cantidad de muestras",
                icon = Icons.Filled.TableChart,
                onClick = { navController.navigate(AppDestination.Dataset.route) }
            )
        }
    }
}

/* ----------------- Modo entrenamiento: captura ----------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingCaptureScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Grabar nuevas muestras") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { padding ->
        // Más adelante podemos separar la lógica de training de tu CameraScreen actual
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Acá vamos a conectar el modo training que ya tenés armado (captura de muestras, JSON, etc.).",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/* ----------------- Dataset ----------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatasetScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Dataset de señas") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Acá vamos a listar etiquetas, cantidad de muestras y un botón para exportar el JSON.",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    // TODO: llamar a tu exportAndShareJson(...)
                }
            ) {
                Text("Exportar dataset (JSON)")
            }
        }
    }
}

/* ----------------- Diccionario ----------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Diccionario de señas") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Acá vamos a mostrar las palabras soportadas y una breve descripción de la seña.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/* ----------------- Ajustes ----------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Acá vamos a controlar cosas como:",
                style = MaterialTheme.typography.bodyMedium
            )
            Text("- Velocidad de la voz", style = MaterialTheme.typography.bodySmall)
            Text("- Umbral de confianza de las predicciones", style = MaterialTheme.typography.bodySmall)
            Text("- Mostrar / ocultar overlay de landmarks", style = MaterialTheme.typography.bodySmall)

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { navController.navigate(AppDestination.About.route) }
            ) {
                Icon(Icons.Filled.Info, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Acerca de la app")
            }

            Spacer(Modifier.height(32.dp))

            // 👉 BOTÓN DE CERRAR SESIÓN
            val scope = rememberCoroutineScope()

            OutlinedButton(
                onClick = {
                    scope.launch {
                        try {
                            // Cierra sesión en Clerk
                            Clerk.signOut()

                            // Limpia el back stack y vuelve al flujo de auth
                            navController.navigate(AppDestination.AuthEntry.route) {
                                popUpTo(AppDestination.Home.route) { inclusive = true }
                            }
                        } catch (e: Exception) {
                            // Si querés, podés loguear el error
                            e.printStackTrace()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors()
            ) {
                Icon(Icons.Filled.Cloud, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Cerrar sesión")
            }
        }
    }
}

/* ----------------- Acerca de ----------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Acerca de") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Traductor LSA",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Proyecto de Tesina de Evelin Aragón.\nLengua de Señas Argentina ↔ Texto y Voz.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Tecnologías: Kotlin, Jetpack Compose, CameraX, MediaPipe, TensorFlow Lite.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthClerkScreen(navController: NavHostController) {
    val isInitialized by Clerk.isInitialized.collectAsStateWithLifecycle(false)
    val user by Clerk.userFlow.collectAsStateWithLifecycle()

    // Cuando Clerk está listo y hay usuaria → ir al Home
    LaunchedEffect(isInitialized, user) {
        if (isInitialized && user != null) {
            navController.navigate(AppDestination.Home.route) {
                // limpiamos el flujo de auth del back stack
                popUpTo(AppDestination.AuthEntry.route) { inclusive = true }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Sincronizar con mi cuenta") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { padding ->

        when {
            // 1) Clerk todavía inicializando → spinner
            !isInitialized -> {
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // 2) Sin usuaria → mostramos AuthView normalmente
            user == null -> {
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                ) {
                    AuthView(
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // 3) Con usuaria → no mostramos AuthView (ya se está navegando al Home)
            else -> {
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Opcional: un mini texto tipo "Entrando a SeñAR..."
                    // Text("Entrando a SeñAR…")
                }
            }
        }
    }
}



