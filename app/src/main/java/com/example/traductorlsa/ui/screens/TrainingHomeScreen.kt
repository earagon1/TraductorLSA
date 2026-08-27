package com.example.traductorlsa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.traductorlsa.ui.AppDestination
import com.example.traductorlsa.ui.components.Aire
import com.example.traductorlsa.ui.components.PantallaSenar
import com.example.traductorlsa.ui.components.TituloDePagina
import com.example.traductorlsa.ui.theme.SenarAzul050
import com.example.traductorlsa.ui.theme.SenarAzul600
import com.example.traductorlsa.ui.theme.SenarAzul700
import com.example.traductorlsa.ui.theme.SenarBlanco
import com.example.traductorlsa.ui.theme.SenarBordeSuave
import com.example.traductorlsa.ui.theme.SenarGrafito500
import com.example.traductorlsa.ui.theme.SenarGrafito900

@Composable
fun TrainingHomeScreen(navController: NavHostController) {
    PantallaSenar(onAvatar = { navController.navigate(AppDestination.Settings.route) }) {
        TituloDePagina(
            texto = "Modo entrenamiento",
            onVolver = { navController.popBackStack() },
        )

        Aire(20)
        Text(
            text = "Este modo es para ampliar el modelo: grabás la misma seña varias veces y esas muestras se guardan para reentrenarlo después.",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp),
            color = SenarGrafito500,
        )

        Aire(24)
        TarjetaEntrenamiento(
            fondo = SenarAzul600,
            fondoIcono = SenarBlanco.copy(alpha = 0.17f),
            icono = Icons.Filled.Videocam,
            tinteIcono = SenarBlanco,
            titulo = "Grabar nuevas muestras",
            descripcion = "Capturar gestos para ampliar el dataset",
            colorTitulo = SenarBlanco,
            colorDescripcion = SenarBlanco.copy(alpha = 0.78f),
            onClick = { navController.navigate(AppDestination.TrainingCapture.route) },
        )

        Aire(14)
        TarjetaEntrenamiento(
            fondo = SenarBlanco,
            fondoIcono = SenarAzul050,
            icono = Icons.Filled.TableChart,
            tinteIcono = SenarAzul600,
            titulo = "Ver dataset",
            descripcion = "Etiquetas y cantidad de muestras",
            colorTitulo = SenarGrafito900,
            colorDescripcion = SenarGrafito500,
            conBorde = true,
            onClick = { navController.navigate(AppDestination.Dataset.route) },
        )

        Aire(26)
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SenarAzul050)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = SenarAzul700,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "Las muestras se exportan como JSON para reentrenar el modelo en Python. Nada se sube a ningún lado.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.5.sp),
                color = SenarAzul700,
            )
        }

        Aire(24)
    }
}

@Composable
private fun TarjetaEntrenamiento(
    fondo: Color,
    fondoIcono: Color,
    icono: ImageVector,
    tinteIcono: Color,
    titulo: String,
    descripcion: String,
    colorTitulo: Color,
    colorDescripcion: Color,
    onClick: () -> Unit,
    conBorde: Boolean = false,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(124.dp),
        shape = RoundedCornerShape(24.dp),
        color = fondo,
        border = if (conBorde) androidx.compose.foundation.BorderStroke(1.dp, SenarBordeSuave) else null,
    ) {
        Row(
            Modifier.padding(22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(fondoIcono),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    tint = tinteIcono,
                    modifier = Modifier.size(30.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 17.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = colorTitulo,
                )
                Aire(6)
                Text(
                    text = descripcion,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.5.sp),
                    color = colorDescripcion,
                )
            }
        }
    }
}
