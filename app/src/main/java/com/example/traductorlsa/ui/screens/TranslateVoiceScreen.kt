package com.example.traductorlsa.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.example.traductorlsa.settings.ajustesSenar
import com.example.traductorlsa.ui.AppDestination
import com.example.traductorlsa.ui.brand.SenarIconoOnda
import com.example.traductorlsa.ui.components.Aire
import com.example.traductorlsa.ui.components.PantallaSenar
import com.example.traductorlsa.ui.components.TituloDePagina
import com.example.traductorlsa.ui.theme.SenarBlanco
import com.example.traductorlsa.ui.theme.SenarBorde
import com.example.traductorlsa.ui.theme.SenarBordeSuave
import com.example.traductorlsa.ui.theme.SenarGrafito300
import com.example.traductorlsa.ui.theme.SenarGrafito500
import com.example.traductorlsa.ui.theme.SenarGrafito700
import com.example.traductorlsa.ui.theme.SenarGrafito900
import com.example.traductorlsa.ui.theme.SenarPapelHundido
import com.example.traductorlsa.voice.VoiceToText

private val ROJO_DETENER = androidx.compose.ui.graphics.Color(0xFFC0392B)

@Composable
fun TranslateVoiceScreen(navController: NavHostController) {
    val context = LocalContext.current
    val ajustes by ajustesSenar()

    // Se separan para poder mostrar lo confirmado en negro y lo provisorio en
    // gris: la distinción ya la hace VoiceToText, faltaba que se viera.
    var textoFinal by remember { mutableStateOf("") }
    var textoParcial by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var escuchando by remember { mutableStateOf(false) }

    val vtt = remember {
        VoiceToText(
            context = context,
            onPartial = { textoParcial = it },
            onFinal = {
                if (it.isNotBlank()) {
                    textoFinal = if (textoFinal.isBlank()) it else "$textoFinal $it"
                }
                textoParcial = ""
                escuchando = false
            },
            onError = {
                error = it
                textoParcial = ""
                escuchando = false
            },
        )
    }

    val pedirPermiso = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            error = null
            escuchando = true
            vtt.start(ajustes.variante.etiquetaBcp47)
        } else {
            error = "Sin permiso de micrófono no se puede escuchar."
        }
    }

    DisposableEffect(Unit) {
        onDispose { vtt.stop() }
    }

    val hayTexto = textoFinal.isNotBlank() || textoParcial.isNotBlank()

    PantallaSenar(
        onAvatar = { navController.navigate(AppDestination.Settings.route) },
        desplazable = false,
    ) {
        TituloDePagina(
            texto = "Traducir voz",
            onVolver = {
                vtt.stop()
                navController.popBackStack()
            },
        )

        Aire(20)
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(SenarBlanco)
                .border(1.dp, SenarBordeSuave, RoundedCornerShape(28.dp))
                .padding(24.dp)
        ) {
            if (escuchando) {
                IndicadorEscuchando()
                Aire(22)
            }

            if (!hayTexto) {
                EstadoVacio(Modifier.weight(1f))
            } else {
                Text(
                    text = buildAnnotatedString {
                        append(textoFinal)
                        if (textoParcial.isNotBlank()) {
                            if (textoFinal.isNotBlank()) append(" ")
                            withStyle(SpanStyle(color = SenarGrafito300)) { append(textoParcial) }
                        }
                    },
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 26.sp,
                        lineHeight = 35.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = SenarGrafito900,
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                )
            }

            val nota = error ?: if (textoParcial.isNotBlank()) "El texto en gris todavía se está confirmando" else null
            if (nota != null) {
                Aire(18)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(SenarPapelHundido)
                )
                Aire(14)
                Text(
                    text = nota,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                    color = if (error != null) ROJO_DETENER else SenarGrafito500,
                )
            }
        }

        if (hayTexto) {
            Aire(14)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Surface(
                    onClick = {
                        textoFinal = ""
                        textoParcial = ""
                        error = null
                    },
                    shape = CircleShape,
                    color = SenarBlanco,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SenarBorde),
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = null,
                            tint = SenarGrafito500,
                            modifier = Modifier.size(17.dp),
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = "Limpiar",
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.5.sp),
                            color = SenarGrafito500,
                        )
                    }
                }
            }
        }

        Aire(16)
        BotonEscuchar(
            escuchando = escuchando,
            onClick = {
                if (escuchando) {
                    escuchando = false
                    vtt.stop()
                } else {
                    val tienePermiso = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED

                    if (tienePermiso) {
                        error = null
                        escuchando = true
                        vtt.start(ajustes.variante.etiquetaBcp47)
                    } else {
                        pedirPermiso.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            },
        )
    }
}

@Composable
private fun IndicadorEscuchando() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            listOf(7, 16, 11, 5).forEach { alto ->
                Box(
                    Modifier
                        .width(3.dp)
                        .height(alto.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(SenarGrafito700)
                )
            }
        }
        Text(
            text = "ESCUCHANDO",
            style = MaterialTheme.typography.labelSmall,
            color = SenarGrafito300,
        )
    }
}

@Composable
private fun EstadoVacio(modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(SenarPapelHundido),
            contentAlignment = Alignment.Center,
        ) {
            SenarIconoOnda(tamano = 46.dp, color = SenarGrafito300)
        }
        Aire(20)
        Text(
            text = "Acá vas a leer lo que digan",
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 19.sp),
            color = SenarGrafito900,
            textAlign = TextAlign.Center,
        )
        Aire(8)
        Text(
            text = "Tocá el micrófono y pedile a la otra persona que hable.",
            style = MaterialTheme.typography.bodySmall,
            color = SenarGrafito500,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

@Composable
private fun BotonEscuchar(escuchando: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(20.dp),
        color = if (escuchando) SenarBlanco else SenarGrafito700,
        border = if (escuchando) androidx.compose.foundation.BorderStroke(1.5.dp, SenarBorde) else null,
        shadowElevation = if (escuchando) 0.dp else 8.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = if (escuchando) Icons.Filled.Stop else Icons.Filled.Mic,
                contentDescription = null,
                tint = if (escuchando) ROJO_DETENER else SenarBlanco,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = if (escuchando) "Detener" else "Escuchar",
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 18.sp),
                color = if (escuchando) SenarGrafito900 else SenarBlanco,
            )
        }
    }
}
