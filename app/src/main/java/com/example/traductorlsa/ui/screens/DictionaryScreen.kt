package com.example.traductorlsa.ui.screens

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.traductorlsa.ui.AppDestination
import com.example.traductorlsa.ui.components.Aire
import com.example.traductorlsa.ui.components.PantallaSenar
import com.example.traductorlsa.ui.components.SelectorSegmentado
import com.example.traductorlsa.ui.components.TituloDePagina
import com.example.traductorlsa.ui.theme.SenarAzul050
import com.example.traductorlsa.ui.theme.SenarAzul100
import com.example.traductorlsa.ui.theme.SenarAzul600
import com.example.traductorlsa.ui.theme.SenarAzul700
import com.example.traductorlsa.ui.theme.SenarBlanco
import com.example.traductorlsa.ui.theme.SenarBorde
import com.example.traductorlsa.ui.theme.SenarBordeSuave
import com.example.traductorlsa.ui.theme.SenarGrafito300
import com.example.traductorlsa.ui.theme.SenarGrafito500
import com.example.traductorlsa.ui.theme.SenarGrafito700
import com.example.traductorlsa.ui.theme.SenarGrafito900
import com.example.traductorlsa.ui.theme.SenarPapel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer
import java.util.Locale

enum class WordCategory { PALABRAS, LETRAS, NUMEROS }

data class DictionaryEntry(
    val word: String,
    val display: String,
    val imageAssetPath: String?,
    val category: WordCategory,
)

@Composable
fun DictionaryScreen(navController: NavHostController) {
    val context = LocalContext.current

    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var entradas by remember { mutableStateOf<List<DictionaryEntry>>(emptyList()) }

    var busqueda by remember { mutableStateOf("") }
    var pestana by remember { mutableStateOf(0) }
    var ascendente by remember { mutableStateOf(true) }
    var seleccion by remember { mutableStateOf<DictionaryEntry?>(null) }

    LaunchedEffect(Unit) {
        cargando = true
        error = null
        try {
            entradas = withContext(Dispatchers.IO) {
                loadWordsFromAssets(context.assets, "words.json").map { palabra ->
                    DictionaryEntry(
                        word = palabra,
                        display = nombreParaMostrar(palabra),
                        imageAssetPath = findWordImageInAssets(context.assets, palabra),
                        category = categorizeWord(palabra),
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("Dictionary", "Error cargando el diccionario", e)
            error = "No se pudo cargar el diccionario. Revisá assets/words.json."
        } finally {
            cargando = false
        }
    }

    val categoria: WordCategory = when (pestana) {
        1 -> WordCategory.LETRAS
        2 -> WordCategory.NUMEROS
        else -> WordCategory.PALABRAS
    }

    val visibles: List<DictionaryEntry> = remember(entradas, busqueda, categoria, ascendente) {
        val q = sinTildes(busqueda.trim())
        val base = entradas
            .filter { it.category == categoria }
            .filter { q.isEmpty() || sinTildes(it.display).contains(q) || sinTildes(it.word).contains(q) }

        val ordenadas = if (categoria == WordCategory.NUMEROS) {
            base.sortedBy { it.word.toIntOrNull() ?: Int.MAX_VALUE }
        } else {
            base.sortedBy { sinTildes(it.display) }
        }
        if (ascendente) ordenadas else ordenadas.reversed()
    }

    val elegida = seleccion
    BackHandler(enabled = elegida != null) { seleccion = null }

    PantallaSenar(
        onAvatar = { navController.navigate(AppDestination.Settings.route) },
        desplazable = false,
    ) {
        if (elegida != null) {
            DetalleDeSena(
                entrada = elegida,
                onVolver = { seleccion = null },
                onPracticar = { navController.navigate(AppDestination.TranslateSign.route) },
            )
        } else {
        TituloDePagina(texto = "Diccionario", onVolver = { navController.popBackStack() })

        Aire(22)
        CampoDeBusqueda(valor = busqueda, onCambio = { busqueda = it })

        Aire(16)
        SelectorSegmentado(
            opciones = listOf("Palabras", "Letras", "Números"),
            seleccionada = pestana,
            onSeleccion = { pestana = it },
        )

        Aire(20)
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = when {
                    cargando -> "CARGANDO…"
                    else -> "${visibles.size} ${etiquetaDeCategoria(categoria, visibles.size)}"
                },
                style = MaterialTheme.typography.labelSmall,
                color = SenarGrafito300,
            )
            Surface(
                onClick = { ascendente = !ascendente },
                shape = CircleShape,
                color = SenarBlanco,
                border = BorderStroke(1.dp, SenarBorde),
            ) {
                Row(
                    Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.SwapVert,
                        contentDescription = null,
                        tint = SenarGrafito500,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (ascendente) "A – Z" else "Z – A",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = SenarGrafito500,
                    )
                }
            }
        }

        Aire(14)
        val mensajeError = error
        when {
            cargando -> Caja { CircularProgressIndicator(color = SenarAzul600) }
            mensajeError != null -> Caja { Aviso(mensajeError) }
            visibles.isEmpty() -> Caja {
                Aviso(
                    if (busqueda.isBlank()) "Todavía no hay señas en esta categoría."
                    else "No encontramos ninguna seña con “${busqueda.trim()}”."
                )
            }
            else -> {
                // Las palabras necesitan ver el cuerpo entero, así que van de a dos.
                // Las letras y los números son configuraciones de la mano: entran cuatro.
                val columnas = if (categoria == WordCategory.PALABRAS) 2 else 4
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columnas),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(if (columnas == 2) 14.dp else 10.dp),
                    verticalArrangement = Arrangement.spacedBy(if (columnas == 2) 14.dp else 10.dp),
                ) {
                    items(visibles, key = { it.word }) { entrada ->
                        TarjetaDeSena(
                            entrada = entrada,
                            compacta = columnas > 2,
                            onClick = { seleccion = entrada },
                        )
                    }
                }
            }
        }
        }
    }
}

/* ---------------- detalle ---------------- */

// Extiende ColumnScope porque la tarjeta de la imagen usa weight para
// quedarse con el alto que sobra.
@Composable
private fun ColumnScope.DetalleDeSena(
    entrada: DictionaryEntry,
    onVolver: () -> Unit,
    onPracticar: () -> Unit,
) {
    TituloDePagina(texto = entrada.display, onVolver = onVolver)

    Aire(22)
    Box(
        Modifier
            .fillMaxWidth()
            .weight(1f)
            .clip(RoundedCornerShape(28.dp))
            .background(SenarBlanco)
            .padding(18.dp),
        contentAlignment = Alignment.Center,
    ) {
        DictionaryAssetImage(
            assetPath = entrada.imageAssetPath,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }

    Aire(18)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Chip(
            texto = when (entrada.category) {
                WordCategory.PALABRAS -> "Palabra"
                WordCategory.LETRAS -> "Letra"
                WordCategory.NUMEROS -> "Número"
            },
            fondo = SenarAzul100,
            contenido = SenarAzul700,
        )
        Chip(texto = "El modelo la reconoce", fondo = SenarBlanco, contenido = SenarGrafito700, conBorde = true)
    }

    Aire(20)
    Surface(
        onClick = onPracticar,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        shape = RoundedCornerShape(20.dp),
        color = SenarAzul600,
        shadowElevation = 8.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Videocam,
                contentDescription = null,
                tint = SenarBlanco,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Practicar esta seña",
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 17.5.sp),
                color = SenarBlanco,
            )
        }
    }

    Aire(12)
    Text(
        // TODO: cuando el traductor acepte una seña esperada, cambiar por
        // "Abre la cámara esperando justo esta seña".
        text = "Abre el traductor para que la practiques",
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
        color = SenarGrafito500,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Aire(8)
}

/* ---------------- piezas ---------------- */

@Composable
private fun TarjetaDeSena(
    entrada: DictionaryEntry,
    compacta: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(if (compacta) 16.dp else 20.dp),
        color = SenarBlanco,
        border = BorderStroke(1.dp, SenarBordeSuave),
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(if (compacta) 80.dp else 148.dp)
                    .padding(if (compacta) 5.dp else 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Fit y no Crop: en una seña, dónde queda el brazo respecto del
                // cuerpo es parte de lo que la tarjeta viene a enseñar.
                DictionaryAssetImage(
                    assetPath = entrada.imageAssetPath,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(SenarPapel)
                    .padding(
                        horizontal = if (compacta) 6.dp else 14.dp,
                        vertical = if (compacta) 8.dp else 12.dp,
                    ),
                contentAlignment = if (compacta) Alignment.Center else Alignment.CenterStart,
            ) {
                Text(
                    text = entrada.display,
                    style = if (compacta) {
                        MaterialTheme.typography.headlineSmall.copy(fontSize = 19.sp)
                    } else {
                        MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    color = SenarGrafito900,
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
private fun CampoDeBusqueda(valor: String, onCambio: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(SenarBlanco)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            tint = SenarGrafito300,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (valor.isEmpty()) {
                Text(
                    text = "Buscar una seña",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.5.sp),
                    color = SenarGrafito300,
                )
            }
            BasicTextField(
                value = valor,
                onValueChange = onCambio,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.5.sp,
                    color = SenarGrafito900,
                ),
                cursorBrush = SolidColor(SenarAzul600),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (valor.isNotEmpty()) {
            Surface(
                onClick = { onCambio("") },
                shape = CircleShape,
                color = SenarAzul050,
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Borrar la búsqueda",
                    tint = SenarGrafito500,
                    modifier = Modifier
                        .padding(5.dp)
                        .size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun Chip(
    texto: String,
    fondo: Color,
    contenido: Color,
    conBorde: Boolean = false,
) {
    Surface(
        shape = CircleShape,
        color = fondo,
        border = if (conBorde) BorderStroke(1.dp, SenarBorde) else null,
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = contenido,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
        )
    }
}

@Composable
private fun ColumnScope.Caja(contenido: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .weight(1f),
        contentAlignment = Alignment.Center,
    ) { contenido() }
}

@Composable
private fun Aviso(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodyMedium,
        color = SenarGrafito500,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 32.dp),
    )
}

@Composable
internal fun DictionaryAssetImage(
    assetPath: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val context = LocalContext.current

    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = assetPath) {
        value = withContext(Dispatchers.IO) {
            if (assetPath.isNullOrBlank()) return@withContext null
            try {
                context.assets.open(assetPath).use { BitmapFactory.decodeStream(it) }
            } catch (e: Exception) {
                Log.w("Dictionary", "No se pudo abrir el asset: $assetPath", e)
                null
            }
        }
    }

    val mapa = bitmap
    if (mapa != null) {
        Image(
            bitmap = mapa.asImageBitmap(),
            contentDescription = null,
            contentScale = contentScale,
            modifier = modifier,
        )
    } else {
        Box(modifier, contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.Image,
                contentDescription = null,
                tint = SenarGrafito300,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

/* ---------------- datos ---------------- */

private fun etiquetaDeCategoria(categoria: WordCategory, cantidad: Int): String = when (categoria) {
    WordCategory.LETRAS -> if (cantidad == 1) "LETRA" else "LETRAS"
    WordCategory.NUMEROS -> if (cantidad == 1) "NÚMERO" else "NÚMEROS"
    WordCategory.PALABRAS -> if (cantidad == 1) "SEÑA" else "SEÑAS"
}

internal fun categorizeWord(raw: String): WordCategory {
    val w = raw.trim()
    if (w.matches(Regex("^\\d+$"))) return WordCategory.NUMEROS
    if (w.length == 1 && w[0].isLetter()) return WordCategory.LETRAS
    return WordCategory.PALABRAS
}

/**
 * Nombre para mostrar.
 *
 * words.json guarda los ids con los que entrena el modelo, sin tildes ni
 * signos: "adios", "como_estas". Eso no puede llegar tal cual a la pantalla,
 * así que se traduce acá. Al agregar una seña nueva al modelo, sumar también
 * su nombre a este mapa.
 */
private val NOMBRES_PARA_MOSTRAR = mapOf(
    "hola" to "Hola",
    "adios" to "Adiós",
    "bien" to "Bien",
    "como_estas" to "¿Cómo estás?",
    "gracias" to "Gracias",
)

internal fun nombreParaMostrar(raw: String): String {
    val id = raw.trim().lowercase(Locale.getDefault())
    NOMBRES_PARA_MOSTRAR[id]?.let { return it }

    val limpio = id.replace("_", " ").replace("\\s+".toRegex(), " ").trim()
    return when {
        limpio.isEmpty() -> raw.trim()
        limpio.length == 1 -> limpio.uppercase(Locale.getDefault())
        limpio.all { it.isDigit() } -> limpio
        else -> limpio.replaceFirstChar { it.uppercase(Locale.getDefault()) }
    }
}

/** Para que buscar "adios" encuentre "Adiós". */
private fun sinTildes(texto: String): String =
    Normalizer.normalize(texto.lowercase(Locale.getDefault()), Normalizer.Form.NFD)
        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")

internal fun loadWordsFromAssets(assetManager: AssetManager, fileName: String): List<String> {
    val raw = assetManager.open(fileName).bufferedReader().use { it.readText() }.trim()

    fun JSONArray.aLista(): List<String> =
        (0 until length()).mapNotNull { i -> optString(i, null)?.trim()?.takeIf { it.isNotBlank() } }

    return try {
        when {
            raw.startsWith("[") -> JSONArray(raw).aLista()
            raw.startsWith("{") -> {
                val obj = JSONObject(raw)
                listOf("word_ids", "words", "actions", "labels", "items")
                    .firstNotNullOfOrNull { obj.optJSONArray(it) }
                    ?.aLista() ?: emptyList()
            }
            else -> emptyList()
        }
    } catch (e: Exception) {
        Log.e("Dictionary", "Error parseando $fileName", e)
        emptyList()
    }
}

internal fun findWordImageInAssets(assetManager: AssetManager, word: String): String? {
    val base = normalizeForFileName(word)
    val extensiones = listOf("jpg", "jpeg", "png", "webp")
    val candidatos = extensiones.map { "dictionary/$base.$it" } + extensiones.map { "$base.$it" }

    for (ruta in candidatos) {
        try {
            assetManager.open(ruta).use { }
            return ruta
        } catch (_: Exception) {
        }
    }
    return null
}

/** Nombre de archivo: minúsculas, sin tildes, con "_" en lugar de espacios. */
private fun normalizeForFileName(input: String): String =
    sinTildes(input.trim())
        .replace("[^a-z0-9_ ]".toRegex(), "")
        .replace("\\s+".toRegex(), "_")
