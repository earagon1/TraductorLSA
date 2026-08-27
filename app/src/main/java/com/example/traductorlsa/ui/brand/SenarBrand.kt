package com.example.traductorlsa.ui.brand

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import com.example.traductorlsa.ui.theme.SenarAzul500
import com.example.traductorlsa.ui.theme.SenarAzul750
import com.example.traductorlsa.ui.theme.SenarAzul800
import com.example.traductorlsa.ui.theme.SenarAzul900
import com.example.traductorlsa.ui.theme.SenarBlanco
import com.example.traductorlsa.ui.theme.SenarGrafito700

/*
 * Marca SeÑAR dibujada en código.
 *
 * El isotipo son dos burbujas: la azul con una mano (seña) y la grafito con
 * una onda de audio (voz). Está dibujado en un lienzo de 200 × 156 unidades y
 * escala a cualquier tamaño sin perder nitidez.
 */

/** Alto del isotipo relativo a su ancho (156 / 200). */
const val PROPORCION_ISOTIPO = 0.78f

@Composable
fun SenarIsotipo(
    ancho: Dp,
    modifier: Modifier = Modifier,
    burbujaSena: Color = SenarAzul500,
    glifoSena: Color = SenarBlanco,
    burbujaVoz: Color = SenarGrafito700,
    glifoVoz: Color = SenarBlanco,
    descripcion: String? = "SeÑAR",
) {
    val desc = descripcion
    Canvas(
        modifier
            .size(ancho, ancho * PROPORCION_ISOTIPO)
            .then(if (desc != null) Modifier.semantics { contentDescription = desc } else Modifier)
    ) {
        val u = size.width / 200f
        withTransform({ scale(u, u, Offset.Zero) }) {
            // Burbuja de seña
            drawPath(triangulo(28f, 92f, 22f, 134f, 58f, 104f), burbujaSena)
            redondeado(burbujaSena, 8f, 0f, 112f, 104f, 27f)
            mano(glifoSena, 33f, 20f, 0.62f)

            // Burbuja de voz
            drawPath(triangulo(168f, 116f, 178f, 150f, 144f, 126f), burbujaVoz)
            redondeado(burbujaVoz, 104f, 44f, 92f, 84f, 22f)
            redondeado(glifoVoz, 122f, 76f, 11f, 20f, 5.5f)
            redondeado(glifoVoz, 137f, 66f, 11f, 40f, 5.5f)
            redondeado(glifoVoz, 152f, 72f, 11f, 28f, 5.5f)
            redondeado(glifoVoz, 167f, 80f, 11f, 12f, 5.5f)
        }
    }
}

/** Logotipo "SeÑAR" con la Ñ destacada. */
@Composable
fun SenarLogotipo(
    estilo: TextStyle,
    colorTexto: Color,
    colorEne: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = buildAnnotatedString {
            append("Se")
            withStyle(SpanStyle(color = colorEne)) { append("Ñ") }
            append("AR")
        },
        style = estilo,
        color = colorTexto,
        modifier = modifier.semantics { contentDescription = "SeÑAR" },
    )
}

/** La mano del isotipo, suelta, para usar como ícono del modo señas. */
@Composable
fun SenarIconoMano(tamano: Dp, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(tamano)) {
        val u = size.minDimension / 100f
        withTransform({ scale(u, u, Offset.Zero) }) {
            mano(color, 0f, 0f, 1f)
        }
    }
}

/** La onda del isotipo, suelta, para usar como ícono del modo voz. */
@Composable
fun SenarIconoOnda(tamano: Dp, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(tamano)) {
        val u = size.minDimension / 100f
        withTransform({ scale(u, u, Offset.Zero) }) {
            redondeado(color, 20f, 38f, 11f, 24f, 5.5f)
            redondeado(color, 36f, 24f, 11f, 52f, 5.5f)
            redondeado(color, 52f, 32f, 11f, 36f, 5.5f)
            redondeado(color, 68f, 42f, 11f, 16f, 5.5f)
        }
    }
}

/**
 * La mano con sus landmarks encima: lo mismo que ve la usuaria mientras la
 * app la está leyendo. Se usa como ilustración del paso de señas.
 */
@Composable
fun SenarManoConLandmarks(
    tamano: Dp,
    modifier: Modifier = Modifier,
    colorMano: Color = SenarAzul500,
    colorLandmark: Color = SenarBlanco,
) {
    Canvas(modifier.size(tamano)) {
        val u = size.minDimension / 100f
        withTransform({ scale(u, u, Offset.Zero) }) {
            mano(colorMano, 0f, 0f, 1f)
            HUESOS.forEach { hueso ->
                val trazo = Path().apply {
                    moveTo(hueso[0], hueso[1])
                    var i = 2
                    while (i < hueso.size) {
                        lineTo(hueso[i], hueso[i + 1]); i += 2
                    }
                }
                drawPath(
                    path = trazo,
                    color = colorLandmark,
                    alpha = 0.92f,
                    style = Stroke(width = 1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
            var i = 0
            while (i < YEMAS.size) {
                drawCircle(colorLandmark, 2.9f, Offset(YEMAS[i], YEMAS[i + 1])); i += 2
            }
            i = 0
            while (i < ARTICULACIONES.size) {
                drawCircle(colorLandmark, 2.3f, Offset(ARTICULACIONES[i], ARTICULACIONES[i + 1])); i += 2
            }
        }
    }
}

/**
 * Constelación de landmarks de la mano: los mismos puntos que dibuja el
 * overlay durante la traducción, usados acá como textura de fondo.
 */
@Composable
fun SenarConstelacion(
    modifier: Modifier,
    colorTrazo: Color,
    colorPunto: Color,
    opacidad: Float,
) {
    Canvas(modifier) {
        val u = size.minDimension / 300f
        withTransform({ scale(u, u, Offset.Zero) }) {
            CADENAS.forEach { cadena ->
                val trazo = Path().apply {
                    moveTo(cadena[0], cadena[1])
                    var i = 2
                    while (i < cadena.size) {
                        lineTo(cadena[i], cadena[i + 1]); i += 2
                    }
                }
                drawPath(
                    path = trazo,
                    color = colorTrazo,
                    alpha = opacidad,
                    style = Stroke(width = 2.6f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
            var i = 0
            while (i < PUNTAS.size) {
                drawCircle(colorPunto, 5.4f, Offset(PUNTAS[i], PUNTAS[i + 1]), opacidad); i += 2
            }
            i = 0
            while (i < NUDOS.size) {
                drawCircle(colorPunto, 4.5f, Offset(NUDOS[i], NUDOS[i + 1]), opacidad); i += 2
            }
        }
    }
}

/** Las dos burbujas en contorno, para usar como marca de agua. */
@Composable
fun SenarBurbujasContorno(modifier: Modifier, color: Color, opacidad: Float) {
    Canvas(modifier) {
        val u = size.width / 200f
        withTransform({ scale(u, u, Offset.Zero) }) {
            val trazo = Stroke(width = 3.2f)
            drawRoundRect(
                color, Offset(8f, 0f), Size(112f, 104f),
                CornerRadius(27f, 27f), trazo, opacidad,
            )
            drawRoundRect(
                color, Offset(104f, 44f), Size(92f, 84f),
                CornerRadius(22f, 22f), trazo, opacidad,
            )
        }
    }
}

/** Fondo de marca: el mismo degradado del splash y de la pantalla de acceso. */
fun Modifier.fondoDeMarca(): Modifier = drawBehind {
    drawRect(
        Brush.radialGradient(
            0f to SenarAzul750,
            0.46f to SenarAzul800,
            1f to SenarAzul900,
            center = Offset(size.width * 0.5f, size.height * 0.32f),
            radius = size.maxDimension * 0.86f,
        )
    )
}

/* ---------------- primitivas de dibujo ---------------- */

private fun DrawScope.redondeado(
    color: Color, x: Float, y: Float, ancho: Float, alto: Float, radio: Float,
) = drawRoundRect(
    color = color,
    topLeft = Offset(x, y),
    size = Size(ancho, alto),
    cornerRadius = CornerRadius(radio, radio),
)

private fun triangulo(
    x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float,
): Path = Path().apply {
    moveTo(x1, y1); lineTo(x2, y2); lineTo(x3, y3); close()
}

/** Mano abierta dibujada en un espacio de 100 × 100, con origen y escala propios. */
private fun DrawScope.mano(color: Color, x: Float, y: Float, escala: Float) {
    withTransform({ translate(x, y); scale(escala, escala, Offset.Zero) }) {
        redondeado(color, 30f, 46f, 58f, 44f, 19f)   // palma
        redondeado(color, 32f, 20f, 13f, 46f, 6.5f)  // índice
        redondeado(color, 46.5f, 13f, 13f, 53f, 6.5f) // mayor
        redondeado(color, 61f, 19f, 13f, 47f, 6.5f)  // anular
        redondeado(color, 75.5f, 29f, 13f, 37f, 6.5f) // meñique
        withTransform({ rotate(-28f, Offset(16.5f, 61f)) }) {
            redondeado(color, 10f, 44f, 13f, 34f, 6.5f) // pulgar
        }
    }
}

private val CADENAS = listOf(
    floatArrayOf(150f, 270f, 110f, 240f, 86f, 212f, 70f, 184f, 56f, 158f),
    floatArrayOf(150f, 270f, 128f, 200f, 120f, 160f, 114f, 132f, 110f, 106f),
    floatArrayOf(152f, 196f, 152f, 152f, 152f, 122f, 152f, 94f),
    floatArrayOf(176f, 200f, 182f, 158f, 186f, 130f, 188f, 104f),
    floatArrayOf(150f, 270f, 198f, 212f, 210f, 180f, 216f, 158f, 220f, 136f),
    floatArrayOf(128f, 200f, 152f, 196f, 176f, 200f, 198f, 212f),
)

/** Puntas de los dedos: se dibujan un poco más grandes. */
private val PUNTAS = floatArrayOf(56f, 158f, 110f, 106f, 152f, 94f, 188f, 104f, 220f, 136f)

private val NUDOS = floatArrayOf(
    150f, 270f, 110f, 240f, 86f, 212f, 70f, 184f,
    128f, 200f, 120f, 160f, 114f, 132f,
    152f, 196f, 152f, 152f, 152f, 122f,
    176f, 200f, 182f, 158f, 186f, 130f,
    198f, 212f, 210f, 180f, 216f, 158f,
)

private val HUESOS = listOf(
    floatArrayOf(38.5f, 26f, 38.5f, 45f, 38.5f, 62f),
    floatArrayOf(53f, 19f, 53f, 41f, 53f, 62f),
    floatArrayOf(67.5f, 25f, 67.5f, 44f, 67.5f, 62f),
    floatArrayOf(82f, 35f, 82f, 50f, 82f, 62f),
    floatArrayOf(38.5f, 62f, 53f, 62f, 67.5f, 62f, 82f, 62f),
    floatArrayOf(59f, 86f, 38.5f, 62f),
    floatArrayOf(59f, 86f, 82f, 62f),
    floatArrayOf(59f, 86f, 22f, 71f, 14f, 50f),
)

private val YEMAS = floatArrayOf(38.5f, 26f, 53f, 19f, 67.5f, 25f, 82f, 35f, 14f, 50f)

private val ARTICULACIONES = floatArrayOf(
    38.5f, 45f, 38.5f, 62f,
    53f, 41f, 53f, 62f,
    67.5f, 44f, 67.5f, 62f,
    82f, 50f, 82f, 62f,
    22f, 71f, 59f, 86f,
)
