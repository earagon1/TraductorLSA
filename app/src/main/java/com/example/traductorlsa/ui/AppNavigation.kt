package com.example.traductorlsa.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.background

import androidx.compose.material3.*
import androidx.compose.runtime.*


import androidx.compose.ui.unit.dp

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.traductorlsa.ui.brand.SenarLogotipo
import com.example.traductorlsa.ui.screens.AuthEntryScreen
import com.example.traductorlsa.ui.screens.HomeScreen
import com.example.traductorlsa.ui.screens.SettingsScreen
import com.example.traductorlsa.ui.screens.TrainingHomeScreen
import com.example.traductorlsa.ui.screens.TranslateVoiceScreen
import com.example.traductorlsa.ui.screens.OnboardingScreen
import com.example.traductorlsa.ui.screens.SplashScreen
import com.example.traductorlsa.ui.theme.SenarTheme

import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clerk.api.Clerk
import com.clerk.ui.auth.AuthView
// CORRECCIóN 1: Nombre correcto del componente de Clerk


import androidx.compose.ui.platform.LocalContext

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap

import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavHostController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer
import java.util.Locale

import android.content.res.AssetManager
import androidx.compose.foundation.Image
import androidx.compose.ui.window.Dialog

import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow


@Composable
fun LsaTranslatorApp() {
    val navController = rememberNavController()

    SenarTheme {
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

        // Y aca usamos directamente AuthView en la ruta AuthEntry
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

        composable(AppDestination.TranslateVoice.route) {
            TranslateVoiceScreen(navController)
        }

        composable(AppDestination.TrainingHome.route) {
            TrainingAccessGate(navController) { TrainingHomeScreen(navController) }
        }
        composable(AppDestination.TrainingCapture.route) {
            TrainingAccessGate(navController) { TrainingCaptureScreen(navController) }
        }
        composable(AppDestination.Dataset.route) {
            TrainingAccessGate(navController) { DatasetScreen(navController) }
        }
        composable(AppDestination.Dictionary.route) { DictionaryScreen(navController) }
        composable(AppDestination.Settings.route) { SettingsScreen(navController) }
        composable(AppDestination.About.route) { AboutScreen(navController) }
    }
}


private fun TrainingAccessGate(
    navController: NavHostController,
    content: @Composable () -> Unit
) {
    val user by Clerk.userFlow.collectAsStateWithLifecycle()

    if (user != null) {
        content()
        return
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Modo entrenamiento") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.CloudOff,
                contentDescription = null,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "El entrenamiento solo está disponible con tu cuenta sincronizada.",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Podrás seguir usando el diccionario en modo offline. Para entrenar nuevas señas, iniciá sesión desde la app.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = { navController.navigate(AppDestination.AuthClerk.route) }) {
                Text("Iniciar sesión")
            }
        }
    }
}

@Composable
/* ----------------- Dataset ----------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatasetScreen(navController: NavHostController) {
    val context = LocalContext.current
    var counts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var totalSamples by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        counts = loadDatasetCountsByLabel(context)
        totalSamples = counts.values.sum()
    }

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
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Revisá cuántas muestras hay por seña y exportá el dataset cuando lo necesites.",
                style = MaterialTheme.typography.bodyMedium
            )

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Resumen del dataset", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (totalSamples > 0) {
                            "$totalSamples muestras guardadas en total."
                        } else {
                            "Todavía no hay muestras guardadas."
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(onClick = { shareDatasetJsonFile(context) }) {
                        Text("Exportar dataset (JSON)")
                    }
                }
            }

            if (counts.isEmpty()) {
                Text(
                    text = "Cuando guardes muestras desde entrenamiento, acá vas a poder revisar cuántas hay por seña.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text("Muestras por seña", style = MaterialTheme.typography.titleMedium)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(counts.toList()) { (label, count) ->
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(displayWord(label), style = MaterialTheme.typography.bodyLarge)
                                Text("$count", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ----------------- Diccionario ----------------- */
// ----------------------------
// Modelos / categorías
// ----------------------------
enum class WordCategory { PALABRAS, LETRAS, NUMEROS }

data class DictionaryEntry(
    val word: String,          // id real (ej: "como_estas")
    val display: String,       // lo que se ve (ej: "como estas?")
    val imageAssetPath: String?,
    val category: WordCategory
)

private fun categorizeWord(raw: String): WordCategory {
    val w = raw.trim()
    if (w.matches(Regex("^\\d+$"))) return WordCategory.NUMEROS
    if (w.length == 1 && w[0].isLetter()) return WordCategory.LETRAS
    return WordCategory.PALABRAS
}

/**
 * Convierte "como_estas" -> "como estas?"
 * Convierte "muchas_gracias" -> "muchas gracias"
 */
private fun displayWord(raw: String): String {
    val w = raw.trim()
    val spaced = w.replace("_", " ").replace("\\s+".toRegex(), " ").trim()
    val lower = spaced.lowercase(Locale.getDefault())

    // caso especial que pediste
    return if (lower == "como estas" || lower == "como estas?") "como estas?"
    else spaced
}

// ----------------------------
// Screen principal
// ----------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryScreen(navController: NavHostController) {
    val context = LocalContext.current

    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var entries by remember { mutableStateOf<List<DictionaryEntry>>(emptyList()) }

    // UI state
    var query by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) } // 0 palabras, 1 letras, 2 numeros
    var sortAsc by remember { mutableStateOf(true) }
    var selectedItem by remember { mutableStateOf<DictionaryEntry?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        errorMsg = null
        entries = emptyList()

        try {
            val result = withContext(Dispatchers.IO) {
                val words = loadWordsFromAssets(context.assets, "words.json")
                Log.d("Dictionary", "words.json -> ${words.size} palabras: ${words.take(10)}")

                words.map { w ->
                    DictionaryEntry(
                        word = w,
                        display = displayWord(w),
                        imageAssetPath = findWordImageInAssets(context.assets, w),
                        category = categorizeWord(w)
                    )
                }
            }
            entries = result
        } catch (e: Exception) {
            Log.e("Dictionary", "Error cargando diccionario", e)
            errorMsg = "No se pudo cargar el diccionario. Revisá assets/words.json."
        } finally {
            isLoading = false
        }
    }

    val selectedCategory = when (selectedTab) {
        1 -> WordCategory.LETRAS
        2 -> WordCategory.NUMEROS
        else -> WordCategory.PALABRAS
    }

    val filtered = remember(entries, query, selectedCategory, sortAsc) {
        val q = query.trim().lowercase(Locale.getDefault())

        val base = entries
            .asSequence()
            .filter { it.category == selectedCategory }
            .filter { q.isEmpty() || it.display.lowercase(Locale.getDefault()).contains(q) }

        // Orden:
        // - números: orden numérico real
        // - letras/palabras: alfabético
        val sorted = if (selectedCategory == WordCategory.NUMEROS) {
            base.sortedBy { it.word.toIntOrNull() ?: Int.MAX_VALUE }.toList()
        } else {
            base.sortedBy { it.display.lowercase(Locale.getDefault()) }.toList()
        }

        if (sortAsc) sorted else sorted.asReversed()
    }

    val tabs = listOf("Palabras", "Letras", "Números")

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Diccionario de señas") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Buscar palabra") }
            )

            Spacer(Modifier.height(10.dp))

            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { sortAsc = !sortAsc }) {
                    Text(if (sortAsc) "Orden: A a Z" else "Orden: Z a A")
                }
            }

            Spacer(Modifier.height(8.dp))

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            if (errorMsg != null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error)
                }
                return@Column
            }

            if (entries.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay palabras en words.json.")
                }
                return@Column
            }

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay resultados.")
                }
                return@Column
            }

            // Columnas por categorÃ­a
            val cols = when (selectedCategory) {
                WordCategory.LETRAS -> GridCells.Adaptive(120.dp)
                WordCategory.NUMEROS -> GridCells.Adaptive(120.dp)
                WordCategory.PALABRAS -> GridCells.Adaptive(160.dp)
            }

            LazyVerticalGrid(
                columns = cols,
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered) { item ->
                    DictionaryCard(
                        item = item,
                        onClick = { selectedItem = it }
                    )
                }
            }
        }

        //Preview: tap afuera cierra
        selectedItem?.let { item ->
            ImagePreviewDialog(item = item, onDismiss = { selectedItem = null })
        }
    }
}

// ----------------------------
// Card
// ----------------------------
@Composable
private fun DictionaryCard(
    item: DictionaryEntry,
    onClick: (DictionaryEntry) -> Unit
) {
    val cardHeight = 220.dp
    val imageHeight = 140.dp

    Card(
        onClick = { onClick(item) },
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            DictionaryAssetImage(
                assetPath = item.imageAssetPath,
                contentScale = ContentScale.Crop, // âœ… llena todo el rectÃ¡ngulo
                modifier = Modifier
                    .fillMaxWidth()
                    .height(imageHeight)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = item.display,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.imageAssetPath == null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Sin imagen",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ----------------------------
// Dialog preview (tap afuera cierra)
// ----------------------------
@Composable
private fun ImagePreviewDialog(
    item: DictionaryEntry,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = item.display,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(10.dp))

                DictionaryAssetImage(
                    assetPath = item.imageAssetPath,
                    contentScale = ContentScale.Fit, // muestra completa
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp, max = 320.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }
        }
    }
}

// ----------------------------
// Imagen desde assets
// ----------------------------
@Composable
private fun DictionaryAssetImage(
    assetPath: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop // âœ… por defecto: llena la miniatura
) {
    val context = LocalContext.current

    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = assetPath) {
        value = withContext(Dispatchers.IO) {
            if (assetPath.isNullOrBlank()) return@withContext null
            try {
                context.assets.open(assetPath).use { BitmapFactory.decodeStream(it) }
            } catch (e: Exception) {
                Log.w("Dictionary", "No se pudo abrir asset: $assetPath", e)
                null
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            contentScale = contentScale, // âœ… usa el parÃ¡metro
            modifier = modifier
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ----------------------------
// JSON loader (soporta {"word_ids":[...]})
// ----------------------------
private fun loadWordsFromAssets(assetManager: AssetManager, fileName: String): List<String> {
    val raw = assetManager.open(fileName).bufferedReader().use { it.readText() }.trim()

    fun JSONArray.toStringList(): List<String> =
        (0 until length()).mapNotNull { i ->
            optString(i, null)?.trim()?.takeIf { it.isNotBlank() }
        }

    return try {
        when {
            raw.startsWith("[") -> JSONArray(raw).toStringList()
            raw.startsWith("{") -> {
                val obj = JSONObject(raw)
                val keysToTry = listOf("word_ids", "words", "actions", "labels", "items")

                var arr: JSONArray? = null
                for (k in keysToTry) {
                    val candidate = obj.optJSONArray(k)
                    if (candidate != null) {
                        arr = candidate
                        break
                    }
                }

                arr?.toStringList() ?: emptyList()
            }
            else -> emptyList()
        }
    } catch (e: Exception) {
        Log.e("Dictionary", "Error parseando $fileName", e)
        emptyList()
    }
}

// ----------------------------
// Resolver imagen por palabra (assets/dictionary/<base>.<ext>)
// ----------------------------
private fun findWordImageInAssets(assetManager: AssetManager, word: String): String? {
    val base = normalizeForFileName(word) // "como_estas" => "como_estas"
    val exts = listOf("jpg", "jpeg", "png", "webp")

    val candidates = mutableListOf<String>()
    for (ext in exts) candidates.add("dictionary/$base.$ext")
    for (ext in exts) candidates.add("$base.$ext")

    for (path in candidates) {
        try {
            assetManager.open(path).use { }
            return path
        } catch (_: Exception) {}
    }
    return null
}

/**
 * Normaliza para nombre de archivo:
 * - minÃºsculas
 * - sin tildes
 * - mantiene "_" (porque tus words vienen con _)
 * - espacios -> "_"
 */
private fun normalizeForFileName(input: String): String {
    val lower = input.trim().lowercase(Locale.getDefault())

    val noAccents = Normalizer.normalize(lower, Normalizer.Form.NFD)
        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")

    // mantenemos "_" y números/letras/espacios
    return noAccents
        .replace("[^a-z0-9_ ]".toRegex(), "")
        .replace("\\s+".toRegex(), "_")
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
            SenarLogotipo(
                estilo = MaterialTheme.typography.headlineSmall,
                colorTexto = MaterialTheme.colorScheme.onSurface,
                colorEne = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Proyecto de Tesina de Evelin Aragón.\nLengua de Señas Argentina Texto y Voz.",
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

    // Cuando Clerk está listo y hay usuaria ir al Home
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
            // 1) Clerk todavía inicializando spinner
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

            // 2) Sin usuaria mostramos AuthView normalmente
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

            // 3) Con usuaria no mostramos AuthView (ya se está¡ navegando al Home)
            else -> {
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Opcional: un mini texto tipo "Entrando a SeÑAR..."
                    // Text("Entrando a SeÑAR")
                }
            }
        }
    }
}
