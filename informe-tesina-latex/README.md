# Informe de Tesina en LaTeX

Repositorio local para redactar el informe de tesina:

**Traductor movil en tiempo real para Lengua de Señas Argentina basado en aprendizaje automatico y ejecucion local en Android.**

## Estructura

- `main.tex`: documento principal.
- `preamble.tex`: configuracion general de paquetes y formato.
- `chapters/`: capitulos del informe.
- `figures/`: imagenes, diagramas y capturas.
- `tables/`: tablas complementarias.
- `bibliografia.bib`: referencias bibliograficas.

## Como compilar

Si tenes MiKTeX, TeX Live u Overleaf:

```bash
pdflatex main.tex
bibtex main
pdflatex main.tex
pdflatex main.tex
```

Si usas `latexmk`:

```bash
latexmk -pdf main.tex
```

## Nota

Se compila localmente con MiKTeX ejecutando `build.ps1`; Overleaf es opcional.
