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
| Informe LaTeX | `informe-tesina-latex/` (versionado acá con su historial; tiene su propio `CLAUDE.md` y `NOTAS_DE_TRABAJO.md`) |
| Propuesta (la referencia de la tesis) | `docs/Propuesta de Proyecto de Tesina-Aragon Evelin-2.pdf` |

## Pipeline de la seña
```
CameraX (ImageAnalysis, RGBA) → FrameAnalyzer: RgbaABitmap copia y rota el frame (YuvToRgbConverter solo si la cámara no da RGBA)
  → GestureEngine.process → HandTracker (MediaPipe HandLandmarker, IMAGE, hand_landmarker.task; GPU con vuelta a CPU)
  → captura de 10/15/20 frames crudos según los FPS (solo frames con mano)
  → ContratoV2.secuenciaDesdeFrames (lh[63] ‖ rh[63], lienzo cuadrado, remuestreo a 15): la misma cuenta que el entrenamiento
  → TFLiteClassifier (actions_15_opt.tflite sin ops Flex, 15×126 → C) + LabelProvider (words.json)
  → top-3 → UI (CameraScreen) + SpeechManager (TTS)
```
- Máquina de estados en `ml/MaquinaDeCaptura.kt` (pura, con tests; `GestureEngine` hace la parte de Android): IDLE → WAITING (500 ms con mano) → CAPTURING (solo frames con mano; si se pierde la mano, vuelve a IDLE) → DONE.
- Lateralidad: con `isFrontCamera=false`, la etiqueta "Left" de HandLandmarker va a `lh`. El frame llega sin espejar, así que vale también con la cámara frontal. Medido en la PC y en la tablet (2026-10-05/07, `contract/features_v2.md`, regla 3): el slot `lh` es la mano izquierda del sujeto.
- Voz a texto: `voice/VoiceToText.kt` (`EXTRA_PREFER_OFFLINE`) + `voice/LimpiezaDeTexto.kt`.
- Federado por red local (experimental, solo a pedido desde su pantalla): `federado/`, contrato `contract/federado_v1.json`; el servidor es `modelo_LSA/federated/servidor.py`.
- Cada paquete tiene su `README.md` con el *por qué* de las decisiones (la cámara, en el de `ui/`).

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
- El volumen ya no se sube al arrancar: `SpeechManager` lo lleva al máximo la primera vez que habla en cada pantalla, solo si está por debajo de la mitad (`speech/VolumenDeVoz.kt`, TL-A6).

## Reglas que no se negocian
- **Assets del modelo:** `app/src/main/assets/{actions_15_opt.tflite, words.json, calibracion.json, modelo.json}` son el contrato con el modelo y **solo los escribe** `modelo_LSA/scripts/publicar_modelo.py` (`--verificar` revisa lo publicado). El orden de `words.json` *es* el índice de salida del modelo. La app valida `modelo.json` al cargar y, si no coincide, la cámara muestra un aviso en vez de traducir. Cada publicación se anota en `plan/progreso.md`.
- **Dataset del teléfono:** vive en `Android/data/com.example.traductorlsa/files/Documents/` (`lsa_samples.json` y `lsa_samples_v2.jsonl`, cada uno con su `.bak`). Desinstalar la app o cambiar el `applicationId` lo borra o lo deja huérfano: **exportarlo antes** (botón Exportar o `adb pull`).
- **Nombres de clases:** minúsculas, sin tildes, con espacio (`como estas`, `mi nombre es`, `por favor`). Tienen que coincidir con `words.json`, con `NOMBRES_PARA_MOSTRAR` en `ui/screens/DictionaryScreen.kt` y con `assets/dictionary/<id con _>.jpg`. Detalle en `modelo_LSA/ejecucion.txt`.
- **Git:** commits directo a `master`, sin ramas. **Sin `Co-Authored-By`:** el historial va a nombre de Evelin. Sin push salvo que lo pida. Mensajes en español, con el *por qué* en el cuerpo.
- **Estilo:** comentarios que explican el *por qué*, nombres en español donde el código ya los usa, funciones puras y testeables (el modelo a seguir es `voice/LimpiezaDeTexto.kt` + `LimpiezaDeTextoTest.kt`). Sin `!!`.
- La cámara está partida (T-A20): `ui/CameraScreen.kt` es solo la interfaz, el estado y la lógica van en `ui/camera/CameraViewModel.kt` (con tests en la JVM vía `AlmacenDeTomas`) y los recursos nativos en `ui/camera/PipelineDeCamara.kt`. Lógica nueva de la cámara va al ViewModel, con su test.

## Contrato de features
El contrato v2, compartido con modelo_LSA, está en `contract/` (`features_v2.md` y `features_v2.json`, más los fixtures de paridad). Se regenera en modelo_LSA y se copia entero acá: `ContratoEspejoTest` verifica cada copia contra su `SHA256SUMS` y `python tools/chequeo_contrato_espejo.py` compara los dos repos.
