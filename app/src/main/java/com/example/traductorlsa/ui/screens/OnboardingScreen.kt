package com.example.traductorlsa.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.traductorlsa.ui.brand.SenarIconoMano
import com.example.traductorlsa.ui.brand.SenarIconoOnda
import com.example.traductorlsa.ui.brand.SenarIsotipo
import com.example.traductorlsa.ui.brand.SenarManoConLandmarks
import com.example.traductorlsa.ui.theme.SenarAmbar
import com.example.traductorlsa.ui.theme.SenarAzul050
import com.example.traductorlsa.ui.theme.SenarAzul100
import com.example.traductorlsa.ui.theme.SenarAzul500
import com.example.traductorlsa.ui.theme.SenarAzul600
import com.example.traductorlsa.ui.theme.SenarAzul700
import com.example.traductorlsa.ui.theme.SenarAzul900
import com.example.traductorlsa.ui.theme.SenarBlanco
import com.example.traductorlsa.ui.theme.SenarBorde
import com.example.traductorlsa.ui.theme.SenarBordeSuave
import com.example.traductorlsa.ui.theme.SenarGrafito300
import com.example.traductorlsa.ui.theme.SenarGrafito500
import com.example.traductorlsa.ui.theme.SenarGrafito700
import com.example.traductorlsa.ui.theme.SenarGrafito900
import com.example.traductorlsa.ui.theme.SenarPapel
import com.example.traductorlsa.ui.theme.SenarPapelHundido
import com.example.traductorlsa.ui.theme.SenarPista
import com.example.traductorlsa.ui.theme.SenarSobreAzul
import com.example.traductorlsa.ui.theme.SenarSobreGrafito
import com.example.traductorlsa.ui.theme.SenarSystemBars
import com.example.traductorlsa.ui.theme.SenarVisorAlto
import com.example.traductorlsa.ui.theme.SenarVisorBajo
import kotlinx.coroutines.launch

private data class PasoOnboarding(val titulo: String, val descripcion: String)

private val PASOS = listOf(
    PasoOnboarding(
        titulo = "Tus señas, en texto y voz",
        descripcion = "Apuntá la cámara, hacé la seña y SeÑAR la escribe en pantalla y la dice en voz alta.",
    ),
    PasoOnboarding(
        titulo = "Ida y vuelta, sin intérprete",
        descripcion = "Modo señas para vos. Modo voz para que la otra persona hable y vos leas lo que dice.",
    ),
    PasoOnboarding(
        titulo = "Todo pasa en tu teléfono",
        descripcion = "El reconocimiento corre en el dispositivo. No se sube ningún video a la nube.",
    ),
)

/** Alto de las ilustraciones. Es un gráfico, no texto: no escala con la fuente. */
private val ALTO_HERO = 340.dp

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    SenarSystemBars(iconosOscuros = true)

    val pager = rememberPagerState { PASOS.size }
    val scope = rememberCoroutineScope()
    val esUltimo = pager.currentPage == PASOS.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SenarPapel)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(start = 24.dp, end = 24.dp, top = 10.dp, bottom = 20.dp)
    ) {
        BarraSuperior(
            pagina = pager.currentPage,
            total = PASOS.size,
            onAtras = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
            onSaltar = onFinish,
        )

        HorizontalPager(
            state = pager,
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.Top,
        ) { pagina ->
            val paso = PASOS[pagina]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(26.dp))
                when (pagina) {
                    0 -> HeroSenas()
                    1 -> HeroDosModos()
                    else -> HeroPrivacidad()
                }
                Spacer(Modifier.height(34.dp))
                Text(
                    text = paso.titulo,
                    style = MaterialTheme.typography.headlineMedium,
                    color = SenarGrafito900,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = paso.descripcion,
                    style = MaterialTheme.typography.bodyLarge,
                    color = SenarGrafito500,
                )
                Spacer(Modifier.height(24.dp))
            }
        }

        Button(
            onClick = {
                if (esUltimo) onFinish()
                else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = SenarAzul600,
                contentColor = SenarBlanco,
            ),
        ) {
            Text(
                text = if (esUltimo) "Empezar" else "Siguiente",
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/* ---------------- barra superior ---------------- */

@Composable
private fun BarraSuperior(
    pagina: Int,
    total: Int,
    onAtras: () -> Unit,
    onSaltar: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (pagina == 0) {
                SenarIsotipo(ancho = 42.dp, descripcion = null)
            } else {
                Surface(
                    onClick = onAtras,
                    modifier = Modifier.size(44.dp),
                    shape = MaterialTheme.shapes.small,
                    color = SenarBlanco,
                    border = BorderStroke(1.dp, SenarBorde),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Paso anterior",
                            tint = SenarGrafito700,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }

        IndicadorPasos(actual = pagina, total = total)

        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            if (pagina < total - 1) {
                TextButton(onClick = onSaltar) {
                    Text(
                        text = "Saltar",
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 15.sp),
                        color = SenarGrafito500,
                    )
                }
            }
        }
    }
}

@Composable
private fun IndicadorPasos(actual: Int, total: Int) {
    Row(
        modifier = Modifier.semantics {
            contentDescription = "Paso ${actual + 1} de $total"
        },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { indice ->
            val activo = indice == actual
            val ancho by animateDpAsState(
                targetValue = if (activo) 30.dp else 12.dp,
                label = "tramoProgreso",
            )
            Box(
                Modifier
                    .width(ancho)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(if (activo) SenarAzul600 else SenarPista)
            )
        }
    }
}

/* ---------------- paso 1: la seña se lee ---------------- */

@Composable
private fun HeroSenas() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(ALTO_HERO)
            .clip(MaterialTheme.shapes.extraLarge)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        0f to SenarVisorAlto,
                        0.60f to SenarVisorBajo,
                        1f to SenarAzul900,
                        center = Offset(size.width * 0.5f, size.height * 0.28f),
                        radius = size.maxDimension * 0.95f,
                    )
                )
            }
    ) {
        EsquinasDeEncuadre(
            Modifier
                .fillMaxSize()
                .padding(22.dp)
        )

        SenarManoConLandmarks(
            tamano = 186.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 54.dp),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "SEÑA RECONOCIDA",
                style = MaterialTheme.typography.labelSmall,
                color = SenarAzul100.copy(alpha = 0.62f),
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(SenarBlanco)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "HOLA",
                    style = MaterialTheme.typography.titleLarge,
                    color = SenarGrafito900,
                )
                Spacer(Modifier.width(12.dp))
                Box(
                    Modifier
                        .width(1.dp)
                        .height(18.dp)
                        .background(SenarBordeSuave)
                )
                Spacer(Modifier.width(12.dp))
                Icon(
                    imageVector = Icons.Filled.VolumeUp,
                    contentDescription = null,
                    tint = SenarAzul600,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun EsquinasDeEncuadre(modifier: Modifier) {
    Canvas(modifier) {
        val largo = 28.dp.toPx()
        val color = SenarBlanco.copy(alpha = 0.30f)
        val trazo = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
        val ancho = size.width
        val alto = size.height

        esquina(color, trazo, 0f, largo, 0f, 0f, largo, 0f)
        esquina(color, trazo, ancho - largo, 0f, ancho, 0f, ancho, largo)
        esquina(color, trazo, 0f, alto - largo, 0f, alto, largo, alto)
        esquina(color, trazo, ancho - largo, alto, ancho, alto, ancho, alto - largo)
    }
}

private fun DrawScope.esquina(
    color: Color,
    trazo: Stroke,
    x1: Float, y1: Float,
    x2: Float, y2: Float,
    x3: Float, y3: Float,
) {
    val camino = Path().apply {
        moveTo(x1, y1); lineTo(x2, y2); lineTo(x3, y3)
    }
    drawPath(path = camino, color = color, style = trazo)
}

/* ---------------- paso 2: los dos modos ---------------- */

@Composable
private fun HeroDosModos() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(ALTO_HERO)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(SenarBlanco)
            .border(1.dp, SenarBordeSuave, MaterialTheme.shapes.extraLarge)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        SelectorDeModo()

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            BurbujaSena(texto = "HOLA ¿CÓMO ESTÁS?", etiqueta = "VOS · SEÑA")
            BurbujaVoz(texto = "Bien, gracias. ¿Y vos?", etiqueta = "LA OTRA PERSONA · VOZ")
            BurbujaSena(texto = "BIEN GRACIAS", etiqueta = null)
        }
    }
}

@Composable
private fun SelectorDeModo() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SenarPapelHundido)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SenarBlanco),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SenarIconoMano(tamano = 17.dp, color = SenarAzul600)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Señas",
                style = MaterialTheme.typography.labelMedium,
                color = SenarGrafito900,
            )
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .height(42.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SenarIconoOnda(tamano = 17.dp, color = SenarGrafito300)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Voz",
                style = MaterialTheme.typography.labelMedium,
                color = SenarGrafito300,
            )
        }
    }
}

@Composable
private fun BurbujaSena(texto: String, etiqueta: String?) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        if (etiqueta != null) {
            Text(
                text = etiqueta,
                style = MaterialTheme.typography.labelSmall,
                color = SenarGrafito300,
            )
        }
        Row(
            modifier = Modifier
                .widthIn(max = 250.dp)
                .clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp))
                .background(SenarAzul600)
                .padding(horizontal = 15.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = texto,
                style = MaterialTheme.typography.titleMedium,
                color = SenarBlanco,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = Icons.Filled.VolumeUp,
                contentDescription = null,
                tint = SenarSobreAzul,
                modifier = Modifier.size(17.dp),
            )
        }
    }
}

@Composable
private fun BurbujaVoz(texto: String, etiqueta: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.labelSmall,
            color = SenarGrafito300,
        )
        Row(
            modifier = Modifier
                .widthIn(max = 250.dp)
                .clip(RoundedCornerShape(18.dp, 18.dp, 6.dp, 18.dp))
                .background(SenarGrafito700)
                .padding(horizontal = 15.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SenarIconoOnda(tamano = 15.dp, color = SenarSobreGrafito)
            Spacer(Modifier.width(10.dp))
            Text(
                text = texto,
                style = MaterialTheme.typography.bodyMedium,
                color = SenarBlanco,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
    }
}

/* ---------------- paso 3: nada sale del teléfono ---------------- */

@Composable
private fun HeroPrivacidad() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(ALTO_HERO)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(SenarAzul050)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val margen = 24.dp.toPx()
            drawRoundRect(
                color = SenarAzul600,
                topLeft = Offset(margen, 16.dp.toPx()),
                size = Size(size.width - margen * 2, 256.dp.toPx()),
                cornerRadius = CornerRadius(56.dp.toPx(), 56.dp.toPx()),
                alpha = 0.42f,
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(7.dp.toPx(), 9.dp.toPx())
                    ),
                ),
            )
        }

        Icon(
            imageVector = Icons.Filled.CloudOff,
            contentDescription = null,
            tint = SenarGrafito300,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 26.dp, end = 24.dp)
                .size(46.dp),
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 40.dp)
                .size(width = 128.dp, height = 224.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(SenarBlanco)
                .border(2.5.dp, SenarGrafito900, RoundedCornerShape(28.dp))
                .padding(9.dp)
                .clip(RoundedCornerShape(19.dp))
                .background(SenarAzul900),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SenarIconoMano(tamano = 72.dp, color = SenarAzul500)
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SenarBlanco)
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "GRACIAS",
                        style = MaterialTheme.typography.labelSmall,
                        color = SenarGrafito900,
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = 63.dp, y = 218.dp)
                .size(50.dp)
                .clip(CircleShape)
                .background(SenarAmbar),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = SenarGrafito900,
                modifier = Modifier.size(24.dp),
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChipInformativo(
                icono = Icons.Filled.PhoneAndroid,
                texto = "En tu dispositivo",
                fondo = SenarAzul100,
                contenido = SenarAzul700,
                borde = null,
            )
            ChipInformativo(
                icono = Icons.Filled.CloudOff,
                texto = "Sin nube",
                fondo = SenarBlanco,
                contenido = SenarGrafito700,
                borde = SenarBorde,
            )
        }
    }
}

@Composable
private fun ChipInformativo(
    icono: ImageVector,
    texto: String,
    fondo: Color,
    contenido: Color,
    borde: Color?,
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(fondo)
            .then(if (borde != null) Modifier.border(1.dp, borde, CircleShape) else Modifier)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = contenido,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = texto,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = contenido,
        )
    }
}
