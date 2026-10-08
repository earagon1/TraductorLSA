# Protocolo de la sesión de captura multipersona (Etapa 3)

> Borrador para revisión de la directora. Versión 0.1, 2026-10-08.
> Lo que todavía falta decidir está marcado con **[DECIDIR]**.

## 1. Para qué es esta sesión

Hasta hoy, todas las muestras con las que se entrenó y se validó el modelo son de una
sola persona (la autora), grabadas con la webcam de la PC, y casi idénticas entre sí.
Con eso el 95 % de accuracy mide "reconocer las tomas de la autora en la PC", no
"reconocer señas de personas en el teléfono". Esta sesión produce, de una sola vez, los
cinco insumos que faltan:

1. **Datos de entrenamiento del dominio real:** el teléfono, en vertical, con la app.
2. **Personas no vistas,** para la validación *leave-one-person-out* (LOPO).
3. **El set de robustez** que compromete la propuesta (§3.5): luz, ángulo y distancia.
4. **Clientes reales para el aprendizaje federado:** cada persona es un cliente.
5. **Tomas de "nada" (transición),** para que el modelo pueda decir "no es una seña".

Los datos de la webcam no se tiran: siguen siendo la fuente con la que se compara.

## 2. Participantes y dispositivos

| Participante | Seudónimo en el informe | Dispositivo principal | Notas |
|---|---|---|---|
| Evelin (autora) | P1 | Tablet Lenovo TB321FU | Conoce las señas; ya tiene datos de webcam |
| Novio | P2 | Su teléfono | **[DECIDIR]** modelo y versión de Android |
| Papá | P3 | Su teléfono | **[DECIDIR]** modelo y versión de Android |
| — | — | Tablet del novio | Dispositivo compartido para el bloque C (otro hardware) |

- En el informe se nombra a cada persona solo por su seudónimo (P1, P2, P3).
- **Limitación que se declara:** P2 y P3 no son señantes de LSA; aprenden las 12 señas para la sesión. El modelo se evalúa sobre señas producidas por aprendices, no por personas sordas usuarias de LSA. Un estudio con señantes queda como trabajo futuro, o para el estudio de usabilidad de la Etapa 7 si se consigue.

## 3. Requisitos antes de la sesión

### 3.1 En la app (bloqueantes)
| Requisito | Por qué | Tarea |
|---|---|---|
| El dataset guarda **los landmarks crudos** de cada frame, sin remuestrear ni corregir, más `ts`, `uuid`, `img_w/img_h`, `orientation`, `facing`, `fps`, `n_frames_orig` y `features_version` | Sin los crudos, los datos quedan atados al preprocesamiento de hoy y no se pueden llevar al contrato v2 | T-A14 |
| Cada muestra lleva su **condición** (luz, ángulo, distancia), que se elige en pantalla antes de grabar un bloque | Sin esto el set de robustez no se puede separar por condición | T-A14 (campo `condicion`) + selector en el modo entrenamiento |
| **Orientación fija vertical** en el modo seña | Decisión D-7; una sola geometría | T-A16 |
| **Sesión guiada:** la app lee el orden de la planilla del participante y, después de cada muestra guardada, pasa sola a la siguiente seña y condición (con "Deshacer" para repetir) | Con el orden aleatorio, elegir la seña a mano antes de cada toma suma unos 20 minutos por persona y es fácil grabar con la etiqueta equivocada, que es el error más caro porque no se nota después | nueva (base del modo práctica N-01) |
| `participant_id` seudónimo | Ya está (commit `6511a27`) | T-A31 |
| La app **instalada y probada** en los 4 dispositivos, cada persona con **su propia cuenta** iniciada en su dispositivo | El `participant_id` sale de la cuenta con sesión iniciada: si P2 graba en un dispositivo con la sesión de P1, sus muestras quedan atribuidas a P1 | — |

### 3.2 Materiales
- Un trípode o soporte para el teléfono, a la altura del pecho. Grabar en mano mezcla el temblor con la seña.
- Cinta métrica, o marcas en el piso a 40, 70 y 120 cm de la cámara.
- Las imágenes de referencia de cada seña: el diccionario de la app.
- La planilla de registro impresa o en una notebook (`planilla_registro.csv`).
- Los consentimientos firmados (`consentimiento.md`).
- Cargadores: la sesión es larga y la cámara gasta batería.

### 3.3 Práctica previa (P2 y P3)
Uno o dos días antes, 15 minutos: la autora muestra las 12 señas y cada participante
las practica mirando el diccionario de la app. Se considera que una seña está aprendida
cuando la autora la reconoce sin dudar. Esto **no se graba**.

## 4. Qué se graba

### 4.1 Clases
Las 12 del modelo actual: `hola`, `adios`, `bien`, `como estas`, `gracias`, `e`, `v`,
`l`, `i`, `n`, `mi nombre es` y `por favor`. A eso se suma **`nada`**, que se crea como
"seña propia" en el modo entrenamiento.

### 4.2 Condiciones (propuesta §3.5)
| Factor | Estándar | Adversas |
|---|---|---|
| Luz | interior, luz de techo encendida | exterior (o junto a una ventana con sol); sombra (luz de techo apagada, solo luz indirecta) |
| Ángulo | frontal (0°) | 30° y 60° (la persona gira el cuerpo respecto de la cámara) |
| Distancia | media, 70 cm | cerca, 40 cm; lejos, 120 cm |

En todas las condiciones las dos manos tienen que entrar en el cuadro durante toda la seña.

### 4.3 Bloques por persona

| Bloque | Qué | Tomas | Tiempo estimado |
|---|---|---|---|
| **A: estándar** | 12 clases × **10 tomas**, en la condición estándar | 120 | 25–30 min |
| **N: nada** | 15 tomas de transición: bajar las manos, rascarse la cara, acomodarse el pelo, tocar el teléfono, moverse sin señar | 15 | 5 min |
| **B: robustez** | 12 clases × **1 toma** en cada una de las 6 condiciones adversas (una por vez: el resto, estándar) | 72 | 20–25 min |
| **C: otro dispositivo** | 12 clases × **3 tomas** en la tablet del novio, condición estándar | 36 | 10 min |
| | **Total por persona** | **243** | **≈ 1 h 15 min** (con pausas) |

- **Versión mínima, si no alcanza el tiempo:** bloque A con 8 tomas + N + B con 4 condiciones (sombra, 30°, cerca, lejos) y sin C: unas 159 tomas, alrededor de 45 min. **[DECIDIR]**
- **Orden:** A → N → B → C, con una pausa de 5 minutos entre bloques.
- **Dentro de cada bloque, las clases van en orden aleatorio.** La lista de cada persona sale de `tools/planilla_captura.py` → `planilla_registro.csv` (reproducible: la semilla depende del participante), y la app la sigue en el modo de sesión guiada. Grabar las 10 tomas de una clase seguidas produce tomas casi idénticas, que es justamente lo que infló la validación de la webcam.
- Conviene grabar el bloque A en dos tandas de 5 tomas por clase, separadas por una pausa: dan dos "sesiones" distintas para la validación agrupada por sesión.

### 4.4 Cómo se hace cada toma
1. Las manos empiezan **abajo, fuera del cuadro**.
2. La app empieza a capturar sola cuando detecta las manos. Se hace la **seña completa**, a ritmo natural.
3. Se bajan las manos y se espera a que la app confirme la muestra guardada.
4. Si la toma salió mal (una seña equivocada, la mano fuera de cuadro, un estornudo), **se deshace con el botón "Deshacer"** y se repite. Se anota en la planilla.
5. No se corrige al participante por la predicción de la app: en el modo entrenamiento lo que importa es la etiqueta elegida, no lo que el modelo crea.

## 5. Cómo se usan los datos (decidido antes de grabar)

Fijar esto ahora evita elegir el análisis después de ver los resultados.

- **Validación por persona (LOPO):** se entrena con dos personas y se evalúa con la tercera, rotando. Se reporta junto al esquema viejo (tomas mezcladas) para cuantificar la fuga.
- **Validación agrupada por sesión** dentro de cada persona: las dos tandas del bloque A.
- **Robustez:** el bloque B **nunca** se usa para entrenar. Es solo de evaluación, por condición, con la métrica σ.
- **Otro dispositivo:** el bloque C es solo de evaluación (generalización entre dispositivos).
- **Clase "nada":** bloque N, con su tasa de falsos positivos.
- **Federado:** cada persona es un cliente.
  - El backbone (las LSTM) se entrena **solo con los datos de webcam de P1**, re-extraídos con el contrato v2.
  - El federado de la cabeza usa los bloques A de P1, P2 y P3 desde el teléfono.
  - Se declara que P1 aparece en los dos lados (backbone por webcam y cliente por teléfono). Que P2 y P3 nunca entran al backbone es lo que hace que el piloto sea limpio para ellos. **[DECIDIR]** si se prefiere dejar a P1 fuera del federado.

## 6. Después de la sesión (el mismo día)

1. Por cada dispositivo: **exportar el dataset** (botón Exportar, o `adb pull`) **antes de desinstalar o actualizar nada**.
2. Copiar los archivos a `D:\Respaldo-Tesis\<fecha>\captura_E3\<dispositivo>\` y generar el manifiesto de hashes.
3. Correr `federated/verificar_export.py` sobre cada archivo. Tiene que dar 3 personas y los conteos por clase esperados (comparar con la planilla).
4. Anotar en `plan/progreso.md`: fecha, dispositivos, tomas por persona y bloque, y las incidencias.

## 7. Ética y datos personales

- **Qué se guarda:** solo las coordenadas de 21 puntos por mano en cada frame, y metadatos técnicos (dispositivo, resolución, orientación, condición). **No se guardan imágenes, video ni audio** (D-8).
- **Identidad:** cada persona figura con un `participant_id` seudónimo derivado de su cuenta; el dataset no contiene nombres, mails ni el id de la cuenta.
- **Consentimiento informado por escrito**, antes de grabar (`consentimiento.md`). Que participen familiares no exime del consentimiento: el tribunal lo va a preguntar.
- **Retiro:** cualquier participante puede pedir que se borren sus muestras, en cualquier momento antes de la entrega final. Se borran por `participant_id`, en el dataset y en el respaldo.
- **Publicación del dataset:** solo si el participante lo autoriza aparte (casilla opcional del consentimiento). **[DECIDIR]** si se ofrece.
- **[DECIDIR]** si la carrera o la UNGS piden aval de un comité de ética para este tipo de estudio. Lo consulta la directora.

## 8. Cronograma propuesto

| Fecha (2026) | Qué |
|---|---|
| 08/10 – 15/10 | Requisitos de la app (§3.1): dataset v2 con crudos y condición, orientación fija. Revisión de este protocolo por la directora |
| 16/10 – 17/10 | Práctica de P2 y P3; prueba completa del flujo en los 4 dispositivos con 5 tomas de prueba (que después se borran) |
| **18/10 – 19/10** | **Sesión de captura** (un día por persona si no entra en uno) **[DECIDIR] fecha** |
| 20/10 en adelante | Re-extracción v2, entrenamiento, LOPO, robustez, federado |

La entrega es a mediados de noviembre: la captura no puede pasar del 25/10 sin
recortar el análisis.

## 9. Riesgos de la sesión

| Riesgo | Qué hacer |
|---|---|
| Una persona no puede completar todo | Priorizar A → N → B; C es lo primero que se recorta |
| Se graba con la cuenta equivocada | Antes de cada bloque, mirar en Ajustes de la app qué cuenta está iniciada; anotarlo en la planilla |
| La app se cierra o el teléfono se reinicia | El dataset se guarda muestra por muestra; seguir desde la última toma de la planilla |
| Poca luz en "sombra" y MediaPipe no detecta | Es un resultado válido: registrar las tomas que no se pudieron capturar en lugar de forzar la luz |
| El teléfono de P3 es muy lento | Es justamente el dato de gama baja que falta: medir también sus FPS con las métricas de la app |
