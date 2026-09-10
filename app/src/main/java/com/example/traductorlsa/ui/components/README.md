# Directorio `ui/components/`

Este paquete contiene los **componentes reutilizables** de la interfaz: el andamio que comparten todas las pantallas internas, la barra fija, las tarjetas y los controles de Ajustes.

Existe para que las pantallas no repitan estructura. Si un radio, un espaciado o un color hay que cambiarlo, se cambia acá y no en diez lugares.

---

## **Archivos**

### **1. `Comunes.kt`**
El andamio compartido.

- **`PantallaSenar(onAvatar, desplazable, content)`** — fondo, barra fija y respeto de las barras del sistema. Todo lo que va adentro queda debajo del logotipo y el avatar. El parámetro `desplazable` permite apagar el scroll en pantallas que manejan el suyo, como la de voz.
- **`TituloDePagina(texto, onVolver)`** — botón de volver y título, en su propia línea debajo de la barra fija.
- **`Aire(alto)`** — separación vertical, para no repetir `Spacer(Modifier.height(...))` en todos lados.
- **`ChipDeFiltro(texto, seleccionado, onClick)`** — seleccionado en grafito, el resto en blanco con borde. Lo usan el dataset y el selector de señas del entrenamiento, que filtran la misma lista con los mismos criterios.

### **2. `SenarBarra.kt`**
- **`SenarBarra(onAvatar)`** — la barra fija: logotipo a la izquierda, avatar a la derecha. Es idéntica en todas las pantallas. El avatar abre Ajustes, que es donde vive la cuenta: por eso no hay además un engranaje.
- **`AvatarSenar(...)`** — con sesión muestra las iniciales sobre el azul de marca; sin sesión, un genérico gris. Todavía no muestra la foto de Clerk (`user.imageUrl`): cargarla necesitaría una librería de imágenes que el proyecto no tiene.
- **`inicialesDe(nombre, apellido)`** — **es el único punto del proyecto que lee campos del usuario de Clerk.** Si algún día el SDK los renombra, se arregla acá y en ningún otro lado.

### **3. `Tarjetas.kt`**
- **`TarjetaDeModo(...)`** — la tarjeta grande de un modo de traducción. El color no es decorativo: **azul para el lado que seña, grafito para el que habla**. Es la misma regla que sostiene todo el sistema.
- **`FilaDestino(...)`** — fila de un destino secundario: ícono, título, bajada y chevron.

### **4. `ControlesAjustes.kt`**
Los controles de la pantalla de Ajustes, construidos a medida para respetar el sistema de diseño.

- **`SeccionAjustes(...)`** — rótulo de sección más la tarjeta blanca que agrupa sus controles.
- **`CeldaAjuste(...)`** — una fila dentro de una sección. La última no lleva divisor.
- **`EtiquetaAjuste(...)`** — título de un control, con su valor actual a la derecha.
- **`AyudaAjuste(texto)`** — texto de ayuda: para qué sirve el control y cuándo moverlo.
- **`FilaInterruptor(...)`** y **`DeslizadorAjuste(...)`**.
- **`SelectorSegmentado(...)`** — selector de opciones excluyentes, hecho a mano en lugar de usar el de Material para que siga el radio y los colores del sistema.

---

## **Relación con otros módulos**
- Toma color, tipografía y formas de `ui/theme/`.
- Toma el isotipo y el logotipo de `ui/brand/`.
- Lo consumen todas las pantallas de `ui/screens/` y la propia `ui/CameraScreen.kt`.
