package com.example.traductorlsa.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.clerk.api.Clerk
import com.example.traductorlsa.R
import com.example.traductorlsa.ui.AppDestination
import com.example.traductorlsa.ui.brand.SenarBurbujasContorno
import com.example.traductorlsa.ui.brand.SenarConstelacion
import com.example.traductorlsa.ui.brand.SenarIsotipo
import com.example.traductorlsa.ui.brand.SenarLogotipo
import com.example.traductorlsa.ui.brand.fondoDeMarca
import com.example.traductorlsa.ui.theme.SenarAzul100
import com.example.traductorlsa.ui.theme.SenarAzul200
import com.example.traductorlsa.ui.theme.SenarAzul300
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

    // Si ya hay sesión abierta, no tiene sentido mostrar esta pantalla.
    LaunchedEffect(isInitialized, user) {
        if (isInitialized && user != null) {
            navController.navigate(AppDestination.Home.route) {
                popUpTo(AppDestination.AuthEntry.route) { inclusive = true }
            }
        }
    }

    val entrarComoInvitada = {
        navController.navigate(AppDestination.Home.route) {
            popUpTo(AppDestination.AuthEntry.route) { inclusive = true }
        }
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

            Button(
                onClick = entrarComoInvitada,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SenarBlanco,
                    contentColor = SenarAzul900,
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
                onClick = { navController.navigate(AppDestination.AuthClerk.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SenarBlanco.copy(alpha = 0.10f),
                    contentColor = SenarBlanco,
                ),
                border = BorderStroke(1.5.dp, SenarAzul100.copy(alpha = 0.34f)),
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

            Spacer(Modifier.height(22.dp))
            // TODO: enlazar a las páginas de términos y privacidad cuando existan.
            Text(
                text = buildAnnotatedString {
                    append("Al continuar aceptás los ")
                    withStyle(SpanStyle(color = SenarAzul300, fontWeight = FontWeight.SemiBold)) {
                        append("Términos")
                    }
                    append(" y la ")
                    withStyle(SpanStyle(color = SenarAzul300, fontWeight = FontWeight.SemiBold)) {
                        append("Política de privacidad")
                    }
                    append(".")
                },
                style = MaterialTheme.typography.bodySmall,
                color = SenarSobreMarcaSuave,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
