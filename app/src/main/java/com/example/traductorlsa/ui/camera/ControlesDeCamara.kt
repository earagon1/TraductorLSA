package com.example.traductorlsa.ui.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.traductorlsa.ui.screens.DictionaryAssetImage
import com.example.traductorlsa.ui.theme.SenarAzul300
import com.example.traductorlsa.ui.theme.SenarAzul600
import com.example.traductorlsa.ui.theme.SenarBlanco
import java.util.*

/*
 * Piezas chicas que se superponen a la cámara: cabecera, insignia, botones, rótulo,
 * encuadre y referencia. Salieron de ui/CameraScreen.kt (T-A20).
 */

/**
 * Cabecera de las dos camaras: volver, titulo y una insignia a la derecha.
 *
 * Una sola linea. Cada franja que ocupa la interfaz obliga a la persona a
 * alejarse de la camara, y de lejos las manos le quedan chicas al detector.
 */
@Composable
internal fun CabeceraCamara(
    titulo: String,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    insignia: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(Color(0xB8080B14), Color(0x00080B14)))
            )
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BotonRedondo(
            icono = Icons.Default.ArrowBack,
            descripcion = "Volver",
            onClick = onVolver,
            tamano = 38.dp,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.92f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(10.dp))
        insignia()
    }
}

/** Muestras guardadas de la sena que se esta entrenando, con su barra. */
@Composable
internal fun InsigniaDeProgreso(muestras: Int) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.13f))
            .padding(start = 13.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(muestras.toString()) }
                withStyle(SpanStyle(color = Color.White.copy(alpha = 0.5f))) { append("/$MUESTRAS_OBJETIVO") }
            },
            color = Color.White,
            fontSize = 13.sp,
        )
        Spacer(Modifier.width(8.dp))
        LinearProgressIndicator(
            progress = { (muestras.toFloat() / MUESTRAS_OBJETIVO).coerceIn(0f, 1f) },
            modifier = Modifier.width(38.dp).height(4.dp).clip(RoundedCornerShape(999.dp)),
            color = SenarAzul300,
            trackColor = Color.White.copy(alpha = 0.20f),
            strokeCap = StrokeCap.Round,
        )
    }
}

/** Interruptor de voz, a mano durante la conversacion y no enterrado en Ajustes. */
@Composable
internal fun PildoraDeVoz(activa: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (activa) SenarAzul600 else Color.White.copy(alpha = 0.14f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (activa) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
            contentDescription = if (activa) "Apagar la voz" else "Encender la voz",
            tint = Color.White,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text("Voz", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

/**
 * Degradado inferior en lugar de una tarjeta.
 *
 * Es el recurso de los subtitulos: deja el texto legible sobre cualquier video
 * sin recortarle un tercio de pantalla al visor.
 */
@Composable
internal fun VeloInferior(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(190.dp)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0x00060910), Color(0xBD060910), Color(0xF0060910))
                )
            )
    )
}

@Composable
internal fun BotonRedondo(
    icono: ImageVector,
    descripcion: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primario: Boolean = false,
    tamano: Dp = 42.dp,
) {
    Box(
        modifier
            .size(tamano)
            .clip(CircleShape)
            .background(if (primario) SenarAzul600 else Color.White.copy(alpha = 0.13f))
            .then(
                if (primario) Modifier
                else Modifier.border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icono,
            contentDescription = descripcion,
            tint = Color.White,
            modifier = Modifier.size(tamano * 0.42f),
        )
    }
}

/** Rotulo de estado: un punto de color y una linea en versalitas. */
@Composable
internal fun RotuloDeEstado(color: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(
            text = texto,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            color = Color.White.copy(alpha = 0.68f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Guia de encuadre: cuatro esquinas y nada mas.
 *
 * Sin esto la camara puede estar apuntando al techo y la muestra se guarda
 * igual, con lo cual entra ruido al dataset. Van solo las esquinas para no
 * tapar a la persona justo mientras esta senando; se ponen azules cuando el
 * detector encuentra manos, que es la senal de que el encuadre sirve.
 */
@Composable
internal fun GuiaDeEncuadre(
    encuadreCorrecto: Boolean,
    modifier: Modifier = Modifier,
) {
    val color = if (encuadreCorrecto) SenarAzul300 else Color.White.copy(alpha = 0.30f)
    Canvas(modifier) {
        val largo = 30.dp.toPx()
        val grosor = 3.dp.toPx()
        val m = grosor / 2f
        val ancho = size.width - m
        val alto = size.height - m

        fun linea(x1: Float, y1: Float, x2: Float, y2: Float) {
            drawLine(color, Offset(x1, y1), Offset(x2, y2), grosor, StrokeCap.Round)
        }

        linea(m, m + largo, m, m); linea(m, m, m + largo, m)
        linea(ancho - largo, m, ancho, m); linea(ancho, m, ancho, m + largo)
        linea(m, alto - largo, m, alto); linea(m, alto, m + largo, alto)
        linea(ancho - largo, alto, ancho, alto); linea(ancho, alto, ancho, alto - largo)
    }
}

/**
 * Miniatura de referencia, fija en pantalla.
 *
 * Antes era un boton que abria un modal, y ademas se apagaba cuando la sena no
 * tenia imagen. Un modal no se puede mirar mientras se sena, que es justo
 * cuando hace falta: aca queda de reojo y se toca para agrandarla.
 */
@Composable
internal fun ReferenciaFija(
    assetPath: String?,
    onAmpliar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (assetPath == null) return

    Box(
        modifier
            .size(58.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SenarBlanco.copy(alpha = 0.86f))
            .clickable(onClick = onAmpliar)
            .padding(4.dp)
    ) {
        DictionaryAssetImage(
            assetPath = assetPath,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )
    }
}
