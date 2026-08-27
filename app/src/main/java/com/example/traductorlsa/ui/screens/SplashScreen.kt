package com.example.traductorlsa.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.traductorlsa.startup.AssetWarmup
import com.example.traductorlsa.ui.brand.SenarBurbujasContorno
import com.example.traductorlsa.ui.brand.SenarConstelacion
import com.example.traductorlsa.ui.brand.SenarIsotipo
import com.example.traductorlsa.ui.brand.SenarLogotipo
import com.example.traductorlsa.ui.brand.fondoDeMarca
import com.example.traductorlsa.ui.theme.SenarAzul100
import com.example.traductorlsa.ui.theme.SenarAzul200
import com.example.traductorlsa.ui.theme.SenarAzul300
import com.example.traductorlsa.ui.theme.SenarAzul500
import com.example.traductorlsa.ui.theme.SenarBlanco
import com.example.traductorlsa.ui.theme.SenarGrafito900
import com.example.traductorlsa.ui.theme.SenarSobreMarca
import com.example.traductorlsa.ui.theme.SenarSobreMarcaSuave
import com.example.traductorlsa.ui.theme.SenarSystemBars
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Tiempo mínimo en pantalla. Es el número a tocar si querés que la
 * presentación dure más o menos: cuenta desde que arranca la animación de
 * entrada, que termina a los 780 ms.
 */
private const val TIEMPO_MINIMO_MS = 2_200L

/** Techo de espera: si la precarga se hace larga, entramos igual. */
private const val TIEMPO_MAXIMO_MS = 5_000L

/** La barra empieza a llenarse recién cuando terminó de aparecer. */
private const val RETARDO_BARRA_MS = 320L

/**
 * Hasta dónde sube la barra sola. El tramo que queda no se completa hasta que
 * los assets están realmente leídos: así nunca dice "listo" antes de tiempo.
 */
private const val TOPE_BARRA = 0.90f

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    SenarSystemBars(iconosOscuros = false)

    val context = LocalContext.current

    // La barra arranca vacía y sube sola. Los tres assets terminan a saltos, así
    // que atarla al progreso real se veía como dos tirones, no como una carga.
    val relleno = remember { Animatable(0f) }

    // La entrada arranca en cuanto la pantalla se compone: el isotipo aparece
    // creciendo y el texto lo sigue, un poco después.
    var entro by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entro = true }

    val escalaIsotipo by animateFloatAsState(
        targetValue = if (entro) 1f else 0.86f,
        animationSpec = tween(durationMillis = 620, easing = FastOutSlowInEasing),
        label = "escalaIsotipo",
    )
    val opacidadIsotipo by animateFloatAsState(
        targetValue = if (entro) 1f else 0f,
        animationSpec = tween(durationMillis = 520),
        label = "opacidadIsotipo",
    )
    val opacidadTexto by animateFloatAsState(
        targetValue = if (entro) 1f else 0f,
        animationSpec = tween(durationMillis = 520, delayMillis = 260),
        label = "opacidadTexto",
    )
    val desplazamientoTexto by animateDpAsState(
        targetValue = if (entro) 0.dp else 12.dp,
        animationSpec = tween(durationMillis = 520, delayMillis = 260, easing = FastOutSlowInEasing),
        label = "desplazamientoTexto",
    )

    val version = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull()
    }

    LaunchedEffect(Unit) {
        val minimo = launch { delay(TIEMPO_MINIMO_MS) }
        val avance = launch {
            relleno.animateTo(
                targetValue = TOPE_BARRA,
                animationSpec = tween(
                    durationMillis = (TIEMPO_MINIMO_MS - RETARDO_BARRA_MS).toInt(),
                    delayMillis = RETARDO_BARRA_MS.toInt(),
                    easing = LinearEasing,
                ),
            )
        }

        withTimeoutOrNull(TIEMPO_MAXIMO_MS) { AssetWarmup.precargar(context) }
        minimo.join()

        avance.cancel()
        relleno.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        )
        delay(140)
        onFinished()
    }

    Box(
        Modifier
            .fillMaxSize()
            .fondoDeMarca()
            .clipToBounds()
    ) {
        SenarConstelacion(
            modifier = Modifier
                .size(420.dp)
                .align(Alignment.TopStart)
                .offset(x = (-120).dp, y = (-70).dp),
            colorTrazo = SenarAzul200,
            colorPunto = SenarAzul100,
            opacidad = 0.16f,
        )
        SenarBurbujasContorno(
            modifier = Modifier
                .size(380.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 140.dp, y = 90.dp),
            color = SenarAzul200,
            opacidad = 0.09f,
        )

        Box(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-40).dp)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SenarIsotipo(
                    ancho = 186.dp,
                    modifier = Modifier.graphicsLayer {
                        scaleX = escalaIsotipo
                        scaleY = escalaIsotipo
                        alpha = opacidadIsotipo
                    },
                    burbujaVoz = SenarAzul100,
                    glifoVoz = SenarGrafito900,
                )
                Spacer(Modifier.height(34.dp))
                SenarLogotipo(
                    estilo = MaterialTheme.typography.displayLarge,
                    colorTexto = SenarBlanco,
                    colorEne = SenarAzul300,
                    modifier = Modifier
                        .offset(y = desplazamientoTexto)
                        .graphicsLayer { alpha = opacidadTexto },
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Traductor de Lengua de Señas Argentina",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = SenarSobreMarca,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .offset(y = desplazamientoTexto)
                        .graphicsLayer { alpha = opacidadTexto },
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 44.dp)
                    .graphicsLayer { alpha = opacidadTexto },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .width(148.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(SenarAzul200.copy(alpha = 0.22f))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(relleno.value)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(SenarAzul500)
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Preparando el reconocimiento…",
                    style = MaterialTheme.typography.bodySmall,
                    color = SenarSobreMarcaSuave,
                )
                if (version != null) {
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = "V $version",
                        style = MaterialTheme.typography.labelSmall,
                        color = SenarSobreMarcaSuave.copy(alpha = 0.65f),
                    )
                }
            }
        }
    }
}
