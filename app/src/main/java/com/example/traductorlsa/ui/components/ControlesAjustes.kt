package com.example.traductorlsa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.traductorlsa.ui.theme.SenarAzul600
import com.example.traductorlsa.ui.theme.SenarBlanco
import com.example.traductorlsa.ui.theme.SenarBordeSuave
import com.example.traductorlsa.ui.theme.SenarGrafito300
import com.example.traductorlsa.ui.theme.SenarGrafito500
import com.example.traductorlsa.ui.theme.SenarGrafito900
import com.example.traductorlsa.ui.theme.SenarPapelHundido

/** Rótulo de sección más la tarjeta blanca que agrupa sus controles. */
@Composable
fun SeccionAjustes(
    titulo: String,
    modifier: Modifier = Modifier,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.padding(top = 26.dp)) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.labelSmall,
            color = SenarGrafito300,
        )
        Aire(12)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(SenarBlanco)
                .border(1.dp, SenarBordeSuave, RoundedCornerShape(22.dp)),
            content = contenido,
        )
    }
}

/** Una fila dentro de una sección. La última no lleva divisor. */
@Composable
fun CeldaAjuste(
    ultima: Boolean = false,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.padding(18.dp), content = contenido)
    if (!ultima) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(SenarPapelHundido)
        )
    }
}

/** Título de un control, con su valor actual a la derecha. */
@Composable
fun EtiquetaAjuste(
    titulo: String,
    valor: String? = null,
    accesorio: @Composable RowScope.() -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 15.5.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = SenarGrafito900,
            modifier = Modifier.weight(1f, fill = false),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (valor != null) {
                Text(
                    text = valor,
                    style = MaterialTheme.typography.labelMedium,
                    color = SenarAzul600,
                )
            }
            accesorio()
        }
    }
}

/** Texto de ayuda debajo de un control: para qué sirve y cuándo moverlo. */
@Composable
fun AyudaAjuste(texto: String) {
    Aire(8)
    Text(
        text = texto,
        style = MaterialTheme.typography.bodySmall,
        color = SenarGrafito500,
    )
}

@Composable
fun FilaInterruptor(
    titulo: String,
    ayuda: String,
    marcado: Boolean,
    onCambio: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = SenarGrafito900,
            )
            AyudaAjuste(ayuda)
        }
        Switch(checked = marcado, onCheckedChange = onCambio)
    }
}

@Composable
fun DeslizadorAjuste(
    valor: Float,
    rango: ClosedFloatingPointRange<Float>,
    onCambio: (Float) -> Unit,
    pasos: Int = 0,
) {
    Slider(
        value = valor,
        onValueChange = onCambio,
        valueRange = rango,
        steps = pasos,
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * Selector de opciones excluyentes. Hecho a mano en lugar de usar el de
 * Material para que siga el radio y los colores del sistema.
 */
@Composable
fun SelectorSegmentado(
    opciones: List<String>,
    seleccionada: Int,
    onSeleccion: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(SenarPapelHundido)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        opciones.forEachIndexed { indice, opcion ->
            val activa = indice == seleccionada
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (activa) SenarBlanco else SenarPapelHundido)
                    .clickable { onSeleccion(indice) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = opcion,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.5.sp),
                    color = if (activa) SenarGrafito900 else SenarGrafito300,
                )
            }
        }
    }
}
