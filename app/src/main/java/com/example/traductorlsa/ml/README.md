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
```

---

### **3. `TFLiteClassifier.kt`**
Ejecuta la inferencia con **TensorFlow Lite** y devuelve las probabilidades de cada clase.

#### **Elección del modelo**
Recorre los assets en orden de preferencia y carga el primero que encuentra:

1. `actions_15_opt.tflite` — cuantización dinámica. **Es el que se usa.**
2. `actions_15_f32.tflite` — float32, de respaldo.
3. `modelo.tflite` / `model.tflite` — nombres heredados.

El cuantizado va primero a propósito. Antes ganaba el float32 y el optimizado viajaba en el APK sin que se lo usara nunca, así que la cuantización que compromete la propuesta no llegaba al dispositivo. Medido con `benchmark_cuantizacion.py` (repo `Modelo_LSA`): **182 KB contra 648**, con la misma accuracy y sin cambiar una sola predicción, a cambio de unos 0,4 ms más por inferencia.

> Si cambiás este orden, actualizá también `startup/AssetWarmup.kt`, que precarga los mismos archivos.

#### **Forma de la entrada**
`T` y `D` se leen del tensor de entrada del modelo (15 × 126), no están escritos a mano: si el modelo cambia de longitud de secuencia, no hay que tocar código.

#### **Funciones**
- **`inferTop(seqT, labels)`** → `Triple(índice, probabilidad, vector completo)`
  - Arma un `ByteBuffer` directo de `4 × T × D` en orden nativo.
  - Rellena con ceros los frames que falten.
  - Si la salida no suma ≈ 1, aplica un softmax estabilizado (resta el máximo antes de exponenciar). Es una red de seguridad por si el modelo se exportara sin la capa softmax.
- **`infer(seqT, labels)`** → `Pair(índice, probabilidad)`. Atajo sobre `inferTop`.
- **`close()`** libera el intérprete.

---

## **Relación con otros módulos**
- Recibe las secuencias normalizadas de `SequenceBuffer` (en `features/`).
- `GestureEngine` es quien lo invoca; nadie más habla con el intérprete directamente.
- Las etiquetas salen de `LabelProvider`, que lee el mismo `words.json` con el que se entrenó el modelo. **El orden importa**: los índices de salida de la red corresponden a las posiciones de ese arreglo.
