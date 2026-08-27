package com.example.traductorlsa.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.traductorlsa.ui.theme.SenarBlanco
import com.example.traductorlsa.ui.theme.SenarBorde
import com.example.traductorlsa.ui.theme.SenarGrafito700
import com.example.traductorlsa.ui.theme.SenarGrafito900
import com.example.traductorlsa.ui.theme.SenarPapel
import com.example.traductorlsa.ui.theme.SenarSystemBars

/**
 * Andamio de las pantallas internas: fondo, barra fija y respeto de las barras
 * del sistema. Todo lo que va adentro queda debajo del logotipo y el avatar.
 */
@Composable
fun PantallaSenar(
    onAvatar: () -> Unit,
    modifier: Modifier = Modifier,
    desplazable: Boolean = true,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    SenarSystemBars(iconosOscuros = true)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SenarPapel)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(start = 24.dp, end = 24.dp, top = 10.dp, bottom = 24.dp)
    ) {
        // La barra queda fija: solo se desplaza lo de abajo.
        SenarBarra(onAvatar = onAvatar)
        Column(
            modifier = Modifier
                .weight(1f)
                .then(if (desplazable) Modifier.verticalScroll(rememberScrollState()) else Modifier),
            content = contenido,
        )
    }
}

/** Botón de volver y título, en su propia línea debajo de la barra fija. */
@Composable
fun TituloDePagina(
    texto: String,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            onClick = onVolver,
            modifier = Modifier.size(44.dp),
            shape = MaterialTheme.shapes.small,
            color = SenarBlanco,
            border = BorderStroke(1.dp, SenarBorde),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = SenarGrafito700,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Text(
            text = texto,
            style = MaterialTheme.typography.headlineSmall,
            color = SenarGrafito900,
        )
    }
}

/** Separación vertical, para no repetir Spacer(Modifier.height(...)). */
@Composable
fun Aire(alto: Int) = Spacer(Modifier.height(alto.dp))
