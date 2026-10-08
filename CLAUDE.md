# CLAUDE.md: TraductorLSA (app SeÑAR)

App Android (Kotlin + Jetpack Compose) que traduce Lengua de Señas Argentina (LSA)
a texto y voz, y voz a texto, **todo en el teléfono y sin internet**. Es la tesina de
grado de Evelin Aragón (UNGS). La premisa de la propuesta es el procesamiento local:
cualquier cambio que agregue red, nube, cuentas o telemetría remota necesita su OK.

## Dónde está todo
| Qué | Ruta |
|---|---|
| Esta app | `C:\Users\Evelin\Desktop\Tesis\TraductorLSA` (rama `master`) |
| Entrenamiento (Python) | `C:\Users\Evelin\Desktop\Tesis\LSA-training\modelo_LSA` (repo propio, **anidado** dentro de `LSA-training`; ver su `CLAUDE.md`) |
| Plan de trabajo y avance | `C:\Users\Evelin\Desktop\Tesis\plan\` (`PLAN_MAESTRO.md`, `progreso.md`). **Leer `progreso.md` al empezar** |
| Informe LaTeX | `informe-tesina-latex/` (sin versionar acá; tiene su propio `AGENTS.md` y `NOTAS_DE_TRABAJO.md`) |
| Propuesta (la referencia de la tesis) | `docs/Propuesta de Proyecto de Tesina-Aragon Evelin-2.pdf` |

## Pipeline de la seña
```
CameraX (ImageAnalysis) → YuvToRgbConverter (YUV→JPEG→Bitmap) → GestureEngine.process
  → rotación 90/270 a Bitmap "arriba" → HandTracker (MediaPipe HandLandmarker, IMAGE, hand_landmarker.task)
  → FeatureBuilder.toVector126 (lh[63] ‖ rh[63], corrección a lienzo cuadrado)
  → SequenceBuffer (captura de 10/15/20 frames según los FPS → normalizeTo 15)
  → TFLiteClassifier (actions_15_opt.tflite sin ops Flex, 15×126 → C) + LabelProvider (words.json)
  → top-3 → UI (CameraScreen) + SpeechManager (TTS)
```
- Máquina de estados de `ml/GestureEngine.kt`: IDLE → WAITING (500 ms con mano) → CAPTURING (solo frames con mano; si se pierde la mano, vuelve a IDLE) → DONE.
- Lateralidad: con `isFrontCamera=false`, la etiqueta "Left" de HandLandmarker va a `lh`. Medido en la PC: coincide con Holistic (el slot `lh` es la mano izquierda del sujeto). Falta confirmarlo en el dispositivo (ver `progreso.md`).
- Voz a texto: `voice/VoiceToText.kt` (`EXTRA_PREFER_OFFLINE`) + `voice/LimpiezaDeTexto.kt`.
- Cada paquete tiene su `README.md` con el *por qué* de las decisiones.

## Comandos (esta máquina: Windows, JDK 17 en `C:\Program Files\Java\jdk-17`)
```bash
./gradlew.bat assembleDebug            # APK en app/build/outputs/apk/debug/
./gradlew.bat testDebugUnitTest        # tests JVM
./gradlew.bat compileDebugKotlin -q    # chequeo rápido de compilación
python tools/chequeo_imports.py && python tools/chequeo_orden.py && python tools/chequeo_compose.py
```
- **Tests instrumentados sin perder el dataset:** `connectedAndroidTest` desinstala la app al terminar y borra el dataset del teléfono. Usar:
  `./gradlew.bat installDebug installDebugAndroidTest` y `adb shell am instrument -w -e class <Clase> com.example.traductorlsa.test/androidx.test.runner.AndroidJUnitRunner`.
- **Dispositivo por Wi-Fi:** la tablet de Evelin (Lenovo TB321FU) está vinculada con `adb pair`; si la depuración inalámbrica está activa, `adb devices` la encuentra sola por mDNS. En Git Bash usar `MSYS_NO_PATHCONV=1` para rutas del dispositivo (`/sdcard/...`).
- **Métricas del pipeline:** `adb shell run-as com.example.traductorlsa cat files/metricas/senar_metrics.jsonl` → `tools/logcat_metrics.py` / `tools/linea_base.py`.
- `adb` no está en el PATH: `"$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"`.
- Logs del pipeline en el dispositivo: `adb logcat -s GestureEngine GestureDebug TFLiteClassifier`.
- `MainActivity` sube el volumen al máximo en cada arranque (H-A23): ojo al probar.

## Reglas que no se negocian
- **Assets del modelo:** `app/src/main/assets/{actions_15_opt.tflite, words.json}` son el contrato con el modelo y **se actualizan juntos**. El orden de `words.json` *es* el índice de salida del modelo. Cuando exista `publicar_modelo.py` (en modelo_LSA), solo ese script los escribe. Mientras tanto, si se copian a mano, se copian los dos y se anota en `plan/progreso.md`.
- **Dataset del teléfono:** vive en `Android/data/com.example.traductorlsa/files/Documents/lsa_samples.json`. Desinstalar la app o cambiar el `applicationId` lo borra o lo deja huérfano: **exportarlo antes** (botón Exportar o `adb pull`).
- **Nombres de clases:** minúsculas, sin tildes, con espacio (`como estas`, `mi nombre es`, `por favor`). Tienen que coincidir con `words.json`, con `NOMBRES_PARA_MOSTRAR` en `ui/screens/DictionaryScreen.kt` y con `assets/dictionary/<id con _>.jpg`. Detalle en `modelo_LSA/ejecucion.txt`.
- **Git:** commits directo a `master`, sin ramas. **Sin `Co-Authored-By`:** el historial va a nombre de Evelin. Sin push salvo que lo pida. Mensajes en español, con el *por qué* en el cuerpo.
- **Estilo:** comentarios que explican el *por qué*, nombres en español donde el código ya los usa, funciones puras y testeables (el modelo a seguir es `voice/LimpiezaDeTexto.kt` + `LimpiezaDeTextoTest.kt`). Sin `!!`.
- `ui/CameraScreen.kt` (1644 líneas) se va a partir (tarea T-A20) **antes** de cualquier otro cambio grande en la cámara.

## Contrato de features
El contrato v2, compartido con modelo_LSA, va en `contract/` (espejado en los dos repos, con un test de hash). Hasta que exista, la referencia es la tabla "Desajustes" de `plan/modelo_lsa.md` §1.4, más los hallazgos medidos en `plan/progreso.md`.
