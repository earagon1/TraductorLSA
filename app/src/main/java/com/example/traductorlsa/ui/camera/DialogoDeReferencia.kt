package com.example.traductorlsa.ui.camera

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.traductorlsa.ui.screens.DictionaryAssetImage
import com.example.traductorlsa.ui.screens.VideoDeSena
import com.example.traductorlsa.ui.screens.findWordVideoInAssets
import com.example.traductorlsa.ui.screens.nombreParaMostrar
import com.example.traductorlsa.ui.theme.SenarBlanco

/**
 * La referencia de la seña que se está entrenando, en un diálogo: el mismo video del
 * diccionario, que es lo que muestra cómo se hace. Si la seña no tiene video o no se
 * puede reproducir, queda la imagen. Salió de ui/CameraScreen.kt (T-A20).
 */
@Composable
internal fun TrainingReferenceDialog(
    label: String,
    assetPath: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val nombre = nombreParaMostrar(label)
    val video = remember(label) { findWordVideoInAssets(context.assets, label) }
    var videoFallo by remember(label) { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        // El ancho tiene tope: en una tablet, a todo el ancho de la pantalla el video vertical
        // ocupaba el alto entero y tapaba la cámara.
        Surface(
            modifier = Modifier.widthIn(max = 360.dp),
            shape = RoundedCornerShape(28.dp),
            color = SenarBlanco,
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(nombre, style = MaterialTheme.typography.titleLarge)
                if (video != null && !videoFallo) {
                    VideoDeSena(
                        assetPath = video,
                        descripcion = "Video de cómo se hace la seña $nombre. Tocá para pausar.",
                        onFalla = { videoFallo = true },
                        // Ocupa todo el ancho del cartel: con un tope de alto el video quedaba
                        // angosto, con una franja blanca a cada lado.
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    DictionaryAssetImage(
                        assetPath = assetPath,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(20.dp)),
                        descripcion = "Imagen de cómo se hace la seña $nombre",
                    )
                }
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Cerrar")
                }
            }
        }
    }
}
