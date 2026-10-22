$ErrorActionPreference = "Stop"

if (Get-Command latexmk -ErrorAction SilentlyContinue) {
    latexmk -pdf main.tex
    exit $LASTEXITCODE
}

if (Get-Command pdflatex -ErrorAction SilentlyContinue) {
    pdflatex main.tex
    if (Get-Command bibtex -ErrorAction SilentlyContinue) {
        bibtex main
    }
    pdflatex main.tex
    pdflatex main.tex
    exit $LASTEXITCODE
}

Write-Host "No se encontro latexmk ni pdflatex. Instala MiKTeX/TeX Live o subi el proyecto a Overleaf."
exit 1
