# Directorio `ui/screens/`

Este paquete contiene las **pantallas completas** de la app. Cada archivo es un destino del `NavHost` definido en `ui/AppNavigation.kt`.

La pantalla de cámara **no** está acá: vive en `ui/CameraScreen.kt`, porque además de dibujar maneja el ciclo de vida de CameraX y el pipeline de inferencia.

---

## **Flujo de entrada**

### **1. `SplashScreen.kt`**
Presentación mientras `startup/AssetWarmup` precarga los assets pesados.

- `TIEMPO_MINIMO_MS` — cuánto se queda en pantalla como mínimo, contando desde que arranca la animación de entrada (que termina a los 780 ms). Es el número a tocar si se quiere que dure más o menos.
- `TIEMPO_MAXIMO_MS` — techo de espera: si la precarga se hace larga, se entra igual.
- `TOPE_BARRA` — hasta dónde sube la barra sola. El tramo que queda **no se completa hasta que los assets están realmente leídos**, así nunca dice "listo" antes de tiempo.

### **2. `OnboardingScreen.kt`**
Los pasos de bienvenida, con ilustraciones dibujadas en Compose (`ui/brand/`): qué son las señas, los dos modos de traducción, y la privacidad del procesamiento local. `ALTO_HERO` es fijo a propósito: es un gráfico, no texto, y no debe escalar con la fuente del sistema.

### **3. `AuthEntryScreen.kt`**
Pantalla de acceso, con el fondo de marca. Ofrece entrar con cuenta o seguir sin ella.

---

## **Pantallas principales**

### **4. `HomeScreen.kt`**
El menú: las dos tarjetas de modo (señas y voz) y los destinos secundarios (entrenamiento, diccionario, ajustes).

### **5. `TranslateVoiceScreen.kt`**
El modo **voz a texto**. Usa `voice/VoiceToText`.

- Separa el texto **confirmado** del **provisorio**: lo provisorio se muestra en gris hasta que el reconocedor lo confirma.
- Indicador de escucha, botón de limpiar y pedido de permiso de micrófono en el momento en que hace falta.

### **6. `DictionaryScreen.kt`**
El catálogo de señas, con búsqueda, categorías y el dibujo de cada una.

- **`categorizeWord`** agrupa en categorías (`WordCategory`).
- **`loadWordsFromAssets`** lee `words.json`, que guarda los ids con los que entrena el modelo: en minúsculas, sin tildes ni signos, y con espacio donde la frase lo lleva (`adios`, `como estas`, `por favor`). Esos ids **no pueden llegar tal cual a la pantalla**, así que se traducen a un nombre legible en `NOMBRES_PARA_MOSTRAR`. Al agregar una seña nueva hay que sumarla también a esa traducción, o se muestra el id crudo.
- **`findWordImageInAssets`** busca el dibujo en `assets/dictionary/`, normalizando el nombre a minúsculas, sin tildes y con `_` en lugar de espacios.

---

## **Modo entrenamiento**

### **7. `TrainingHomeScreen.kt`**
Las dos entradas del modo: grabar muestras nuevas y ver el dataset.

### **8. `DatasetScreen.kt`**
El dataset como **lista de tareas**, no como reporte.

- **`CoberturaDelModelo`** — una barra partida en tres dice de un vistazo lo que un total suelto no dice: cuántas señas están listas, cuántas a medias y cuántas sin empezar.
- Cada fila muestra el dibujo del diccionario, el nombre y el progreso hacia la meta (`MUESTRAS_OBJETIVO`); tocarla abre la cámara ya entrenando esa seña.
- Filtros por lo que falta, todas o listas.
- **`leerSenasDelDataset`** junta el catálogo con lo guardado, e incluye también las etiquetas que solo existen en el dataset: son las señas propias agregadas desde el entrenamiento, que no están en `words.json`.
- El botón de exportar solo se enciende cuando hay muestras.

---

## **Secundarias**

### **9. `SettingsScreen.kt`**
Edita todo lo de `settings/AjustesSenar`. Incluye un chip para **probar la voz** con la velocidad y el tono elegidos. `conComa` formatea siempre igual, sin depender del idioma del dispositivo.

### **10. `AboutScreen.kt`**
Información del proyecto y las tecnologías usadas.

---

## **Relación con otros módulos**
- `ui/AppNavigation.kt` las registra como destinos.
- Todas se apoyan en `ui/components/` para el andamio y en `ui/theme/` para el estilo.
- Las que tocan preferencias leen `settings/AjustesSenar`.
