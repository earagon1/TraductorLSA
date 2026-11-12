package com.example.traductorlsa.ui.camera

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.traductorlsa.ml.GestureEngine
import com.example.traductorlsa.ui.camera.EstadoTraduccion
import com.example.traductorlsa.ui.screens.nombreParaMostrar
import com.example.traductorlsa.ui.theme.SenarAmbar
import com.example.traductorlsa.ui.theme.SenarAzul300
import com.example.traductorlsa.ui.theme.SenarAzul500
import com.example.traductorlsa.ui.theme.SenarAzul600
import java.util.*

/*
 * Los pies de la cámara: el de captura (entrenamiento) y el de traducción.
 * Salieron de ui/CameraScreen.kt (T-A20).
 */

/**
 * Pie del modo entrenamiento.
 *
 * Dos lineas y los controles en redondo, alineados con el texto. Nombra los
 * estados que GestureEngine ya tenia y no mostraba; los dos que faltaban son
 * que bajar las manos en plena captura la descarta entera, y que despues de
 * guardar hay que bajarlas para que arranque la siguiente.
 */
@Composable
internal fun PieDeCaptura(
    haySena: Boolean,
    muestrasDeLaSena: Int,
    frameCount: Int,
    frameTarget: Int,
    hayManos: Boolean,
    recienGuardada: Boolean,
    listoParaSiguiente: Boolean = false,
    onCambiarSena: () -> Unit,
    onGirarCamara: () -> Unit,
    onDeshacer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val grabando = frameCount > 0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 18.dp, end = 18.dp, bottom = 22.dp),
    ) {
        RotuloDeEstado(
            color = when {
                !haySena -> Color.White.copy(alpha = 0.40f)
                grabando || recienGuardada -> SenarAzul600
                else -> Color.White.copy(alpha = 0.40f)
            },
            texto = when {
                !haySena -> "SIN SEÑA ELEGIDA"
                grabando -> "GRABANDO · CUADRO $frameCount DE $frameTarget"
                recienGuardada -> "MUESTRA $muestrasDeLaSena GUARDADA"
                hayManos -> "TE VEO"
                else -> "ESPERANDO"
            },
        )

        Row(
            modifier = Modifier.padding(top = 7.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = when {
                        !haySena -> "Elegí una seña para empezar"
                        grabando -> "No bajes las manos"
                        recienGuardada && listoParaSiguiente -> "Ya podés hacer la siguiente seña"
                        recienGuardada -> "Bajá las manos"
                        else -> "Mostrá la seña frente a la cámara"
                    },
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 24.sp,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (grabando || recienGuardada) {
                    LinearProgressIndicator(
                        progress = {
                            if (recienGuardada) 1f
                            else (frameCount.toFloat() / frameTarget.coerceAtLeast(1)).coerceIn(0f, 1f)
                        },
                        modifier = Modifier
                            .padding(top = 9.dp)
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = if (recienGuardada) SenarAzul600 else SenarAzul500,
                        trackColor = Color.White.copy(alpha = 0.20f),
                        strokeCap = StrokeCap.Round,
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (recienGuardada) {
                    BotonRedondo(
                        icono = Icons.Default.Undo,
                        descripcion = "Deshacer la última muestra",
                        onClick = onDeshacer,
                    )
                }
                BotonRedondo(
                    icono = Icons.Default.SwapHoriz,
                    descripcion = "Cambiar de seña",
                    onClick = onCambiarSena,
                )
                BotonRedondo(
                    icono = Icons.Default.Cameraswitch,
                    descripcion = "Girar la cámara",
                    onClick = onGirarCamara,
                )
            }
        }
    }
}

/**
 * Pie del modo traduccion.
 *
 * La frase se lee como subtitulo: azul-300 para lo ya dicho, que es el color de
 * quien sena en todo el sistema, y la ultima palabra en blanco y negrita. Se
 * distingue por peso y color y no por tamano, asi la linea nunca salta de alto.
 */
@Composable
internal fun PieDeTraduccion(
    frase: List<String>,
    hayManos: Boolean,
    estado: EstadoTraduccion,
    onRepetir: () -> Unit,
    onLimpiar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val descartada = estado.esAviso

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 18.dp, end = 18.dp, bottom = 22.dp),
    ) {
        RotuloDeEstado(
            color = when {
                descartada -> SenarAmbar
                frase.isNotEmpty() -> SenarAzul600
                else -> Color.White.copy(alpha = 0.40f)
            },
            texto = estado.rotulo,
        )

        val ayuda = estado.ayuda
        if (ayuda != null) {
            Text(
                text = ayuda,
                fontSize = 12.5.sp,
                color = Color.White.copy(alpha = 0.58f),
                // También se anuncia: es el aviso de que una seña no se reconoció (TL-A6).
                modifier = Modifier.padding(top = 6.dp).semantics { liveRegion = LiveRegionMode.Polite },
            )
        }

        if (frase.isEmpty() && !descartada) {
            Text(
                text = if (hayManos) "Hacé la seña" else "Ubicate en el recuadro",
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.82f),
                modifier = Modifier.padding(top = 7.dp),
            )
            Text(
                text = "Torso y las dos manos. Arranca sola.",
                fontSize = 12.5.sp,
                color = Color.White.copy(alpha = 0.50f),
                modifier = Modifier.padding(top = 3.dp),
            )
        } else {
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = buildAnnotatedString {
                        frase.forEachIndexed { i, palabra ->
                            if (i > 0) {
                                withStyle(SpanStyle(color = Color.White.copy(alpha = 0.28f))) {
                                    append("  ·  ")
                                }
                            }
                            val ultima = i == frase.lastIndex && !descartada
                            withStyle(
                                SpanStyle(
                                    color = if (ultima) Color.White else SenarAzul300,
                                    fontWeight = if (ultima) FontWeight.Bold else FontWeight.Medium,
                                )
                            ) {
                                append(nombreParaMostrar(palabra))
                            }
                        }
                    },
                    fontSize = 21.sp,
                    lineHeight = 28.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    // TalkBack anuncia cada seña nueva sin que haya que tocar el texto: sin
                    // esto, quien usa lector de pantalla no se enteraba de la traducción si
                    // tenía la voz de la app apagada (auditoría, TL-A6).
                    modifier = Modifier
                        .weight(1f)
                        .semantics {
                            liveRegion = LiveRegionMode.Polite
                            contentDescription = "Traducción: " + frase.joinToString(", ") { nombreParaMostrar(it) }
                        },
                )

                if (frase.isNotEmpty()) {
                    Spacer(Modifier.width(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BotonRedondo(
                            icono = Icons.Default.VolumeUp,
                            descripcion = "Repetir en voz alta",
                            onClick = onRepetir,
                            primario = true,
                        )
                        BotonRedondo(
                            icono = Icons.Default.Delete,
                            descripcion = "Borrar la frase",
                            onClick = onLimpiar,
                        )
                    }
                }
            }
        }
    }
}
