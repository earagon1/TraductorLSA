# Directorio `model/`

Este paquete contiene **modelos de datos básicos** que representan las estructuras utilizadas en todo el proyecto para intercambio de información entre componentes.

---

## **Archivos**

### **1. `NormPoint.kt`**
Modelo de datos para un **punto normalizado** en la pantalla o en el frame de cámara.

**Propiedades:**
- `x: Float` → Coordenada X normalizada (0.0 a 1.0).
- `y: Float` → Coordenada Y normalizada (0.0 a 1.0).

**Uso principal:**
- Dibujo de landmarks en la interfaz (overlay).
- Referencia de puntos de las manos capturados por MediaPipe.

---

### **2. `PredictionResult.kt`**
Modelo de datos para representar el resultado de una **predicción del modelo TFLite**.

**Propiedades:**
- `gesture: String` → Nombre de la seña detectada.
- `confidence: Float` → Nivel de confianza (0.0 a 1.0) de la predicción.
- `handedness: String` → Mano detectada (`Left`, `Right`, `Both` o `Unknown`).

**Uso principal:**
- Mostrar resultados en la UI.
- Convertir el resultado en texto y voz mediante `SpeechManager`.
- Registrar métricas o depurar el desempeño de las predicciones.

---

## **Relación con otros paquetes**
- **UI (`CameraScreen`)**: usa `PredictionResult` para mostrar el texto traducido y dibujar la predicción.
- **GestureEngine (`ml/`)**: genera y emite objetos `PredictionResult` después de cada inferencia.
- **FeatureBuilder (`features/`)**: usa `NormPoint` para generar coordenadas de overlay en pantalla.
