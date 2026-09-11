#!/usr/bin/env python3
"""Busca variables locales usadas antes de declararse.

Kotlin no permite referenciar un local antes de su declaracion, ni siquiera
desde adentro de una lambda. Sin poder compilar, este error se ve recien
cuando el build falla en la maquina de Evelin, asi que conviene atraparlo aca.

Solo mira declaraciones en el nivel superior del cuerpo de cada funcion: las
de bloques anidados tienen reglas de alcance mas finas y no vale la pena
arriesgar falsos positivos por ellas.
"""
import pathlib
import re
import sys

RAIZ = pathlib.Path("app/src/main/java")

CADENA = re.compile(r'"""[\s\S]*?"""|"(?:\\.|[^"\\])*"')
COMENTARIO = re.compile(r"//[^\n]*|/\*[\s\S]*?\*/")
# Se ancla en la linea del `fun` y la sangria se mide con [ \t] y no con \s:
# \s incluye el salto de linea, asi que desde una linea vacia anterior se comia
# el \n y la sangria daba 1 donde era 0. Las anotaciones van en lineas de arriba
# y no cambian la sangria, asi que no hace falta contemplarlas.
FUNCION = re.compile(r"^([ \t]*)(?:(?:public|internal|private|inline|suspend|operator|tailrec)\s+)*"
                     r"fun\s+(?:<[^>]+>\s*)?(?:[\w.<>?]+\.)?(\w+)\s*\(", re.MULTILINE)
DECLARACION = re.compile(r"^(\s*)(?:val|var)\s+(\w+)\s*(?::|=|\bby\b)")


def limpiar(texto):
    """Deja el codigo sin comentarios ni cadenas, conservando los saltos."""
    def espacios(m):
        return re.sub(r"[^\n]", " ", m.group(0))
    return CADENA.sub(espacios, COMENTARIO.sub(espacios, texto))


def cuerpo_de(lineas, inicio):
    """Rango de lineas del cuerpo de la funcion que abre en `inicio`."""
    nivel, arranco = 0, False
    for i in range(inicio, len(lineas)):
        for c in lineas[i]:
            if c == "{":
                nivel += 1
                arranco = True
            elif c == "}":
                nivel -= 1
                if arranco and nivel == 0:
                    return i
        if arranco and nivel <= 0 and i > inicio:
            return i
    return len(lineas) - 1


def main():
    hallazgos = []
    for archivo in sorted(RAIZ.rglob("*.kt")):
        texto = archivo.read_text(encoding="utf-8")
        limpio = limpiar(texto)
        lineas = limpio.splitlines()

        for m in FUNCION.finditer(limpio):
            i_fun = limpio[:m.start()].count("\n")
            i_fin = cuerpo_de(lineas, i_fun)
            sangria = len(m.group(1)) + 4

            # parametros de la funcion: pueden usarse en cualquier lado
            cabecera = "\n".join(lineas[i_fun:min(i_fun + 30, i_fin + 1)])
            parametros = set(re.findall(r"(\w+)\s*:", cabecera.split("{")[0]))

            declarados = {}
            for i in range(i_fun + 1, i_fin):
                d = DECLARACION.match(lineas[i])
                if d and len(d.group(1)) == sangria:
                    declarados.setdefault(d.group(2), i)

            for nombre, i_decl in declarados.items():
                if nombre in parametros:
                    continue
                patron = re.compile(r"\b" + re.escape(nombre) + r"\b")
                for i in range(i_fun + 1, i_decl):
                    if patron.search(lineas[i]):
                        hallazgos.append(
                            (archivo, i + 1, nombre, m.group(2), i_decl + 1)
                        )
                        break

    for archivo, linea, nombre, funcion, i_decl in hallazgos:
        print(f"{archivo}:{linea}: '{nombre}' se usa antes de declararse "
              f"(esta en la linea {i_decl}, dentro de {funcion})")

    print(f"\nRevisados {len(list(RAIZ.rglob('*.kt')))} archivos.")
    print("Sin hallazgos." if not hallazgos else f"{len(hallazgos)} uso(s) antes de la declaracion.")
    return 1 if hallazgos else 0


if __name__ == "__main__":
    sys.exit(main())
