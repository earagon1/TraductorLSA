# Directorio `ui/`

Este paquete contiene la **interfaz completa**, escrita en Jetpack Compose. En la raíz viven la navegación y la pantalla de cámara; el resto está repartido en subpaquetes.

| Subdirectorio | Qué contiene |
|---|---|
| `brand/` | La identidad visual dibujada en código: isotipo, logotipo, ilustraciones |
| `components/` | Componentes reutilizables: andamio, barra fija, tarjetas, controles |
| `overlay/` | El dibujo de los landmarks sobre la cámara |
| `screens/` | Las pantallas completas, una por destino de navegación |
| `theme/` | El sistema de diseño: color, tipografía, formas |

---

## **Archivos**

### **1. `AppNavigation.kt`**
La navegación de la app.

- **`LsaTranslatorApp()`** — punto de entrada de la UI: aplica `SenarTheme` y monta el `NavHost`.
- **`AppDestination`** — clase sellada con todas las rutas: splash, onboarding, acceso, home, los dos modos de traducción, entrenamiento, dataset, diccionario, ajustes y acerca de.
  - `TrainingCapture` acepta un argumento opcional `sena`: entrar desde «Grabar nuevas muestras» navega a la ruta pelada y el selector se abre solo, mientras que tocar una fila del dataset abre la cámara ya entrenando esa seña.
- **`TrainingAccessGate`** — envoltorio que **exige sesión iniciada** para el modo entrenamiento y el dataset. Sin sesión muestra una explicación y el botón de iniciar sesión, aclarando que el diccionario sigue disponible sin conexión.
- **`AuthClerkScreen`** — la vista de autenticación de Clerk, con sus tres estados: inicializando, sin usuaria y con usuaria (navegando al home).

### **2. `CameraScreen.kt`**
La pantalla de cámara, que sirve a **dos modos** con el mismo pipeline:

- **`TranslateSignScreen`** — traducir señas a texto y voz.
- **`TrainingCaptureScreen`** — capturar muestras etiquetadas para ampliar el dataset.

`CameraScreenMode` es lo que las distingue; todo lo demás se comparte.

`CameraScreen.kt` tiene solo la interfaz y el cableado entre el motor y el estado (T-A20).
El estado y la lógica están en `camera/CameraViewModel.kt`, que usa el dataset a través de
`AlmacenDeTomas` para poder probarse en la JVM; los recursos nativos (CameraX, MediaPipe,
TFLite, voz) y su cierre, en `camera/PipelineDeCamara.kt`. El resto de las piezas:

| Archivo | Qué tiene |
|---|---|
| `camera/ControlesDeCamara.kt` | cabecera, insignia de progreso, interruptor de voz, velo, botón redondo, rótulo de estado, encuadre y referencia fija |
| `camera/PiesDeCamara.kt` | el pie de captura (entrenamiento) y el de traducción |
| `camera/SelectorDeSenas.kt` | la hoja para elegir qué seña entrenar |
| `camera/DialogoDeReferencia.kt` | la imagen de referencia de la seña, en un diálogo |
| `camera/SenasDeEntrenamiento.kt` | etiquetas, nombres e imágenes de las señas; `MUESTRAS_OBJETIVO`, `CONFIG_MEDIAPIPE` |
| `camera/DatasetDeEntrenamiento.kt` | seudónimo de la cuenta, compartir el dataset y conteo por seña |
| `camera/SesionGuiadaUi.kt`, `EstadoTraduccion.kt`, `AvisoModeloInvalido.kt` | la sesión guiada, el estado de la traducción y el aviso de modelo inválido |
| `overlay/OverlayData.kt` | lo que dibuja el overlay de landmarks |

#### **Piezas de la interfaz**
- **`GuiaDeEncuadre`** — cuatro esquinas y nada más. Sin esto la cámara puede estar apuntando al techo y la muestra se guarda igual, con lo cual entra ruido al dataset. Van solo las esquinas para no tapar a la persona.
- **`ReferenciaFija`** — miniatura del dibujo de la seña, fija en pantalla. Antes era un botón que abría un modal, y un modal no se puede mirar mientras se seña, que es justo cuando hace falta.
- **`VeloInferior`** — degradado en lugar de una tarjeta: es el recurso de los subtítulos, deja el texto legible sobre cualquier video sin recortarle un tercio de pantalla al visor.
- **`PieDeTraduccion`** — la frase se lee como subtítulo: azul-300 para lo ya dicho, que es el color de quien seña en todo el sistema, y la última palabra en blanco y negrita.
- **`PieDeCaptura`** — los estados del entrenamiento, incluidos los que `GestureEngine` ya tenía y no se mostraban.
- **`InsigniaDeProgreso`**, **`PildoraDeVoz`**, **`RotuloDeEstado`**, **`TrainingWordPickerSheet`**.

#### **Dataset local**
Los archivos del dataset se leen y se escriben en el paquete `dataset/` (`DatasetV1`, `DatasetV2` y `AlmacenSeguro`, que escribe de forma atómica y guarda un `.bak`). La cámara llega a ellos por `AlmacenDeTomas`, la interfaz que usa el `CameraViewModel`; la implementación real es `AlmacenDelTelefono`, en `camera/DatasetDeEntrenamiento.kt`. Ahí están también:

- **`participanteActual()`** — lee la cuenta del `StateFlow` de Clerk **en el momento de guardar**, no por composición, y devuelve solo su seudónimo: el id crudo de Clerk no llega al dataset.
- **`shareDatasetJsonFile`** — exporta el JSON por el selector del sistema, vía `FileProvider`. Antes de compartir, `DatasetV1.prepararParaCompartir` reemplaza los `client_id` viejos por su seudónimo.
- **`loadDatasetCountsByLabel`** — cuántas muestras hay por seña.

`MUESTRAS_OBJETIVO` (en `camera/SenasDeEntrenamiento.kt`) es la meta por seña: un valor de trabajo, no una medida.

> Un export se puede verificar con `federated/verificar_export.py`, en el repo `modelo_LSA`.

---

## **Relación con otros módulos**
- `CameraScreen` arma y consume el pipeline entero: `camera/`, `detection/`, `features/`, `ml/` y `speech/`.
- Los ajustes de calidad, cámara, sensibilidad, overlay y confianza mínima salen de `settings/AjustesSenar`.
- `MainActivity.kt` es quien llama a `LsaTranslatorApp()`.
