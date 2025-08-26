# Directorio `ml/`

Este paquete contiene el núcleo del motor de **detección de gestos y clasificación**.  
Aquí se procesan las secuencias de landmarks, se ejecuta el modelo TensorFlow Lite y se obtiene la predicción final.

---

## **Archivos**

### **1. `GestureEngine.kt`**
El **orquestador del pipeline**.  
Coordina la detección de manos, el armado de secuencias, la inferencia y la devolución de resultados.

**Funciones principales:**
- **`process(bitmap, rotationDeg, ts)`**  
  - Convierte la imagen a `MPImage`.
  - Ejecuta la detección de manos.
  - Genera puntos de overlay para la UI.
  - Maneja el estado de captura (`IDLE`, `WAITING`, `CAPTURING`, `DONE`).
  - Llama a la inferencia cuando la secuencia está completa.
- **`autoPredict()`**  
  - Normaliza la secuencia capturada.
  - Llama al clasificador para obtener las probabilidades.
  - Devuelve el Top-1 y Top-3 de predicciones.
- **`forcePrediction()`**  
  - Forza una predicción con la secuencia disponible, útil para depuración.
- **`setCameraFacing()`**  
  - Cambia la orientación de la cámara y reinicia el buffer.
- **`release()`**  
  - Libera recursos y callbacks.

**Callbacks principales:**
- `onHands` → Overlay de landmarks en la UI.
- `onPrediction` → Predicción principal (Top-1).
- `onTopPredictions` → Top-3 predicciones + features.
- `onCaptureStats` → Métricas de captura e inferencia.
- `onCaptureProgress` → Progreso en tiempo real de la captura.

---

### **2. `LabelProvider.kt`**
Carga las **etiquetas del modelo** desde el archivo `words.json`.

- Lee el arreglo `word_ids` y extrae el nombre de cada seña.
- Si el archivo no existe, usa etiquetas por defecto:  
  `"hola", "adios", "bien", "como_estas", "gracias"`.

**Uso:**
```kotlin
val labelProvider = LabelProviderImpl(context)
val etiquetas = labelProvider.labels
