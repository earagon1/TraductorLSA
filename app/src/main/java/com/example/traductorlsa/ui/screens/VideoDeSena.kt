package com.example.traductorlsa.ui.screens

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.util.Log
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.traductorlsa.ui.theme.SenarBlanco

/**
 * El video de una seña, desde los assets: arranca solo, se repite y se pausa al tocarlo.
 *
 * Se repite porque una seña dura dos o tres segundos y para aprenderla hay que verla
 * varias veces; tocar para pausar deja mirar la configuración de la mano con calma.
 * No tiene sonido ni controles: no hay nada que escuchar ni tiene sentido adelantarlo.
 *
 * Usa MediaPlayer y no una librería de video porque alcanza para un archivo local corto
 * y no suma dependencias. Si el video no se puede reproducir, avisa por [onFalla] para
 * que la pantalla muestre la imagen en su lugar.
 */
@Composable
internal fun VideoDeSena(
    assetPath: String,
    descripcion: String,
    onFalla: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reproductor = remember(assetPath) { ReproductorDeSena() }
    var proporcion by remember(assetPath) { mutableFloatStateOf(PROPORCION_INICIAL) }
    var visible by remember(assetPath) { mutableStateOf(false) }
    var pausado by remember(assetPath) { mutableStateOf(false) }

    DisposableEffect(reproductor) {
        onDispose { reproductor.soltar() }
    }

    Box(modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .aspectRatio(proporcion)
                .clip(RoundedCornerShape(18.dp))
                .semantics { contentDescription = descripcion }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    // Sin onda al tocar: taparía justo la mano que se quiere mirar.
                    indication = null,
                    onClickLabel = if (pausado) "Reproducir" else "Pausar",
                ) { pausado = reproductor.alternarPausa() },
            contentAlignment = Alignment.Center,
        ) {
            AndroidView(
                // Invisible hasta el primer cuadro: antes de eso la textura está vacía o
                // estirada, y se vería un parpadeo negro.
                modifier = Modifier.alpha(if (visible) 1f else 0f),
                factory = { contexto ->
                    TextureView(contexto).apply {
                        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                            override fun onSurfaceTextureAvailable(textura: SurfaceTexture, ancho: Int, alto: Int) {
                                reproductor.abrir(
                                    abrirAsset = { contexto.assets.openFd(assetPath) },
                                    superficie = Surface(textura),
                                    onTamano = { w, h -> proporcionDeVideo(w, h)?.let { proporcion = it } },
                                    onPrimerCuadro = { visible = true },
                                    onFalla = {
                                        Log.w("Dictionary", "No se pudo reproducir el video: $assetPath")
                                        onFalla()
                                    },
                                )
                                pausado = false
                            }

                            override fun onSurfaceTextureSizeChanged(textura: SurfaceTexture, ancho: Int, alto: Int) = Unit

                            // Al salir de la pantalla o mandar la app al fondo la superficie
                            // se destruye: el reproductor se suelta y se arma de nuevo al volver.
                            override fun onSurfaceTextureDestroyed(textura: SurfaceTexture): Boolean {
                                reproductor.soltar()
                                visible = false
                                return true
                            }

                            override fun onSurfaceTextureUpdated(textura: SurfaceTexture) = Unit
                        }
                    }
                },
            )
            if (pausado) {
                Box(
                    Modifier
                        .size(64.dp)
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = SenarBlanco,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
        }
    }
}

/** Los videos de las señas son verticales (480×724); se usa hasta conocer el tamaño real. */
private const val PROPORCION_INICIAL = 480f / 724f

/** Ancho sobre alto del video, o null si el reproductor todavía no informó un tamaño válido. */
internal fun proporcionDeVideo(ancho: Int, alto: Int): Float? =
    if (ancho > 0 && alto > 0) ancho.toFloat() / alto else null

/**
 * Dónde se busca el video de una seña: `dictionary/<id>.mp4`. [base] ya viene con guion
 * bajo en lugar de espacios, igual que en [rutasCandidatasDeImagen].
 */
internal fun rutaDeVideo(base: String): String = "dictionary/$base.mp4"

/** El MediaPlayer de un video y su superficie: se abren y se sueltan juntos. */
private class ReproductorDeSena {
    private var player: MediaPlayer? = null
    private var superficie: Surface? = null

    fun abrir(
        abrirAsset: () -> android.content.res.AssetFileDescriptor,
        superficie: Surface,
        onTamano: (Int, Int) -> Unit,
        onPrimerCuadro: () -> Unit,
        onFalla: () -> Unit,
    ) {
        soltar()
        this.superficie = superficie
        try {
            val nuevo = MediaPlayer()
            player = nuevo
            abrirAsset().use { nuevo.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
            nuevo.setSurface(superficie)
            nuevo.isLooping = true
            nuevo.setVolume(0f, 0f)
            nuevo.setOnVideoSizeChangedListener { _, w, h -> onTamano(w, h) }
            nuevo.setOnInfoListener { _, que, _ ->
                if (que == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) onPrimerCuadro()
                false
            }
            nuevo.setOnPreparedListener { it.start() }
            nuevo.setOnErrorListener { _, _, _ ->
                soltar()
                onFalla()
                true
            }
            nuevo.prepareAsync()
        } catch (e: Exception) {
            // El asset no existe, está comprimido o el códec no lo abre: la pantalla muestra la imagen.
            Log.w("Dictionary", "No se pudo abrir el video", e)
            soltar()
            onFalla()
        }
    }

    /** Pausa o reanuda. Devuelve true si quedó en pausa. */
    fun alternarPausa(): Boolean {
        val actual = player ?: return false
        return try {
            if (actual.isPlaying) {
                actual.pause()
                true
            } else {
                actual.start()
                false
            }
        } catch (e: IllegalStateException) {
            // Todavía se está preparando: el toque no hace nada.
            Log.w("Dictionary", "El video todavía no está listo", e)
            false
        }
    }

    fun soltar() {
        player?.release()
        player = null
        superficie?.release()
        superficie = null
    }
}
