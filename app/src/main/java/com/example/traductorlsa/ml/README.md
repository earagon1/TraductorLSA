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
  - Le pasa cada frame a `MaquinaDeCaptura` y actúa según el evento que devuelve.
  - Llama a la inferencia cuando la captura está completa.
- **`autoPredict()`**  
  - Arma la secuencia con `ContratoV2` desde los landmarks crudos.
  - Llama al clasificador para obtener las probabilidades.
  - Devuelve el Top-1 y Top-3 de predicciones.
- **`setCameraFacing()`**  
  - Cambia la orientación de la cámara y descarta la captura en curso.

#### `MaquinaDeCaptura.kt`
La máquina de estados de la captura (`IDLE`, `WAITING`, `CAPTURING`, `DONE`), sin Android:
recibe el instante y si hay manos, y devuelve un evento (empezó, frame, completa, abortada,
listo para la siguiente). Salió de `GestureEngine` para poder probarla en la JVM
(`MaquinaDeCapturaTest`); también decide cuántos frames pide la próxima captura (10, 15 o
20, según los FPS de la anterior).

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
  `"hola", "adios", "bien", "como estas", "gracias"`.

**Uso:**
```kotlin
val labelProvider = LabelProviderImpl(context)
val etiquetas = labelProvider.labels
```

---

### **3. `TFLiteClassifier.kt`**
Ejecuta la inferencia con **TensorFlow Lite** y devuelve las probabilidades de cada clase.

#### **El modelo**
Carga un único archivo de assets, `actions_15_opt.tflite` (cuantización dinámica, sin ops Flex). Antes recorría una lista con un float32 de respaldo y nombres heredados; se sacó porque un respaldo silencioso permite traducir con un modelo que no es el publicado (auditoría, X3).

Al cargar calcula el sha256 del modelo y lo compara, junto con la forma de la entrada, la cantidad de salidas, las etiquetas y la calibración, con `modelo.json` (`ValidacionModelo.kt`). Lo que no coincide queda en `problemas`, y con algún problema la cámara muestra un aviso en vez de traducir. Los cuatro assets los escribe solo `modelo_LSA/scripts/publicar_modelo.py`.

> `startup/AssetWarmup.kt` precarga el mismo archivo: si cambia el nombre, cambia en los dos lados.

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
- Recibe la secuencia de 15 × 126 que arma `ContratoV2.secuenciaDesdeFrames` (en `features/`).
- `GestureEngine` es quien lo invoca; nadie más habla con el intérprete directamente.
- Las etiquetas salen de `LabelProvider`, que lee el mismo `words.json` con el que se entrenó el modelo. **El orden importa**: los índices de salida de la red corresponden a las posiciones de ese arreglo.

---

## **`MetricasCaptura.kt`: cuánto tarda cada etapa**

En los builds depurables, cada captura (completa o abortada) deja **una línea JSON**
con los tiempos de cada etapa por frame (`conv`, `rot`, `mp`, `feat`: media, p95 y
máximo), la inferencia, los FPS, el top-3 y en qué mitad del vector apareció cada mano.
- Se escribe en logcat con el tag `SENAR_METRICS` **y** en `files/metricas/senar_metrics.jsonl`. El archivo existe porque el buffer de logcat de algunos dispositivos es de 256 KB y pierde una sesión entera en minutos.
- Las etapas también se marcan con `android.os.Trace` (`senar:conv`, `senar:rot`, `senar:mp`, `senar:feat`, `senar:infer`) para Perfetto.
- En release no se mide ni se escribe nada (`Metricas.activas`, que fija `SenarApp`).
- Para resumir: `tools/logcat_metrics.py`. Para congelar una línea base: `tools/linea_base.py` → `docs/metricas/baseline.json`.

Reemplaza a los `Log.d` que `GestureEngine` escribía en **cada frame**, también en release.
