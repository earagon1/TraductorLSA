
package com.example.traductorlsa.model

data class PredictionResult(
    val gesture: String,
    val confidence: Float,
    val handedness: String
)
