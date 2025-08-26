
package com.example.traductorlsa.ui.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.traductorlsa.ui.OverlayData

@Composable
fun HandLandmarksOverlay(overlay: OverlayData) {
    if (overlay.imgW == 0 || overlay.imgH == 0 || overlay.hands.isEmpty()) return

    Canvas(modifier = Modifier.fillMaxSize()) {
        val viewW = size.width
        val viewH = size.height

        val swap = (overlay.rotationDeg % 180 != 0)
        val effW = if (swap) overlay.imgH.toFloat() else overlay.imgW.toFloat()
        val effH = if (swap) overlay.imgW.toFloat() else overlay.imgH.toFloat()

        val scale = kotlin.math.max(viewW / effW, viewH / effH)
        val scaledW = effW * scale
        val scaledH = effH * scale
        val dx = (viewW - scaledW) / 2f
        val dy = (viewH - scaledH) / 2f

        fun rot(x: Float, y: Float, deg: Int): Pair<Float, Float> = when (((deg % 360) + 360) % 360) {
            0 -> x to y
            90 -> (1f - y) to x
            180 -> (1f - x) to (1f - y)
            270 -> y to (1f - x)
            else -> x to y
        }

        val pointRadius = 3.dp.toPx()
        val lineWidth = 2.dp.toPx()

        val handConnections = listOf(
            0 to 1, 1 to 2, 2 to 3, 3 to 4,
            0 to 5, 5 to 6, 6 to 7, 7 to 8,
            0 to 9, 9 to 10, 10 to 11, 11 to 12,
            0 to 13, 13 to 14, 14 to 15, 15 to 16,
            0 to 17, 17 to 18, 18 to 19, 19 to 20,
            5 to 9, 9 to 13, 13 to 17
        )

        overlay.hands.forEachIndexed { idx, hand ->
            val color = when (idx) { 0 -> Color.Cyan; 1 -> Color.Magenta; else -> Color.Yellow }
            val pts = hand.map { p ->
                val (rx, ry) = rot(p.x, p.y, overlay.rotationDeg)
                val x = dx + (rx * effW) * scale
                val y = dy + (ry * effH) * scale
                Offset(x, y)
            }

            handConnections.forEach { (a, b) ->
                if (a < pts.size && b < pts.size) {
                    drawLine(color.copy(alpha = 0.6f), pts[a], pts[b], strokeWidth = lineWidth)
                }
            }

            pts.forEachIndexed { i, o ->
                val pc = when (i) { 0 -> color; 4,8,12,16,20 -> Color.White; else -> color }
                drawCircle(Color.Black, radius = pointRadius + 1.dp.toPx(), center = o)
                drawCircle(pc, radius = pointRadius, center = o)
            }
        }
    }
}
