# SeÑAR: traductor de Lengua de Señas Argentina (app Android)

App Android en Kotlin y Jetpack Compose que traduce señas de la Lengua de Señas Argentina
(LSA) a texto y voz, y voz a texto. **Todo el procesamiento ocurre en el teléfono, sin
internet.** Es el prototipo de la tesina de grado de Evelin Aragón (Licenciatura en
Sistemas, UNGS).

Reconoce 12 señas aisladas (`hola`, `adios`, `bien`, `como estas`, `gracias`,
`mi nombre es`, `por favor` y las letras `e`, `v`, `l`, `i`, `n`), más la clase `nada`
para cuando no hay una seña. No es un traductor completo de LSA.

El modelo se entrena en otro repositorio (`modelo_LSA`); el informe de la tesina está en
`informe-tesina-latex/`.

---

## **Qué hace**

- **Traducir señas:** la cámara detecta las manos, clasifica la seña y la muestra como
  texto y la dice en voz alta. Si la confianza es baja, lo avisa en vez de arriesgar.
- **Traducir voz:** pasa a texto lo que dice la persona oyente, con el reconocedor del
  sistema en modo sin conexión.
- **Diccionario:** las señas que el modelo conoce, con su ilustración.
- **Entrenar:** graba tomas etiquetadas (libres o en una sesión guiada por planilla) y
  las guarda en el teléfono para exportarlas.
- **Aprendizaje federado (experimental):** entrena la última capa del modelo en el
  teléfono y comparte solo los pesos con un servidor de la red local.

Traducir y consultar el diccionario no necesitan cuenta ni red. La cuenta (Clerk) se
inicializa solo si la persona elige usarla, y hace falta para entrenar.

---

## **Pipeline de la seña**

```
CameraX (ImageAnalysis, RGBA)
   → FrameAnalyzer: RgbaABitmap copia y rota el frame (YuvToRgbConverter si la cámara no da RGBA)
   → GestureEngine.process
   → HandTracker: MediaPipe HandLandmarker (hand_landmarker.task), en GPU con vuelta a CPU
   → MaquinaDeCaptura: 10, 15 o 20 frames con mano, según los FPS
   → ContratoV2.secuenciaDesdeFrames: lh[63] ‖ rh[63], lienzo cuadrado, remuestreo a 15
   → TFLiteClassifier: actions_15_opt.tflite (15 × 126 → 13 clases, sin ops Flex)
   → top-3 → pantalla (CameraScreen) y voz (SpeechManager)
```

- **Máquina de estados** (`ml/MaquinaDeCaptura.kt`): espera a ver la mano 500 ms, captura
  solo frames con mano y, si la mano se pierde, descarta la captura.
- **Contrato de features** (`contract/`): la cuenta que convierte landmarks en la entrada
  del modelo es la misma en la app y en el entrenamiento. La carpeta está espejada en los
  dos repositorios y un fixture verifica que Kotlin y Python den el mismo resultado.
- **Modelo publicado** (`app/src/main/assets/`): `actions_15_opt.tflite`, `words.json`,
  `calibracion.json` y `modelo.json`. Los escribe únicamente
  `modelo_LSA/scripts/publicar_modelo.py`; la app valida `modelo.json` al cargar y, si
  no coincide, muestra un aviso en vez de traducir.

---

## **Estructura de paquetes**
```
com.example.traductorlsa
├── camera/       # CameraX y conversión del frame a Bitmap
├── detection/    # MediaPipe HandLandmarker
├── features/     # Contrato v2: de landmarks a la secuencia 15 × 126
├── ml/           # Motor de captura, clasificador TFLite, validación del modelo, métricas
├── model/        # Tipos simples (NormPoint, PredictionResult)
├── dataset/      # Tomas grabadas en el teléfono (v1 y v2), sesión guiada, seudónimo
├── federado/     # Entrenamiento local de la cabeza y cliente del servidor de la red local
├── speech/       # Texto a voz
├── voice/        # Voz a texto y limpieza del texto reconocido
├── settings/     # Ajustes de la app
├── startup/      # Precarga de assets durante el splash
├── ui/
│   ├── brand/       # Isotipo, logotipo e ilustraciones dibujados en Compose
│   ├── camera/      # Estado (CameraViewModel), recursos (PipelineDeCamara) y piezas de la cámara
│   ├── components/  # Componentes reutilizables
│   ├── overlay/     # Dibujo de los landmarks sobre la cámara
│   ├── screens/     # Las pantallas, una por destino de navegación
│   └── theme/       # Color, tipografía y formas
└── MainActivity.kt
```

Casi todos los paquetes tienen su `README.md` con el porqué de las decisiones.

---

## **Sistema de diseño**

Las pantallas siguen un sistema propio, definido en `ui/theme/`.

**La regla base sale del logo:** una burbuja azul con una mano (seña) y una
burbuja grafito con una onda (voz). De ahí en más, **el azul es siempre el lado
que seña y el grafito el que habla**, y las esquinas redondeadas de esas
burbujas definen los radios de toda la app (28 tarjetas, 18 botones, 13 cajas
de ícono).

- **Color** (`Color.kt`): azul de acción `#3B6AE8` (4,8:1 con blanco), azul de
  marca `#5D8EF9` solo para formas grandes e ilustración, grafito para texto y
  para el lado de la voz, ámbar `#FFBE1B` como acento puntual.
- **Tipografía** (`Type.kt`): Bricolage Grotesque para títulos y Onest para
  cuerpo, ambas empaquetadas en `res/font/` — no se descargan, así la app
  funciona sin red.
- **Tema** (`Theme.kt`): el color dinámico de Material You quedó deshabilitado
  a propósito. Antes la app tomaba el fondo de pantalla del teléfono y se veía
  distinta en cada dispositivo. Todavía no hay esquema oscuro diseñado.
- **Marca** (`ui/brand/`): el isotipo está dibujado con Compose Canvas sobre un
  lienzo de 200 × 156 unidades, así escala a cualquier tamaño y se recolorea
  según el fondo.

Los mockups de las pantallas viven en `docs/design/`.

**Accesibilidad, no negociable:** toque mínimo 48 dp, contraste 4,5:1 en texto,
el color nunca como único indicador, y el contenido de lectura desplazable para
que resista la fuente del sistema al 200 %.

---

## **Compilar y probar**

Requisitos: JDK 17, Android SDK (compileSdk 36) y un dispositivo con Android 7.0 o
superior (API 24) y cámara.

```bash
./gradlew.bat assembleDebug        # APK en app/build/outputs/apk/debug/
./gradlew.bat testDebugUnitTest    # tests en la JVM
./gradlew.bat lintDebug
python tools/chequeo_imports.py && python tools/chequeo_orden.py && python tools/chequeo_compose.py
```

Los tests instrumentados se corren con `installDebug installDebugAndroidTest` y
`adb shell am instrument`, **no** con `connectedAndroidTest`: ese comando desinstala la
app al terminar y borra las tomas grabadas en el teléfono. El detalle está en `CLAUDE.md`.

---

## **Rendimiento medido**

Tiempo por cuadro (mediana) con la versión final. La detección de manos domina el tiempo;
la inferencia del modelo no es el cuello de botella.

| Dispositivo | Preparación (ms) | MediaPipe (ms) | Cuadros por segundo |
|---|---|---|---|
| Lenovo TB321FU | 2,4 | 35 | 24,9 |
| OnePlus CPH2663 | 2,7 | 51 | 17,5 |
| Galaxy Tab S7 | 24,5 | 53 | 12,1 |
| Galaxy A55 | 5,4 | 93 | 9,6 |
| Galaxy A31 | 5,3 | 92 | 9,6 |
| Galaxy A20 | 13,9 | 130 | 6,6 |

Las cifras, el método y la comparación antes y después de las optimizaciones están en el
capítulo de resultados del informe. Las métricas de cada captura quedan en el teléfono
(`files/metricas/senar_metrics.jsonl`) y se resumen con `tools/logcat_metrics.py` y
`tools/linea_base.py`.

El acierto del modelo (67,3 % con una persona que no participó del entrenamiento) y cómo
se midió están en el README de `modelo_LSA` y en el informe.

---

## **Tecnologías**
- Kotlin y Jetpack Compose
- CameraX
- MediaPipe Tasks Vision (HandLandmarker)
- TensorFlow Lite
- Text-to-Speech y reconocimiento de voz de Android
