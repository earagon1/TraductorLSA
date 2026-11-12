package com.example.traductorlsa.ui.overlay

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.traductorlsa.model.NormPoint
import java.util.*

/*
 * Lo que dibuja el overlay de landmarks: tamaño de la imagen, manos y orientación.
 */

data class OverlayData(
    val imgW: Int = 0,
    val imgH: Int = 0,
    val hands: List<List<NormPoint>> = emptyList(),
    val rotationDeg: Int = 0,
    val isFront: Boolean = true
)
