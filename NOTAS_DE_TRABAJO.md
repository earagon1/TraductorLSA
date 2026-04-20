# Notas de trabajo del informe de Tesina

Este archivo funciona como memoria persistente del proyecto para futuras sesiones de trabajo con Codex.

## Uso de esta memoria

Este archivo no debe tratarse como una nota estatica. Debe actualizarse durante el trabajo cuando aparezca informacion relevante para futuras sesiones.

Codex debe agregar o ajustar notas cuando:

- La usuaria defina una nueva regla de trabajo.
- Se resuelva un problema tecnico o de flujo de trabajo y la solucion pueda repetirse.
- Se tome una decision sobre estructura, estilo, bibliografia, compilacion, Overleaf o LaTeX.
- Se detecte una restriccion importante del proyecto Android o del informe.
- Se incorporen lineamientos institucionales o criterios del jurado.
- Cambie el flujo de sincronizacion entre repo local y Overleaf.

Las actualizaciones deben ser breves, accionables y ubicarse en la seccion mas adecuada. Si el aprendizaje no encaja en ninguna seccion, crear una nueva seccion clara.

Regla importante: si solo se modifica esta memoria y no cambia el contenido compilable del informe, al final de la respuesta debe aclararse que no hace falta subir archivos a Overleaf para ver cambios en el PDF.

## Instrucciones locales para Codex

- Leer `AGENTS.md` y este archivo antes de trabajar sobre el informe.
- Respetar las reglas de redaccion, estructura, LaTeX y flujo de trabajo definidas en `AGENTS.md`.
- Si existe conflicto entre una instruccion general y `AGENTS.md`/`NOTAS_DE_TRABAJO.md`, pedir aclaracion o priorizar la regla mas especifica del proyecto cuando no haya riesgo.
- Mantener esta memoria actualizada cuando se aprenda algo nuevo durante la sesion.

## Contexto general

- Autora: Evelin Emilce Aragon.
- Carrera: Licenciatura en Sistemas.
- Universidad: Universidad Nacional de General Sarmiento.
- Proyecto de tesina: traductor movil en tiempo real para Lengua de Señas Argentina (LSA).
- Tipo de tesina: desarrollo de software acompañado por informe academico.
- Repositorio Android principal: `C:\Users\Evelin\Desktop\Tesis\TraductorLSA`.
- Repositorio local del informe LaTeX: `C:\Users\Evelin\Desktop\Tesis\TraductorLSA\informe-tesina-latex`.

## Lineamientos institucionales incorporados

A partir del material del Taller de Tesina y del Tutorial de la plantilla TTL:

- La tesina debe tratarse como una monografía formal, expositiva, explicativa, argumentativa, coherente, cohesiva e impersonal.
- Debe escribirse en tercera persona.
- Debe privilegiarse la redacción explicativa y descriptiva, con párrafos breves y concisos.
- Debe existir coherencia sintáctica y semántica entre introducción y conclusiones.
- La discusión no debe confundirse con las conclusiones.
- Las conclusiones deben responder a objetivos, preguntas o hipótesis.
- Las recomendaciones o trabajo futuro deben derivarse del trabajo realizado.
- Debe respetarse la plantilla TTL en LaTeX y el formato institucional.
- Mantener A4 como formato base.
- Mantener consistencia en el estilo de citas y referencias a lo largo de todo el documento.
- Punto a validar: si la tesina debe seguir APA 7 estrictamente o si se mantiene el esquema numérico de BibTeX de la plantilla.

## Criterios de evaluación del jurado a tener presentes

- Calidad de la investigación.
- Rigurosidad metodológica.
- Calidad y validez de las fuentes.
- Organización y estructura del trabajo.
- Coherencia entre introducción y conclusiones.
- Capacidad de comunicación escrita y oral.
- Dominio del tema y defensa de argumentos y conclusiones.

## Tema del informe

El informe debe presentar el desarrollo y evaluacion de un prototipo de aplicacion movil Android para traducir señas de LSA a texto y voz en tiempo real.

El enfoque tecnico del proyecto combina:

- Android/Kotlin.
- Jetpack Compose.
- CameraX para captura de imagen.
- MediaPipe Hand Landmarker para deteccion de landmarks de manos.
- Extraccion de caracteristicas a partir de landmarks.
- Secuencias temporales normalizadas.
- TensorFlow Lite para clasificacion de señas.
- Text-to-Speech de Android para salida de voz.
- Ejecucion local/offline como decision importante de diseño.

## Enfoque academico

El informe no debe presentarse como "solo hice una app". Debe explicar:

- Que problema real se busca resolver.
- Por que el problema importa en terminos de accesibilidad e inclusion.
- Que solucion se propone.
- Como se diseño la arquitectura.
- Como se implemento el prototipo.
- Como se evaluo.
- Que limitaciones tiene.
- Que trabajo futuro queda abierto.

Idea central sugerida:

> Se propone y desarrolla un prototipo de traductor movil en tiempo real para Lengua de Señas Argentina, ejecutado localmente en Android, orientado a mejorar la accesibilidad comunicacional entre personas sordas y oyentes.

## Estructura actual del informe

Archivo principal:

- `main.tex`

Archivos de configuracion:

- `preamble.tex`
- `bibliografia.bib`
- `.gitignore`
- `build.ps1`
- `README.md`
- `NOTAS_DE_TRABAJO.md`

Capitulos:

- `chapters/01-introduccion.tex`
- `chapters/02-marco-teorico.tex`
- `chapters/03-analisis-problema.tex`
- `chapters/04-propuesta-solucion.tex`
- `chapters/05-implementacion.tex`
- `chapters/06-pruebas-resultados.tex`
- `chapters/07-conclusiones.tex`

Carpetas auxiliares:

- `figures/`
- `tables/`

## Flujo de trabajo con Overleaf

La usuaria no tiene funciones premium de Overleaf, por lo tanto no se puede mantener sincronizacion automatica por Git.

Regla principal:

- Codex edita los archivos localmente en `informe-tesina-latex`.
- Overleaf se usa para visualizar, compilar y revisar como queda el PDF.
- Despues de cada cambio, Codex debe listar explicitamente que archivos modifico y cuales debe subir la usuaria a Overleaf.
- La usuaria debe subir solamente esos archivos modificados, no todo el proyecto en `.zip`, salvo que se indique lo contrario.
- Si Codex agrega un archivo nuevo, una imagen, una tabla o una referencia bibliografica, debe indicarlo claramente.
- Si la usuaria edita mucho contenido directamente en Overleaf, antes de seguir trabajando localmente conviene descargar el ZIP actualizado desde Overleaf y reemplazar/sincronizar la version local.

Al finalizar cada tarea sobre el informe, Codex debe incluir una seccion breve:

```text
Archivos para subir a Overleaf:
- ruta/del/archivo1.tex
- ruta/del/archivo2.bib
```

Si solo se modifican notas internas que no afectan el PDF, aclarar:

```text
No hace falta subir archivos a Overleaf para ver cambios en el PDF.
```

## Compilacion

En la computadora local no se detecto `pdflatex`, `xelatex` ni `latexmk` al momento de crear el repo.

Por ahora, la compilacion se realiza en Overleaf.

Archivo principal de compilacion:

- `main.tex`

Si se instala MiKTeX o TeX Live localmente, se puede probar:

```powershell
powershell -ExecutionPolicy Bypass -File .\build.ps1
```

## Reglas de estilo para el informe

- Redactar en español academico claro.
- Evitar sonar demasiado informal.
- Evitar afirmar resultados no demostrados.
- Diferenciar claramente prototipo, sistema final y trabajo futuro.
- Mantener foco en factibilidad tecnica y accesibilidad.
- No prometer traduccion completa de LSA.
- Usar "Lengua de Señas Argentina (LSA)" la primera vez y luego "LSA".
- Usar "personas sordas" y "personas oyentes" de forma respetuosa.
- Evitar expresiones como "lenguaje de señas"; preferir "lengua de señas".
- Cuando se hable del alcance, remarcar que el conjunto de señas es acotado.
- Cuando se hable de privacidad, conectar con la decision de ejecucion local/offline.

## Datos tecnicos conocidos del proyecto Android

Arquitectura general:

1. Captura de imagen con CameraX.
2. Conversion de frames a `Bitmap`.
3. Deteccion de manos con MediaPipe.
4. Extraccion de landmarks de hasta dos manos.
5. Construccion de vector de caracteristicas de 126 valores.
6. Normalizacion temporal de secuencias, normalmente a 15 frames.
7. Clasificacion con modelo TensorFlow Lite.
8. Presentacion de prediccion en pantalla.
9. Sintesis de voz con Android Text-to-Speech.

Paquetes principales del proyecto Android:

- `camera/`: captura y conversion de frames.
- `detection/`: deteccion de manos con MediaPipe.
- `features/`: extraccion de caracteristicas y buffer temporal.
- `ml/`: motor de inferencia y clasificacion.
- `speech/`: sintesis de voz.
- `ui/`: pantallas, overlays y componentes visuales.
- `model/`: modelos de datos simples.

Archivos/modelos relevantes:

- `app/src/main/assets/hand_landmarker.task`
- `app/src/main/assets/actions_15_opt.tflite`
- `app/src/main/assets/actions_15_f32.tflite`
- `app/src/main/assets/words.json`

Resultados preliminares mencionados en el README Android:

- Galaxy Tab S7: 14.0 FPS efectivos, inferencia aproximada de 32 ms.
- Galaxy A55: 5.3 FPS efectivos, inferencia aproximada de 30 ms.
- Galaxy A31: 3.0 FPS efectivos, inferencia aproximada de 559 ms.

Interpretacion tecnica preliminar:

- El mayor cuello de botella parece estar en captura, conversion de imagen y extraccion de landmarks.
- En algunos dispositivos, la inferencia TensorFlow Lite es relativamente liviana comparada con las etapas previas.
- La orientacion horizontal puede mejorar la estabilidad del tracking.

## Decisiones importantes ya tomadas

- El informe se escribira en LaTeX.
- El repo del informe esta separado dentro del proyecto Android, pero con `.git` propio.
- El repo Android ignora `/informe-tesina-latex/` para evitar mezclar historiales.
- Overleaf se usa como entorno de compilacion/visualizacion.
- Codex debe mantener la estructura por capitulos, salvo que la usuaria pida simplificarla.
- Codex debe evitar modificar archivos generados por Overleaf o LaTeX.

## Estado inicial del repo LaTeX

Commit inicial:

- `a7c0b78 Crear estructura inicial del informe en LaTeX`

El commit inicial contiene:

- estructura base del informe
- capitulos separados
- resumen preliminar
- introduccion inicial
- secciones guia para completar
- bibliografia preliminar
- script `build.ps1`

## Pendientes recomendados

- Completar y mejorar el Capitulo 2: marco teorico y antecedentes.
- Profundizar Capitulo 4: arquitectura de la solucion.
- Desarrollar Capitulo 5 con referencias concretas al codigo Android.
- Revisar Capitulo 6 con datos reales y tablas mas completas.
- Agregar capturas de pantalla de la app en `figures/`.
- Reemplazar placeholders de director/a, fecha institucional y datos de portada.
- Verificar formato exigido por la universidad o por la plantilla provista en `Tutorial.pdf`.
- Incorporar bibliografia academica real sobre reconocimiento de lenguas de señas, LSA, MediaPipe y modelos moviles.

## Recordatorio para Codex

Antes de trabajar en el informe en futuras sesiones:

1. Leer este archivo.
2. Revisar `git status` dentro de `informe-tesina-latex`.
3. No asumir que Overleaf esta sincronizado automaticamente.
4. Al terminar, informar archivos modificados para subir a Overleaf.
5. Si hay cambios locales sin commit, preguntar o revisar antes de sobrescribir.
