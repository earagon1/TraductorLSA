package com.example.traductorlsa.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.traductorlsa.ui.AppDestination
import com.example.traductorlsa.ui.brand.SenarIsotipo
import com.example.traductorlsa.ui.brand.SenarLogotipo
import com.example.traductorlsa.ui.components.Aire
import com.example.traductorlsa.ui.components.PantallaSenar
import com.example.traductorlsa.ui.components.TituloDePagina
import com.example.traductorlsa.ui.theme.SenarAzul050
import com.example.traductorlsa.ui.theme.SenarAzul600
import com.example.traductorlsa.ui.theme.SenarAzul700
import com.example.traductorlsa.ui.theme.SenarBlanco
import com.example.traductorlsa.ui.theme.SenarBordeSuave
import com.example.traductorlsa.ui.theme.SenarGrafito300
import com.example.traductorlsa.ui.theme.SenarGrafito500
import com.example.traductorlsa.ui.theme.SenarGrafito900

private val TECNOLOGIAS = listOf(
    "Kotlin", "Jetpack Compose", "CameraX", "MediaPipe", "TensorFlow Lite",
)

@Composable
fun AboutScreen(navController: NavHostController) {
    val context = LocalContext.current
    val version = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull()
    }

    PantallaSenar(onAvatar = { navController.navigate(AppDestination.Settings.route) }) {
        TituloDePagina(texto = "Acerca de", onVolver = { navController.popBackStack() })

        Aire(36)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SenarIsotipo(ancho = 132.dp)
            Aire(24)
            SenarLogotipo(
                estilo = MaterialTheme.typography.displayMedium.copy(fontSize = 38.sp),
                colorTexto = SenarGrafito900,
                colorEne = SenarAzul600,
            )
            if (version != null) {
                Aire(14)
                Surface(shape = CircleShape, color = SenarAzul050) {
                    Text(
                        text = "Versión $version",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = SenarAzul700,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
        }

        Aire(36)
        Tarjeta {
            Text(
                text = "Traductor de Lengua de Señas Argentina a texto y voz.",
                style = MaterialTheme.typography.bodyLarge,
                color = SenarGrafito900,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Aire(10)
            Text(
                text = "Proyecto de tesina de Evelin Aragón",
                style = MaterialTheme.typography.bodyMedium,
                color = SenarGrafito500,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Aire(26)
        Text(
            text = "CONSTRUIDA CON",
            style = MaterialTheme.typography.labelSmall,
            color = SenarGrafito300,
        )
        Aire(12)
        Tarjeta {
            // Se envuelven a mano en dos filas: FlowRow todavía es experimental.
            FilaDeChips(TECNOLOGIAS.take(2))
            Aire(8)
            FilaDeChips(TECNOLOGIAS.drop(2))
        }

        Aire(26)
        Text(
            text = "El reconocimiento corre en tu dispositivo. No se sube ningún video a la nube.",
            style = MaterialTheme.typography.bodySmall,
            color = SenarGrafito500,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        Aire(24)
    }
}

@Composable
private fun Tarjeta(contenido: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(SenarBlanco)
            .padding(20.dp),
        content = contenido,
    )
}

@Composable
private fun FilaDeChips(etiquetas: List<String>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        etiquetas.forEach { etiqueta ->
            Surface(
                shape = CircleShape,
                color = SenarBlanco,
                border = BorderStroke(1.dp, SenarBordeSuave),
            ) {
                Text(
                    text = etiqueta,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = SenarGrafito500,
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                )
            }
        }
    }
}
