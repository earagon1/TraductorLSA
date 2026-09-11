package com.example.traductorlsa.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.clerk.api.Clerk
import com.example.traductorlsa.R
import com.example.traductorlsa.settings.EstadoDeEntrada
import com.example.traductorlsa.ui.AppDestination
import com.example.traductorlsa.ui.destinoDespuesDelAcceso
import com.example.traductorlsa.ui.brand.SenarBurbujasContorno
import com.example.traductorlsa.ui.brand.SenarConstelacion
import com.example.traductorlsa.ui.brand.SenarIsotipo
import com.example.traductorlsa.ui.brand.SenarLogotipo
import com.example.traductorlsa.ui.brand.fondoDeMarca
import com.example.traductorlsa.ui.theme.SenarAmbar
import com.example.traductorlsa.ui.theme.SenarAzul100
import com.example.traductorlsa.ui.theme.SenarAzul200
import com.example.traductorlsa.ui.theme.SenarAzul300
import com.example.traductorlsa.ui.theme.SenarAzul500
import com.example.traductorlsa.ui.theme.SenarAzul900
import com.example.traductorlsa.ui.theme.SenarBlanco
import com.example.traductorlsa.ui.theme.SenarGrafito900
import com.example.traductorlsa.ui.theme.SenarSobreMarca
import com.example.traductorlsa.ui.theme.SenarSobreMarcaSuave
import com.example.traductorlsa.ui.theme.SenarSystemBars

@Composable
fun AuthEntryScreen(navController: NavHostController) {
    SenarSystemBars(iconosOscuros = false)

    val isInitialized by Clerk.isInitialized.collectAsStateWithLifecycle(false)
    val user by Clerk.userFlow.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val estadoDeEntrada = remember(context) { EstadoDeEntrada.de(context) }

    // Si ya hay sesión abierta, no tiene sentido mostrar esta pantalla.
    LaunchedEffect(isInitialized, user) {
        if (isInitialized && user != null) {
            navController.navigate(destinoDespuesDelAcceso(estadoDeEntrada)) {
                popUpTo(AppDestination.AuthEntry.route) { inclusive = true }
            }
        }
    }

    // Arranca vacía siempre: el consentimiento dura lo que dura la visita a esta
    // pantalla. Sembrarla del valor guardado la mostraba tildada de entrada y sin
    // forma de destildarla, que es justo lo contrario de un consentimiento.
    var acepto by rememberSaveable { mutableStateOf(false) }
    var mostrarPolitica by rememberSaveable { mutableStateOf(false) }
    var faltaAceptar by rememberSaveable { mutableStateOf(false) }

    val entrarComoInvitada = {
        // El modo invitada no deja sesión y no se recuerda: la próxima vez esta
        // pantalla vuelve a preguntar. El tutorial sí, que es sobre la app.
        navController.navigate(destinoDespuesDelAcceso(estadoDeEntrada)) {
            popUpTo(AppDestination.AuthEntry.route) { inclusive = true }
        }
    }

    // Los botones no se deshabilitan: se ven apagados pero responden, y al
    // tocarlos sin aceptar explican por qué. Un botón deshabilitado de verdad no
    // dice nada y con TalkBack se anuncia «desactivado» sin motivo.
    fun siAcepto(accion: () -> Unit) {
        if (acepto) accion() else faltaAceptar = true
    }

    Box(
        Modifier
            .fillMaxSize()
            .fondoDeMarca()
            .clipToBounds()
    ) {
        SenarConstelacion(
            modifier = Modifier
                .size(336.dp)
                .align(Alignment.TopEnd)
                .offset(x = 104.dp, y = (-40).dp),
            colorTrazo = SenarAzul200,
            colorPunto = SenarAzul100,
            opacidad = 0.15f,
        )
        SenarBurbujasContorno(
            modifier = Modifier
                .size(262.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-86).dp, y = (-150).dp),
            color = SenarAzul200,
            opacidad = 0.07f,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 34.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                SenarIsotipo(
                    ancho = 156.dp,
                    burbujaVoz = SenarAzul100,
                    glifoVoz = SenarGrafito900,
                )
                Spacer(Modifier.height(28.dp))
                SenarLogotipo(
                    estilo = MaterialTheme.typography.displayMedium,
                    colorTexto = SenarBlanco,
                    colorEne = SenarAzul300,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Comunicá en Lengua de Señas Argentina",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = SenarSobreMarca,
                    textAlign = TextAlign.Center,
                )
            }

            FilaDeConsentimiento(
                acepto = acepto,
                alertado = faltaAceptar,
                onAbrir = { mostrarPolitica = true },
            )

            Spacer(Modifier.height(18.dp))
            Button(
                onClick = { siAcepto(entrarComoInvitada) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (acepto) SenarBlanco else SenarBlanco.copy(alpha = 0.32f),
                    contentColor = if (acepto) SenarAzul900 else SenarAzul900.copy(alpha = 0.55f),
                ),
            ) {
                Text(
                    text = "Continuar sin cuenta",
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.width(10.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(21.dp),
                )
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = "Traducí ahora mismo, en modo invitada",
                style = MaterialTheme.typography.bodySmall,
                color = SenarSobreMarcaSuave,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(SenarAzul100.copy(alpha = 0.20f))
                )
                Text(
                    text = "o sincronizá tu progreso",
                    style = MaterialTheme.typography.bodySmall,
                    color = SenarSobreMarcaSuave,
                    modifier = Modifier.padding(horizontal = 14.dp),
                )
                Box(
                    Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(SenarAzul100.copy(alpha = 0.20f))
                )
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    siAcepto { navController.navigate(AppDestination.AuthClerk.route) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SenarBlanco.copy(alpha = if (acepto) 0.10f else 0.05f),
                    contentColor = SenarBlanco.copy(alpha = if (acepto) 1f else 0.55f),
                ),
                border = BorderStroke(1.5.dp, SenarAzul100.copy(alpha = if (acepto) 0.34f else 0.14f)),
            ) {
                // Marca de Google: va con sus colores originales, sin tinte.
                Image(
                    painter = painterResource(R.drawable.ic_google),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Continuar con Google",
                    style = MaterialTheme.typography.labelLarge,
                )
            }

        }
    }

    if (mostrarPolitica) {
        HojaDePrivacidad(
            onCerrar = { mostrarPolitica = false },
            aceptadaAlAbrir = acepto,
            onAceptar = {
                acepto = true
                faltaAceptar = false
                mostrarPolitica = false
            },
        )
    }
}

/**
 * La línea de consentimiento del acceso.
 *
 * Una sola línea, para no taparle la primera impresión a la pantalla: el texto
 * completo vive en la hoja que abre. La casilla no se marca desde acá — hay que
 * leer la política — y por eso la fila lleva la flecha de «esto abre algo».
 */
@Composable
private fun FilaDeConsentimiento(
    acepto: Boolean,
    alertado: Boolean,
    onAbrir: () -> Unit,
) {
    val forma = MaterialTheme.shapes.medium
    val borde = when {
        alertado && !acepto -> SenarAmbar
        acepto -> SenarAzul300.copy(alpha = 0.38f)
        else -> SenarAzul100.copy(alpha = 0.20f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(forma)
            .background(
                if (acepto) SenarAzul500.copy(alpha = 0.12f) else SenarBlanco.copy(alpha = 0.06f)
            )
            .border(1.dp, borde, forma)
            .clickable(onClick = onAbrir)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(23.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .then(
                        if (acepto) Modifier.background(SenarAzul500)
                        else Modifier.border(
                            2.dp,
                            if (alertado) SenarAmbar else SenarAzul100.copy(alpha = 0.45f),
                            MaterialTheme.shapes.extraSmall,
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (acepto) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = SenarBlanco,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = if (acepto) {
                    AnnotatedString("Política de privacidad aceptada")
                } else {
                    buildAnnotatedString {
                        append("Acepto la ")
                        withStyle(
                            SpanStyle(
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.SemiBold,
                            )
                        ) {
                            append("política de privacidad")
                        }
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = SenarAzul100,
                modifier = Modifier.weight(1f),
            )
            if (!acepto) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Leer la política",
                    tint = SenarSobreMarcaSuave,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        if (alertado && !acepto) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.padding(start = 35.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.ErrorOutline,
                    contentDescription = null,
                    tint = SenarAmbar,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = "Leela y aceptala para continuar",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = SenarAmbar,
                )
            }
        }
    }
}
