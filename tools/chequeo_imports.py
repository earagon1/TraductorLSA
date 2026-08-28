#!/usr/bin/env python3
"""Busca referencias a simbolos propios del proyecto que no esten importados.

Sin el SDK de Android no se puede compilar, y este es el error que mas veces
rompio el build: mover una pantalla de archivo y olvidar el import. Kotlin lo
reporta como "Unresolved reference", el build falla y Android Studio instala
el APK anterior, asi que la app parece no haber cambiado.

Solo mira simbolos declarados en este proyecto: si el nombre no esta en el
mapa, no dice nada. Eso lo deja sin falsos positivos a costa de no ver los
simbolos de AndroidX.
"""
import pathlib
import re
import sys

RAIZ = pathlib.Path("app/src/main/java")

DECL = re.compile(
    r"^(?:@\w+(?:\([^)]*\))?\s*)*"
    r"(?:public\s+|internal\s+|private\s+|abstract\s+|sealed\s+|open\s+|data\s+|enum\s+|annotation\s+|value\s+)*"
    r"(?:class|interface|object|fun|val|var)\s+"
    r"(?:<[^>]+>\s+)?"
    r"([A-Z]\w*)\b",
    re.MULTILINE,
)
PAQUETE = re.compile(r"^package\s+([\w.]+)", re.MULTILINE)
IMPORT = re.compile(r"^import\s+([\w.]+)(?:\s+as\s+(\w+))?", re.MULTILINE)
CADENA = re.compile(r'"""[\s\S]*?"""|"(?:\\.|[^"\\])*"')
COMENTARIO = re.compile(r"//[^\n]*|/\*[\s\S]*?\*/")


def sin_ruido(texto):
    return CADENA.sub('""', COMENTARIO.sub("", texto))


def main():
    archivos = sorted(RAIZ.rglob("*.kt"))
    fuentes = {f: f.read_text(encoding="utf-8") for f in archivos}

    # nombre -> {paquetes donde se declara}
    mapa = {}
    for f, texto in fuentes.items():
        mp = PAQUETE.search(texto)
        if not mp:
            continue
        paq = mp.group(1)
        limpio = sin_ruido(texto)
        for nombre in DECL.findall(limpio):
            mapa.setdefault(nombre, set()).add(paq)

    hallazgos = []
    for f, texto in fuentes.items():
        mp = PAQUETE.search(texto)
        if not mp:
            continue
        paq = mp.group(1)
        limpio = sin_ruido(texto)

        importados = set()
        paquetes_comodin = set()
        for ruta, alias in IMPORT.findall(limpio):
            if ruta.endswith(".*"):
                paquetes_comodin.add(ruta[:-2])
            else:
                importados.add(alias or ruta.rsplit(".", 1)[-1])

        cuerpo = limpio[mp.end():]
        cuerpo = IMPORT.sub("", cuerpo)

        for nombre in sorted(set(re.findall(r"\b([A-Z]\w*)\b", cuerpo))):
            donde = mapa.get(nombre)
            if not donde:
                continue                      # no es un simbolo del proyecto
            if paq in donde:
                continue                      # se declara en el mismo paquete
            if nombre in importados:
                continue
            if donde & paquetes_comodin:
                continue
            hallazgos.append(
                (f, nombre, ", ".join(sorted(donde)))
            )

    for f, nombre, donde in hallazgos:
        linea = next(
            (i for i, l in enumerate(fuentes[f].splitlines(), 1)
             if re.search(r"\b" + re.escape(nombre) + r"\b", l)
             and not l.strip().startswith(("import ", "package ", "//", "*"))),
            1,
        )
        print(f"{f}:{linea}: '{nombre}' no esta importado (se declara en {donde})")

    print(f"\nRevisados {len(archivos)} archivos.")
    print("Sin hallazgos." if not hallazgos else f"{len(hallazgos)} referencia(s) sin import.")
    return 1 if hallazgos else 0


if __name__ == "__main__":
    sys.exit(main())
