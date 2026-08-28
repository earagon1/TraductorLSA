#!/usr/bin/env python3
"""
Revisa errores de Compose que el compilador de Kotlin solo detecta con el
plugin de Compose y el SDK de Android disponibles.

Nació de tres errores de build reales: anotaciones que quedaron huérfanas al
mover pantallas de archivo, una anotación repetida, y un Modifier.weight usado
fuera de su scope. Los tres parsean perfecto, así que ningún chequeo de
sintaxis los ve.

    python3 tools/chequeo_compose.py

Devuelve 1 si encuentra algo, para poder engancharlo a un hook si hace falta.
"""
import glob
import pathlib
import re
import sys

RAIZ = "app/src/main/java/**/*.kt"

# Modificadores que solo existen dentro de cierto scope, y qué los habilita.
SCOPES = {
    "weight": {
        "receptores": ("RowScope.", "ColumnScope."),
        "bloques": ("Row(", "Column(", "FlowRow(", "FlowColumn(",
                    "PantallaSenar(", "SeccionAjustes(", "CeldaAjuste("),
    },
    "align": {
        "receptores": ("RowScope.", "ColumnScope.", "BoxScope."),
        "bloques": ("Row(", "Column(", "Box(", "PantallaSenar("),
    },
    "matchParentSize": {"receptores": ("BoxScope.",), "bloques": ("Box(",)},
    "alignByBaseline": {"receptores": ("RowScope.",), "bloques": ("Row(",)},
}


def anotaciones(lineas, archivo, hallazgos):
    """Anotaciones que no preceden a una declaración, o repetidas."""
    for i, linea in enumerate(lineas):
        if not re.match(r"^\s*@(Composable|OptIn)", linea):
            continue
        j = i + 1
        while j < len(lineas) and (not lineas[j].strip() or re.match(r"^\s*@", lineas[j])):
            j += 1
        siguiente = lineas[j].strip() if j < len(lineas) else ""
        if not re.match(r"^(private |internal |)(fun|val|class|object)\b", siguiente):
            hallazgos.append(f"{archivo}:{i+1}  anotación huérfana: {linea.strip()}")

    for i, linea in enumerate(lineas):
        if linea.strip() != "@Composable":
            continue
        j = i + 1
        while j < len(lineas) and (not lineas[j].strip() or re.match(r"^\s*(@|/\*)", lineas[j])):
            if lineas[j].strip() == "@Composable":
                hallazgos.append(f"{archivo}:{i+1}  @Composable repetida (también en {j+1})")
            j += 1


def sin_composable(lineas, archivo, hallazgos):
    """Funciones con nombre en mayúscula que parecen composables y no lo declaran."""
    for i, linea in enumerate(lineas):
        m = re.match(r"^(private |internal )?fun (?:\w+Scope\.)?([A-Z]\w+)\(", linea)
        if not m:
            continue
        k, anot = i - 1, []
        while k >= 0 and (lineas[k].strip().startswith("@") or not lineas[k].strip()):
            if lineas[k].strip().startswith("@"):
                anot.append(lineas[k].strip())
            elif anot:
                break
            k -= 1
        if not any("@Composable" in a for a in anot):
            hallazgos.append(f"{archivo}:{i+1}  {m.group(2)} sin @Composable")


def scopes(lineas, archivo, hallazgos):
    """Modifier.weight / align / matchParentSize fuera del scope que los provee."""
    for i, linea in enumerate(lineas):
        for mod, regla in SCOPES.items():
            if not re.search(r"\." + mod + r"\s*\(", linea):
                continue
            k = i
            while k >= 0 and not re.match(r"^\s*(private |internal |)fun ", lineas[k]):
                k -= 1
            if k < 0:
                continue
            if any(r in lineas[k] for r in regla["receptores"]):
                continue
            pila, j = [], k
            while j < i:
                abre = lineas[j].count("{") + lineas[j].count("(")
                cierra = lineas[j].count("}") + lineas[j].count(")")
                if abre > cierra:
                    pila.append(lineas[j])
                elif cierra > abre and pila:
                    for _ in range(min(cierra - abre, len(pila))):
                        pila.pop()
                j += 1
            if any(any(b in l for b in regla["bloques"]) for l in pila):
                continue
            hallazgos.append(f"{archivo}:{i+1}  .{mod}() fuera de su scope")


def main():
    hallazgos = []
    archivos = sorted(glob.glob(RAIZ, recursive=True))
    for ruta in archivos:
        lineas = pathlib.Path(ruta).read_text(encoding="utf-8").split("\n")
        nombre = ruta.split("/")[-1]
        anotaciones(lineas, nombre, hallazgos)
        sin_composable(lineas, nombre, hallazgos)
        scopes(lineas, nombre, hallazgos)

    print(f"Revisados {len(archivos)} archivos.")
    if not hallazgos:
        print("Sin hallazgos.")
        return 0
    for h in hallazgos:
        print(f"  {h}")
    print(f"\n{len(hallazgos)} para revisar.")
    return 1


if __name__ == "__main__":
    sys.exit(main())
