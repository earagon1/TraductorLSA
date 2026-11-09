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

Capitulos (reestructurados el 2026-10-01):

- `chapters/01-introduccion.tex`
- `chapters/02-marco-teorico.tex` (marco teorico y estado del arte)
- `chapters/03-metodologia.tex`
- `chapters/04-dataset-modelo.tex`
- `chapters/05-diseno-implementacion.tex` (incluye requisitos, antes en analisis-problema)
- `chapters/06-desafios-tecnicos.tex` (espejado de manos/handedness, dispositivos de gama baja, orientacion)
- `chapters/07-experimentos-resultados.tex`
- `chapters/08-discusion.tex` (incluye requisitos minimos de hardware)
- `chapters/09-conclusiones.tex`

Decisiones de la sesion 2026-10-01:

- Aprendizaje federado queda fuera del informe por ahora; hay un comentario en `main.tex` donde ira su capitulo (despues del 5).
- Citas numericas con BibTeX (`unsrt`, orden de aparicion), igual que la plantilla. Ya no se usa biblatex/APA; las entradas web van como `@misc`.
- Contenido pendiente se marca con `\pendiente{...}` (definido en `preamble.tex`, sale en rojo en el PDF).
- Decisiones abiertas de la autora se marcan con `\revisar{pregunta}{contenido}` (naranja). Hoy: palabras clave del resumen (dejar o sacar).
- Abreviaciones y glosario viven en `preliminares/`; agregar terminos a medida que aparecen en los capitulos.
- Cap. 1 aprobado por la autora (2026-10-04). Cap. 2 redactado el 2026-10-04 con 18 fuentes verificadas; queda pendiente el relevamiento de apps existentes (seccion 2.7).
- Dos referencias de la propuesta aprobada tenian autores incorrectos: el survey de Electronics 12(12) 2678 es de Zeyu Liang et al. (no "Liang, R."), y "Challenges with Sign Language Datasets" (LREC 2022) es de De Sisto et al. (no Koller). En el informe se usan las versiones corregidas.
- MediaPipe Hands (Zhang et al. 2020) estima 21 landmarks 2.5D (x, y, profundidad relativa a la muneca) y tiene una salida de handedness; util para el Cap. 6 (espejado).
- Pendiente para la migracion a la plantilla: `unsrt` imprime "and", "In", "et al." en ingles; evaluar estilo en espanol.
- No poner supervisor/a en la portada hasta que la autora lo confirme con el profesor del taller.
- La migracion a la plantilla oficial UNGS (portada IDEI, `tesina.tex`) se hace al final; la autora tiene la plantilla en `Downloads/plantilla.zip`.
- Dispositivos de prueba: tres antiguos, un telefono reciente y dos tablets de alta gama (seis en total). Faltan modelos exactos de los que no son Tab S7, A55 y A31.
- El modelo sigue en entrenamiento: no cerrar capitulos 4 y 7 hasta tener el modelo final.
- Se avanza capitulo por capitulo.
- Entrenamiento en el dispositivo y aprendizaje federado todavia no estan implementados en la app (hoy solo se graban muestras con `client_id`). Figuran como objetivos especificos 5 y 6 del Cap. 1; no redactarlos como hechos hasta que la autora los termine.
- MiKTeX esta instalado (`C:\Users\Evelin\AppData\Local\Programs\MiKTeX`). Se compila localmente con `build.ps1` (pdflatex + bibtex) y se revisa el PDF en Edge, que no bloquea el archivo. Overleaf queda como opcion, no es necesario.
- Archivos en UTF-8 sin BOM: no escribir .tex con `Set-Content -Encoding UTF8` de PowerShell 5.1 porque agrega BOM.

Carpetas auxiliares:

- `figures/`
- `tables/`

## Sesión 2026-10-22 (Claude): resultados y capítulo de aprendizaje federado

- El aprendizaje federado y el entrenamiento en el dispositivo YA ESTÁN IMPLEMENTADOS y probados en dispositivos reales (plan en `Tesis/plan/federado_completo.md`, avance en `Tesis/plan/progreso.md`). Las notas anteriores que dicen "no redactarlos como hechos" quedan superadas: se pueden redactar con los resultados medidos.
- Nuevo capítulo `chapters/05b-aprendizaje-federado.tex` (`\label{cap:federado}`), incluido en `main.tex` después del 5. Por eso Experimentos y resultados quedó como Capítulo 8.
- `chapters/07-experimentos-resultados.tex` redactado completo (`\label{cap:resultados}`): esquemas de validación, calibración, consistencia σ, robustez, federado (simulación, costo en el dispositivo, piloto, persona nueva y personalización, límite y rotación), rendimiento en tres dispositivos y variantes del modelo exportado. Todas las cifras salen de `modelo_LSA/resultados_*` y `resultados_tesis/resultados.md`; no transcribir a mano valores nuevos sin regenerarlos.
- Se agregaron los labels `cap:metodologia` y `cap:discusion`.
- Figura `figures/robustez-por-condicion.png` (copiada de `modelo_LSA/resultados_robustez/2026-10-17_p1p2p3_5semillas/robustez.png`).
- Nueva referencia `konecny2016` (Konečný et al., arXiv:1610.05492), citada en la propuesta.
- Las referencias a tablas en el texto dicen «Tabla» (babel con `es-tabla`), no «Cuadro».
- La tabla preliminar de abril (Tab S7, A55, A31) se sacó: se midió con otra versión del pipeline. Los dispositivos medidos con la versión final son Galaxy A55, OnePlus CPH2663 y Lenovo TB321FU.
- Compilación local (bash): `PATH=/c/Users/Evelin/AppData/Local/Programs/MiKTeX/miktex/bin/x64:$PATH`; `pdflatex main; bibtex main; pdflatex main; pdflatex main`.
- El repo del informe tiene dueño distinto (sandbox de Codex): usar `git -c safe.directory=* ...` en cada comando, sin tocar la configuración global.
- Orden acordado con la autora: capítulo 8 (resultados, hecho), luego 4 (datos y modelo), 5 (diseño e implementación), 3 (metodología), 9 y 10 (discusión y conclusiones), y al final ajustar 1 y 2.

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

Estado revisado contra el codigo Android el 2026-04-20.

Tambien se leyo `Propuesta de Proyecto de Tesina-Aragon Evelin-2.pdf` el 2026-04-20. Ese PDF es la propuesta original del proyecto. La autora aclaro que dos dispositivos/modos y aprendizaje federado siguen siendo requisitos del proyecto, aunque federado aun esta pendiente de implementacion.

Regla de interpretacion de la propuesta aprobada:

- La propuesta aprobada por el profesor funciona como guia base de objetivos y compromisos.
- En futuras sesiones, contrastar siempre propuesta aprobada, estado actual del codigo, pendientes y agregados evolutivos.
- El codigo actual puede contener funcionalidades no mencionadas en la propuesta, como Clerk, Lottie, onboarding, diccionario o modo invitada.
- Esos agregados no deben tratarse automaticamente como errores o fuera de alcance: pueden mantenerse si se justifican como decisiones surgidas durante el desarrollo.
- En el informe, distinguir con claridad entre el nucleo comprometido por la propuesta y las mejoras de implementacion/UX incorporadas despues.

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
- `voice/`: voz a texto mediante `SpeechRecognizer` de Android.
- `ui/`: pantallas, overlays y componentes visuales.
- `model/`: modelos de datos simples.

Archivos/modelos relevantes:

- `app/src/main/assets/hand_landmarker.task`
- `app/src/main/assets/actions_15_opt.tflite`
- `app/src/main/assets/actions_15_f32.tflite`
- `app/src/main/assets/words.json`
- `app/src/main/assets/dictionary/*.jpg`
- `app/src/main/assets/entrenamiento28-10/` como copia historica o alternativa a validar.

Vocabulario vigente observado en `words.json`:

- `hola`
- `adios`
- `bien`
- `como_estas`
- `gracias`
- `e`
- `v`
- `l`
- `i`
- `n`

Pantallas/funciones actuales de la app:

- Splash y onboarding.
- Autenticacion opcional con Clerk o modo invitada.
- Home con acceso a traducir señas, traducir voz, entrenamiento, diccionario y ajustes.
- Traduccion de señas con camara, overlay, prediccion textual y Text-to-Speech.
- Voz a texto como funcion complementaria.
- Modo entrenamiento protegido por cuenta, con seleccion de seña, captura de secuencias etiquetadas, guardado local y exportacion JSON.
- Diccionario de señas con imagenes desde assets.

Requisito de dos dispositivos/modos:

- No se desarrollan dos apps separadas.
- La misma app debe poder usarse en dos dispositivos Android.
- Un dispositivo puede operar en modo LSA a texto/voz.
- El otro dispositivo puede operar en modo voz a texto.
- Esta organizacion representa las dos perspectivas de uso de la propuesta original: persona que realiza señas y persona oyente.

Requisito de aprendizaje federado:

- Aprendizaje federado es obligatorio y esta pendiente.
- Debe definirse la arquitectura concreta: simulacion controlada, backend agregador, Flower, TensorFlow Federated u otra alternativa.
- El modo entrenamiento y el archivo `lsa_samples.json` pueden servir como fuente de datos local, pero esto debe confirmarse tecnicamente.
- Hasta implementarlo, no afirmar resultados de federado ni mejoras de precision.

Nota importante para redaccion:

- La frase "ejecucion local/offline" debe aplicarse con precision al pipeline principal de reconocimiento LSA a texto/voz: captura, landmarks, features, TFLite y TTS.
- No afirmar que toda la aplicacion es completamente offline, porque hay autenticacion/sincronizacion opcional con Clerk y voz a texto mediante `SpeechRecognizer`, cuya disponibilidad offline depende del dispositivo/motor instalado.
- Para privacidad, conviene redactar que el flujo de video/reconocimiento no requiere enviar imagenes a servidores externos.
- El modo entrenamiento actual no reentrena el modelo en el telefono; recolecta muestras etiquetadas y permite exportarlas para trabajo posterior.
- La propuesta original planteaba dos dispositivos Android complementarios, offline total, modelo LSTM, aprendizaje federado y metricas como accuracy, F1-score, matrices de confusion y consistencia estadistica. En el informe final, esos puntos deben validarse contra el codigo y las pruebas actuales.
- Voz a texto debe presentarse como el segundo modo requerido para la perspectiva de la persona oyente.
- Aprendizaje federado debe presentarse como requisito pendiente hasta que se implemente y valide.
- Clerk, Lottie, onboarding, diccionario y otros agregados posteriores pueden mencionarse solo si aportan a explicar la app implementada. No deben desplazar el eje academico principal: accesibilidad, reconocimiento LSA, procesamiento local, dos modos y federado.

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
- Definir como nombrar la app en el informe: "SeÑAR" o "Traductor LSA".
- Validar orientacion, mirroring y handedness antes de cerrar la descripcion tecnica.
- Confirmar modelo final usado para demo y resultados.
- Documentar condiciones de prueba antes de presentar metricas como resultados finales.
- Definir e implementar aprendizaje federado.
- Preparar diagrama del escenario con dos dispositivos usando la misma app en dos modos.
- Preparar una tabla de trazabilidad entre propuesta aprobada, codigo actual, pendientes y agregados evolutivos.

## Recordatorio para Codex

Antes de trabajar en el informe en futuras sesiones:

1. Leer este archivo.
2. Revisar `git status` dentro de `informe-tesina-latex`.
3. No asumir que Overleaf esta sincronizado automaticamente.
4. Al terminar, informar archivos modificados para subir a Overleaf.
5. Si hay cambios locales sin commit, preguntar o revisar antes de sobrescribir.
