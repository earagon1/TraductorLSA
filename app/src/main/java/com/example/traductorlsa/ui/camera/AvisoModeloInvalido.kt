package com.example.traductorlsa.ui.camera

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Lo que muestra la cámara cuando el modelo no corresponde a la app (auditoría, X3): en
 * vez de traducir con etiquetas que pueden ser de otra seña, dice qué no coincide.
 * [esPaquete]: el modelo es uno bajado del federado, y se puede volver al de fábrica.
 */
@Composable
fun AvisoModeloInvalido(problemas: List<String>, esPaquete: Boolean, onVolver: () -> Unit) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "El modelo no corresponde a esta versión de la app",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "Para no mostrar una seña equivocada, la cámara no traduce hasta que se corrija.",
            style = MaterialTheme.typography.bodyMedium,
        )
        problemas.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
        Text(
            if (esPaquete) "Podés volver al modelo de fábrica en Aprendizaje federado."
            else "Hay que volver a publicar el modelo con publicar_modelo.py y reinstalar la app.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(onClick = onVolver) { Text("Volver") }
    }
}
