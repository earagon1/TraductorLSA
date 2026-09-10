# Directorio `ui/theme/`

Este paquete define el **sistema de diseño** de la app: color, tipografía, formas y el tema que los aplica.

La regla base sale del logo: una burbuja azul con una mano y una burbuja grafito con una onda. De ahí en más, **el azul es siempre el lado que seña y el grafito el que habla**, y las esquinas de esas burbujas definen los radios de toda la interfaz.

---

## **Archivos**

### **1. `Color.kt`**
La paleta completa, como constantes sueltas y no como un `ColorScheme`, para poder nombrarlas por rol.

- **Azules** (`SenarAzul050` … `SenarAzul900`) — el lado de las señas. `SenarAzul600` (`#3B6AE8`) es el azul de acción, con 4,8:1 de contraste sobre blanco; `SenarAzul500` es el de marca, solo para formas grandes e ilustración.
- **Grafitos** (`SenarGrafito300` … `SenarGrafito900`) — texto y el lado de la voz.
- **Superficies** — `SenarPapel`, `SenarBlanco`, `SenarPapelHundido`, `SenarBorde`, `SenarBordeSuave`, `SenarPista`.
- **Acento** — `SenarAmbar` (`#FFBE1B`), de uso puntual.
- **Visor** — `SenarVisorAlto` y `SenarVisorBajo`, para el degradado sobre la cámara.

### **2. `Type.kt`**
- **Bricolage Grotesque** para títulos.
- **Onest** para cuerpo e interfaz, elegida por legibilidad a tamaños chicos.

Las dos van **empaquetadas en `res/font/`**, no se descargan: la app tiene que funcionar sin red.

### **3. `Shape.kt`**
Los radios del sistema, tomados de las burbujas del isotipo: **28 dp** para tarjetas, **18 dp** para botones y **13 dp** para cajas de ícono.

### **4. `Theme.kt`**
- **`SenarTheme`** — aplica color, tipografía y formas.
  - El **color dinámico de Material You está deshabilitado a propósito**: antes la app tomaba el fondo de pantalla del teléfono y se veía distinta en cada dispositivo.
  - Todavía no hay esquema oscuro diseñado, así que el claro se aplica también cuando el sistema está en modo oscuro. Es una tarea aparte.
- **`SenarSystemBars(iconosOscuros)`** — ajusta el color de los íconos de la barra de estado según el fondo de la pantalla actual. Las pantallas con fondo de marca necesitan íconos claros.

---

## **Accesibilidad**
No es negociable, y condiciona al resto del paquete:

- Toque mínimo de **48 dp**.
- Contraste **4,5:1** en texto.
- El color **nunca** como único indicador.
- Contenido de lectura desplazable, para que resista la fuente del sistema al 200 %.
