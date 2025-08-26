
package com.example.traductorlsa.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.traductorlsa.model.PredictionResult

@Composable
fun PredictionCard(currentPrediction: PredictionResult?) {
    if (currentPrediction == null) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Seña detectada", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
            Text(
                currentPrediction.gesture.uppercase(),
                color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold
            )
            Row(Modifier.padding(top = 8.dp)) {
                Text(
                    text = "Confianza: ${(currentPrediction.confidence * 100).toInt()}%",
                    color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = "Mano: ${currentPrediction.handedness}",
                    color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .padding(top = 8.dp)
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
            )
        }
    }
}
