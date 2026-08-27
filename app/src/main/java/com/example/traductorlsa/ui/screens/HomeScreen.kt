package com.example.traductorlsa.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clerk.api.Clerk
import com.example.traductorlsa.ui.brand.SenarIconoMano
import com.example.traductorlsa.ui.brand.SenarIconoOnda
import com.example.traductorlsa.ui.components.Aire
import com.example.traductorlsa.ui.components.FilaDestino
import com.example.traductorlsa.ui.components.PantallaSenar
import com.example.traductorlsa.ui.components.TarjetaDeModo
import com.example.traductorlsa.ui.theme.SenarAzul600
import com.example.traductorlsa.ui.theme.SenarBlanco
import com.example.traductorlsa.ui.theme.SenarGrafito300
import com.example.traductorlsa.ui.theme.SenarGrafito700
import com.example.traductorlsa.ui.theme.SenarGrafito900
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onTranslateSign: () -> Unit,
    onTranslateVoice: () -> Unit,
    onTraining: () -> Unit,
    onDictionary: () -> Unit,
    onSettings: () -> Unit,
) {
    val user by Clerk.userFlow.collectAsStateWithLifecycle()

    // El avatar de la barra es la puerta a Ajustes: ahí adentro está la cuenta.
    PantallaSenar(onAvatar = onSettings) {
        Aire(26)
        Text(
            text = "¿Qué querés hacer hoy?",
            style = MaterialTheme.typography.headlineSmall,
            color = SenarGrafito900,
        )

        Aire(24)
        TarjetaDeModo(
            fondo = SenarAzul600,
            titulo = "Traducir señas",
            descripcion = "Hacé la seña y la app la escribe y la dice",
            onClick = onTranslateSign,
        ) {
            SenarIconoMano(tamano = 40.dp, color = SenarBlanco)
        }

        Aire(14)
        TarjetaDeModo(
            fondo = SenarGrafito700,
            titulo = "Traducir voz",
            descripcion = "Leé en pantalla lo que dice la otra persona",
            onClick = onTranslateVoice,
        ) {
            SenarIconoOnda(tamano = 40.dp, color = SenarBlanco)
        }

        Aire(32)
        Text(
            text = "TAMBIÉN PODÉS",
            style = MaterialTheme.typography.labelSmall,
            color = SenarGrafito300,
        )

        Aire(12)
        FilaDestino(
            icono = Icons.Filled.MenuBook,
            titulo = "Diccionario de señas",
            descripcion = "Ver las señas que reconoce el modelo",
            onClick = onDictionary,
        )

        // El modo entrenamiento sigue detrás de la sesión, como estaba.
        if (user != null) {
            Aire(12)
            FilaDestino(
                icono = Icons.Filled.School,
                titulo = "Modo entrenamiento",
                descripcion = "Grabar muestras y revisar el dataset",
                onClick = onTraining,
            )
        }

        Aire(24)
    }
}
