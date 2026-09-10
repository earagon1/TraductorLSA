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

#### **Piezas de la interfaz**
- **`GuiaDeEncuadre`** — cuatro esquinas y nada más. Sin esto la cámara puede estar apuntando al techo y la muestra se guarda igual, con lo cual entra ruido al dataset. Van solo las esquinas para no tapar a la persona.
- **`ReferenciaFija`** — miniatura del dibujo de la seña, fija en pantalla. Antes era un botón que abría un modal, y un modal no se puede mirar mientras se seña, que es justo cuando hace falta.
- **`VeloInferior`** — degradado en lugar de una tarjeta: es el recurso de los subtítulos, deja el texto legible sobre cualquier video sin recortarle un tercio de pantalla al visor.
- **`PieDeTraduccion`** — la frase se lee como subtítulo: azul-300 para lo ya dicho, que es el color de quien seña en todo el sistema, y la última palabra en blanco y negrita.
- **`PieDeCaptura`** — los estados del entrenamiento, incluidos los que `GestureEngine` ya tenía y no se mostraban.
- **`InsigniaDeProgreso`**, **`PildoraDeVoz`**, **`RotuloDeEstado`**, **`TrainingWordPickerSheet`**.

#### **Dataset local**
El archivo `lsa_samples.json` se maneja desde acá:

- **`appendSamplesByDate(context, samples, T, D, clientId)`** — guarda muestras agrupadas por fecha. Cada muestra lleva **`client_id`**, el id de la cuenta que la grabó: sin ese campo no hay forma de saber de quién es cada gesto, y el dataset no sirve ni para el experimento de aprendizaje federado ni para medir generalización interusuario. Si no hay sesión se omite la clave, así las muestras sin atribución quedan identificables.
- **`clientIdActual()`** — lee el id del `StateFlow` de Clerk **en el momento de guardar**, no por composición, para que la lambda de captura no se quede con un valor viejo.
- **`removeLastDatasetSample`** — deshacer la última muestra.
- **`shareDatasetJsonFile`** — exporta el JSON por el selector del sistema, vía `FileProvider`.
- **`loadDatasetCountsByLabel`** — cuántas muestras hay por seña.
- **`MUESTRAS_OBJETIVO`** — meta por seña. Es un valor de trabajo, no una medida: el número real sale del script de entrenamiento en Python.

> Un export se puede verificar con `federated/verificar_export.py`, en el repo `Modelo_LSA`.

---

## **Relación con otros módulos**
- `CameraScreen` arma y consume el pipeline entero: `camera/`, `detection/`, `features/`, `ml/` y `speech/`.
- Los ajustes de calidad, cámara, sensibilidad, overlay y confianza mínima salen de `settings/AjustesSenar`.
- `MainActivity.kt` es quien llama a `LsaTranslatorApp()`.
