package com.example.traductorlsa.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.traductorlsa.ui.AppDestination
import com.example.traductorlsa.ui.MUESTRAS_OBJETIVO
import com.example.traductorlsa.ui.components.Aire
import com.example.traductorlsa.ui.components.ChipDeFiltro
import com.example.traductorlsa.ui.components.PantallaSenar
import com.example.traductorlsa.ui.components.TituloDePagina
import com.example.traductorlsa.ui.findTrainingWordImageInAssets
import com.example.traductorlsa.ui.loadAllLabels
import com.example.traductorlsa.ui.loadDatasetCountsByLabel
import com.example.traductorlsa.ui.shareDatasetJsonFile
import com.example.traductorlsa.ui.theme.SenarAzul050
import com.example.traductorlsa.ui.theme.SenarAzul200
import com.example.traductorlsa.ui.theme.SenarAzul300
import com.example.traductorlsa.ui.theme.SenarAzul600
import com.example.traductorlsa.ui.theme.SenarBlanco
import com.example.traductorlsa.ui.theme.SenarBorde
import com.example.traductorlsa.ui.theme.SenarBordeSuave
import com.example.traductorlsa.ui.theme.SenarGrafito300
import com.example.traductorlsa.ui.theme.SenarGrafito500
import com.example.traductorlsa.ui.theme.SenarGrafito900
import com.example.traductorlsa.ui.theme.SenarPista
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Una sena del catalogo con lo que el dataset ya tiene guardado de ella. */
private data class SenaDelDataset(
    val etiqueta: String,
    val nombre: String,
    val imagen: String?,
    val muestras: Int,
) {
    val completa: Boolean get() = muestras >= MUESTRAS_OBJETIVO
}

private enum class FiltroDataset { FALTAN, TODAS, LISTAS }

/**
 * Dataset de senas.
 *
 * La pantalla anterior mostraba numeros crudos («hola — 3») que no dicen nada
 * sin un punto de comparacion, tenia el boton de exportar encendido con cero
 * muestras y era de solo lectura. Ahora cada sena se mide contra una meta, la
 * lista ordena por lo que falta y tocar una fila abre la camara entrenando esa
 * sena: el dataset pasa a ser la lista de tareas.
 */
@Composable
fun DatasetScreen(navController: NavHostController) {
    val context = LocalContext.current
    var senas by remember { mutableStateOf<List<SenaDelDataset>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var filtro by remember { mutableStateOf(FiltroDataset.FALTAN) }

    LaunchedEffect(Unit) {
        senas = withContext(Dispatchers.IO) { leerSenasDelDataset(context) }
        cargando = false
    }

    val total = senas.sumOf { it.muestras }
    val completas = senas.count { it.completa }
    val aMedias = senas.count { it.muestras in 1 until MUESTRAS_OBJETIVO }
    val sinMuestras = senas.count { it.muestras == 0 }

    PantallaSenar(
        onAvatar = { navController.navigate(AppDestination.Settings.route) },
        desplazable = false,
    ) {
        TituloDePagina(texto = "Dataset de señas", onVolver = { navController.popBackStack() })

        when {
            cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SenarAzul600)
            }

            total == 0 -> DatasetVacio(
                onGrabar = { navController.navigate(AppDestination.TrainingCapture.route) },
            )

            else -> {
                Aire(20)
                CoberturaDelModelo(
                    completas = completas,
                    aMedias = aMedias,
                    sinMuestras = sinMuestras,
                )

                Aire(14)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChipDeFiltro("Faltan muestras", filtro == FiltroDataset.FALTAN) {
                        filtro = FiltroDataset.FALTAN
                    }
                    ChipDeFiltro("Todas", filtro == FiltroDataset.TODAS) {
                        filtro = FiltroDataset.TODAS
                    }
                    ChipDeFiltro("Listas", filtro == FiltroDataset.LISTAS) {
                        filtro = FiltroDataset.LISTAS
                    }
                }

                val visibles = when (filtro) {
                    FiltroDataset.FALTAN -> senas.filter { !it.completa }
                    FiltroDataset.TODAS -> senas
                    FiltroDataset.LISTAS -> senas.filter { it.completa }
                }

                Aire(12)
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(visibles, key = { it.etiqueta }) { sena ->
                        FilaDelDataset(
                            sena = sena,
                            onClick = {
                                navController.navigate(
                                    AppDestination.TrainingCapture.paraSena(sena.etiqueta)
                                )
                            },
                        )
                    }
                }

                Aire(14)
                OutlinedButton(
                    onClick = {
                        if (!shareDatasetJsonFile(context)) {
                            Toast.makeText(
                                context,
                                "No se pudo compartir el dataset.",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, SenarBorde),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = SenarBlanco),
                ) {
                    Icon(
                        Icons.Filled.FileUpload,
                        contentDescription = null,
                        tint = SenarGrafito500,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(9.dp))
                    Text(
                        text = "Exportar $total muestras (JSON)",
                        style = MaterialTheme.typography.labelLarge,
                        color = SenarGrafito500,
                    )
                }
            }
        }
    }
}

/**
 * Cobertura del modelo.
 *
 * Una barra partida en tres dice de un vistazo lo que un total suelto no dice:
 * cuantas senas estan listas, cuantas a medias y cuantas sin empezar.
 */
@Composable
private fun CoberturaDelModelo(completas: Int, aMedias: Int, sinMuestras: Int) {
    val totalSenas = completas + aMedias + sinMuestras

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SenarBlanco)
            .border(1.dp, SenarBordeSuave, RoundedCornerShape(24.dp))
            .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 15.dp),
    ) {
        Text(
            text = "COBERTURA DEL MODELO",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.3.sp,
            color = SenarGrafito300,
        )
        Aire(6)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = completas.toString(),
                style = MaterialTheme.typography.headlineMedium,
                color = SenarGrafito900,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "de $totalSenas señas completas",
                style = MaterialTheme.typography.bodyMedium,
                color = SenarGrafito500,
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }

        Aire(12)
        Row(
            modifier = Modifier.fillMaxWidth().height(8.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            // Solo los tramos con contenido: un weight de cero no es valido.
            if (completas > 0) TramoDeCobertura(completas.toFloat(), SenarAzul600)
            if (aMedias > 0) TramoDeCobertura(aMedias.toFloat(), SenarAzul200)
            if (sinMuestras > 0) TramoDeCobertura(sinMuestras.toFloat(), SenarBordeSuave)
        }

        Aire(10)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            LeyendaDeCobertura(SenarAzul600, "$completas listas")
            LeyendaDeCobertura(SenarAzul200, "$aMedias a medias")
            LeyendaDeCobertura(SenarBordeSuave, "$sinMuestras sin muestras")
        }
    }
}

@Composable
private fun RowScope.TramoDeCobertura(peso: Float, color: Color) {
    Box(
        Modifier
            .weight(peso)
            .fillMaxSize()
            .clip(RoundedCornerShape(999.dp))
            .background(color),
    )
}

@Composable
private fun LeyendaDeCobertura(color: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(
            text = texto,
            fontSize = 11.5.sp,
            color = SenarGrafito500,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Fila de una sena: dibujo del diccionario, nombre y progreso hacia la meta. */
@Composable
private fun FilaDelDataset(sena: SenaDelDataset, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SenarBlanco)
            .border(1.dp, SenarBordeSuave, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(SenarAzul050)
                .padding(3.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (sena.imagen != null) {
                DictionaryAssetImage(
                    assetPath = sena.imagen,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Icon(
                    Icons.Filled.PanTool,
                    contentDescription = null,
                    tint = SenarGrafito300,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Spacer(Modifier.width(13.dp))

        Column(Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = sena.nombre,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SenarGrafito900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(10.dp))
                if (sena.completa) {
                    Box(
                        Modifier.size(22.dp).clip(CircleShape).background(SenarAzul600),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = "Completa",
                            tint = SenarBlanco,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                } else {
                    Text(
                        text = "${sena.muestras} / $MUESTRAS_OBJETIVO",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SenarGrafito500,
                    )
                }
            }

            Aire(8)
            LinearProgressIndicator(
                progress = { (sena.muestras.toFloat() / MUESTRAS_OBJETIVO).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = if (sena.completa) SenarAzul600 else SenarAzul300,
                trackColor = SenarBordeSuave,
                strokeCap = StrokeCap.Round,
            )
        }
    }
}

/**
 * Estado vacio.
 *
 * Un solo mensaje y una sola accion. La pantalla anterior decia dos veces lo
 * mismo y ofrecia exportar un dataset que todavia no existe.
 */
@Composable
private fun ColumnScope.DatasetVacio(onGrabar: () -> Unit) {
    Column(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(44.dp))
                .background(SenarBlanco)
                .border(1.dp, SenarBordeSuave, RoundedCornerShape(44.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Videocam,
                contentDescription = null,
                tint = SenarPista,
                modifier = Modifier.size(64.dp),
            )
        }
        Aire(30)
        Text(
            text = "Todavía no hay muestras",
            style = MaterialTheme.typography.headlineSmall,
            color = SenarGrafito900,
        )
        Aire(12)
        Text(
            text = "Cada seña necesita unas $MUESTRAS_OBJETIVO repeticiones tuyas para que el modelo la aprenda. Empezá por la primera.",
            style = MaterialTheme.typography.bodyMedium,
            color = SenarGrafito500,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }

    Button(
        onClick = onGrabar,
        modifier = Modifier.fillMaxWidth().height(58.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SenarAzul600),
    ) {
        Icon(
            Icons.Filled.Videocam,
            contentDescription = null,
            tint = SenarBlanco,
            modifier = Modifier.size(21.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Grabar la primera seña",
            style = MaterialTheme.typography.titleMedium,
            color = SenarBlanco,
        )
    }
    Aire(14)
    Text(
        text = "Vas a poder exportar el dataset cuando tenga muestras.",
        style = MaterialTheme.typography.bodySmall,
        color = SenarGrafito300,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * Junta el catalogo de senas con lo que hay guardado.
 *
 * Se incluyen tambien las etiquetas que solo existen en el dataset: son las
 * senas propias que se agregaron desde el entrenamiento y no estan en
 * words.json, y si no aparecieran no habria forma de ver cuantas muestras
 * tienen.
 */
private fun leerSenasDelDataset(context: Context): List<SenaDelDataset> {
    val conteos = loadDatasetCountsByLabel(context)
    val etiquetas = (loadAllLabels(context) + conteos.keys).distinct()

    return etiquetas
        .map { etiqueta ->
            SenaDelDataset(
                etiqueta = etiqueta,
                nombre = nombreParaMostrar(etiqueta),
                imagen = findTrainingWordImageInAssets(context, etiqueta),
                muestras = conteos[etiqueta] ?: 0,
            )
        }
        // Primero lo que falta, que es lo unico accionable de esta pantalla.
        .sortedWith(compareBy({ it.muestras }, { it.nombre }))
}
