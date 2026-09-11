# -*- coding: utf-8 -*-
"""Acceso con consentimiento explicito: la promesa se acepta antes de entregar la cuenta."""
import pathlib

import subprocess
# Se parte siempre del artboard original guardado en git: el script reescribe
# Acceso.dc.html, asi que leerlo del disco lo haria no repetible.
base = subprocess.run(
    ["git", "show", "HEAD:docs/design/Acceso.dc.html"],
    capture_output=True, text=True, check=True, cwd="../..",
).stdout

PIE_VIEJO = '''  <p style="position: relative; margin: 22px 0 0; font-size: 12.5px; line-height: 1.55; font-weight: 400; text-align: center; color: #8494CC; text-wrap: pretty; flex-shrink: 0;">Al continuar aceptás los <a href="#">Términos</a> y la <a href="#">Política de privacidad</a>.</p>
'''
assert PIE_VIEJO in base

INICIO_BOTONES = '''  <div style="position: relative; display: flex; align-items: center; justify-content: center; gap: 10px; height: 60px; border-radius: 18px; background: #FFFFFF;'''
assert INICIO_BOTONES in base


def casilla(aceptado, alertado=False):
    if aceptado:
        return ('<div style="display:flex;align-items:center;justify-content:center;width:24px;height:24px;border-radius:8px;'
                'background:#5D8EF9;flex-shrink:0;margin-top:1px;">'
                '<svg viewBox="0 0 24 24" width="15" height="15" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">'
                '<path d="M5 12.5 9.5 17 19 7.5" stroke="#FFFFFF" stroke-width="2.8" stroke-linecap="round" stroke-linejoin="round"></path></svg></div>')
    borde = "#FFBE1B" if alertado else "rgba(220,231,254,0.42)"
    return (f'<div style="width:24px;height:24px;border-radius:8px;border:2px solid {borde};'
            'flex-shrink:0;margin-top:1px;box-sizing:border-box;"></div>')


def consentimiento(aceptado, alertado=False):
    borde = "#FFBE1B" if alertado else ("rgba(125,166,255,0.40)" if aceptado else "rgba(220,231,254,0.18)")
    fondo = "rgba(93,142,249,0.12)" if aceptado else "rgba(255,255,255,0.06)"
    aviso = ('' if not alertado else
             '<div style="display:flex;align-items:center;gap:7px;margin-top:11px;">'
             '<svg viewBox="0 0 24 24" width="14" height="14" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">'
             '<circle cx="12" cy="12" r="9" stroke="#FFBE1B" stroke-width="1.9"></circle>'
             '<path d="M12 7.4v5.4" stroke="#FFBE1B" stroke-width="2.1" stroke-linecap="round"></path>'
             '<circle cx="12" cy="16.4" r="1.2" fill="#FFBE1B"></circle></svg>'
             '<span style="font-size:12px;font-weight:600;color:#FFBE1B;">Necesitamos que aceptes para seguir</span></div>')
    return f'''  <div style="position: relative; padding: 15px 16px 14px; border-radius: 20px; background: {fondo}; border: 1px solid {borde}; margin-bottom: 18px; flex-shrink: 0;">
    <div style="display: flex; gap: 12px;">
      {casilla(aceptado, alertado)}
      <div style="flex-grow: 1; min-width: 0;">
        <div style="font-size: 13px; line-height: 1.5; color: #DCE7FE;">La cámara graba video para reconocer tus señas. <b style="font-weight:600;color:#FFFFFF;">El reconocimiento pasa entero en tu teléfono y no se sube ningún video.</b> Si sincronizás tu cuenta, las muestras que grabes en entrenamiento se guardan con ella.</div>
        <div style="margin-top: 9px; font-size: 12px; color: #8494CC;"><a href="#">Términos</a> · <a href="#">Política de privacidad</a></div>
      </div>
    </div>{aviso}
  </div>

'''


def escribir(nombre, aceptado, alertado=False):
    s = base.replace(PIE_VIEJO, "")
    # el consentimiento va ARRIBA de los botones: se lee y despues se decide
    s = s.replace(INICIO_BOTONES, consentimiento(aceptado, alertado) + INICIO_BOTONES, 1)
    if not aceptado:
        # los dos botones quedan apagados hasta que se acepte
        s = s.replace('border-radius: 18px; background: #FFFFFF; box-shadow: 0 12px 28px rgba(3,8,30,0.45);',
                      'border-radius: 18px; background: rgba(255,255,255,0.32);')
        s = s.replace('background: rgba(255,255,255,0.10); border: 1.5px solid rgba(220,231,254,0.34);',
                      'background: rgba(255,255,255,0.05); border: 1.5px solid rgba(220,231,254,0.14); opacity: 0.55;')
        s = s.replace('<span style="font-size: 17.5px; font-weight: 600; color: #0E1738;">',
                      '<span style="font-size: 17.5px; font-weight: 600; color: rgba(14,23,56,0.55);">')
    pathlib.Path(nombre).write_text(s, encoding="utf-8")


escribir("Acceso.dc.html", aceptado=True)
escribir("AccesoBloqueado.dc.html", aceptado=False, alertado=True)
print("listo")
