package com.example.traductorlsa.ui.camera

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.traductorlsa.ui.components.ChipDeFiltro
import com.example.traductorlsa.ui.screens.DictionaryAssetImage
import com.example.traductorlsa.ui.screens.nombreParaMostrar
import com.example.traductorlsa.ui.theme.SenarAzul100
import com.example.traductorlsa.ui.theme.SenarAzul300
import com.example.traductorlsa.ui.theme.SenarAzul600
import com.example.traductorlsa.ui.theme.SenarAzul700
import com.example.traductorlsa.ui.theme.SenarBlanco
import com.example.traductorlsa.ui.theme.SenarBorde
import com.example.traductorlsa.ui.theme.SenarBordeSuave
import com.example.traductorlsa.ui.theme.SenarGrafito300
import com.example.traductorlsa.ui.theme.SenarGrafito500
import com.example.traductorlsa.ui.theme.SenarGrafito900
import com.example.traductorlsa.ui.theme.SenarPapel
import com.example.traductorlsa.ui.theme.SenarPista
import java.util.*

/*
 * La hoja para elegir qué seña entrenar, con su filtro y sus filas.
 * Salió de ui/CameraScreen.kt (T-A20).
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TrainingWordPickerSheet(
    words: List<TrainingWordOption>,
    conteoPorSena: Map<String, Int>,
    onDismiss: () -> Unit,
    onSelect: (TrainingWordOption) -> Unit,
    onCreateWord: (String) -> Unit
) {
    var busqueda by remember { mutableStateOf("") }
    var modoNueva by remember { mutableStateOf(false) }
    var nombreNuevo by remember { mutableStateOf("") }
    var filtro by remember { mutableStateOf(FiltroDeSenas.FALTAN) }

    fun muestrasDe(w: TrainingWordOption) = conteoPorSena[w.label] ?: 0

    val visibles = remember(words, busqueda, filtro, conteoPorSena) {
        val texto = busqueda.trim().lowercase(Locale.getDefault())
        words
            .filter { nombreParaMostrar(it.label).lowercase(Locale.getDefault()).contains(texto) }
            .filter {
                when (filtro) {
                    FiltroDeSenas.FALTAN -> muestrasDe(it) < MUESTRAS_OBJETIVO
                    FiltroDeSenas.TODAS -> true
                    FiltroDeSenas.MIAS -> !it.isOfficial
                }
            }
            // Primero lo que falta: la sena sin muestras es la que hay que grabar.
            .sortedWith(compareBy({ muestrasDe(it) }, { nombreParaMostrar(it.label) }))
    }

    val faltan = words.count { muestrasDe(it) < MUESTRAS_OBJETIVO }
    val mias = words.count { !it.isOfficial }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SenarPapel) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp)
        ) {
            if (!modoNueva) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Elegí la seña a entrenar",
                        style = MaterialTheme.typography.headlineSmall,
                        color = SenarGrafito900,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(12.dp))
                    OutlinedButton(
                        onClick = { modoNueva = true },
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, SenarBorde),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = SenarBlanco),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = SenarAzul600, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Nueva", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = SenarAzul600)
                    }
                }

                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = SenarGrafito300)
                    },
                    placeholder = { Text("Buscar entre ${words.size} señas", color = SenarGrafito300) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SenarBlanco,
                        unfocusedContainerColor = SenarBlanco,
                        unfocusedBorderColor = SenarBorde,
                    ),
                )

                Spacer(Modifier.height(13.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChipDeFiltro("Faltan muestras · $faltan", filtro == FiltroDeSenas.FALTAN) { filtro = FiltroDeSenas.FALTAN }
                    ChipDeFiltro("Todas · ${words.size}", filtro == FiltroDeSenas.TODAS) { filtro = FiltroDeSenas.TODAS }
                    if (mias > 0) {
                        ChipDeFiltro("Mías · $mias", filtro == FiltroDeSenas.MIAS) { filtro = FiltroDeSenas.MIAS }
                    }
                }

                Spacer(Modifier.height(6.dp))
                if (visibles.isEmpty()) {
                    Text(
                        text = if (filtro == FiltroDeSenas.FALTAN) {
                            "Todas las señas llegaron a las $MUESTRAS_OBJETIVO muestras."
                        } else {
                            "No hay señas que coincidan con la búsqueda."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = SenarGrafito500,
                        modifier = Modifier.padding(vertical = 28.dp),
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                        items(visibles) { word ->
                            FilaDeSenaAEntrenar(
                                nombre = nombreParaMostrar(word.label),
                                assetPath = word.imageAssetPath,
                                muestras = muestrasDe(word),
                                propia = !word.isOfficial,
                                onClick = { onSelect(word) },
                            )
                            HorizontalDivider(color = SenarBordeSuave)
                        }
                    }
                }
            } else {
                Text(
                    text = "Agregar una seña",
                    style = MaterialTheme.typography.headlineSmall,
                    color = SenarGrafito900,
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = nombreNuevo,
                    onValueChange = { nombreNuevo = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    label = { Text("Nombre de la seña") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SenarBlanco,
                        unfocusedContainerColor = SenarBlanco,
                        unfocusedBorderColor = SenarBorde,
                    ),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Usá únicamente una seña real de LSA. Va a quedar disponible para entrenar aunque todavía no tenga dibujo de referencia.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SenarGrafito500,
                )
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { modoNueva = false },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, SenarBorde),
                    ) {
                        Text("Cancelar", color = SenarGrafito500, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = { onCreateWord(nombreNuevo); nombreNuevo = "" },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SenarAzul600),
                    ) {
                        Text("Agregar", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

internal enum class FiltroDeSenas { FALTAN, TODAS, MIAS }

/**
 * Fila del selector.
 *
 * Muestra el dibujo del diccionario y cuantas muestras tiene la sena, que es el
 * dato con el que se elige. La hoja anterior repetia "Seña oficial" en las 24
 * filas y escondia el numero.
 */
@Composable
internal fun FilaDeSenaAEntrenar(
    nombre: String,
    assetPath: String?,
    muestras: Int,
    propia: Boolean,
    onClick: () -> Unit,
) {
    val completa = muestras >= MUESTRAS_OBJETIVO

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(SenarBlanco)
                .border(1.dp, SenarBordeSuave, RoundedCornerShape(13.dp))
                .padding(3.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (assetPath != null) {
                DictionaryAssetImage(
                    assetPath = assetPath,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Icon(
                    Icons.Default.PanTool, contentDescription = null,
                    tint = SenarGrafito300, modifier = Modifier.size(19.dp),
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = nombre,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SenarGrafito900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (propia) {
                    Spacer(Modifier.width(7.dp))
                    Surface(shape = RoundedCornerShape(999.dp), color = SenarAzul100) {
                        Text(
                            text = "Tuya",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SenarAzul700,
                        )
                    }
                }
            }
            Spacer(Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { (muestras.toFloat() / MUESTRAS_OBJETIVO).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .width(74.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = if (completa) SenarAzul600 else SenarAzul300,
                    trackColor = SenarBordeSuave,
                    strokeCap = StrokeCap.Round,
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    text = when {
                        muestras == 0 -> "sin muestras"
                        completa -> "completa"
                        else -> "$muestras de $MUESTRAS_OBJETIVO"
                    },
                    fontSize = 12.sp,
                    color = when {
                        muestras == 0 -> SenarGrafito300
                        completa -> SenarAzul600
                        else -> SenarGrafito500
                    },
                )
            }
        }

        if (completa) {
            Box(
                Modifier.size(20.dp).clip(CircleShape).background(SenarAzul600),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = SenarBlanco, modifier = Modifier.size(13.dp))
            }
        } else {
            Icon(
                Icons.Default.ChevronRight, contentDescription = null,
                tint = SenarPista, modifier = Modifier.size(20.dp),
            )
        }
    }
}
