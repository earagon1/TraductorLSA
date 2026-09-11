# -*- coding: utf-8 -*-
"""Acceso con una linea, y la politica en una card modal con lectura hasta el final."""
import pathlib
from _g import suelto, escribir

# Acceso.base.html es el artboard original, sin el bloque de consentimiento.
# Se guarda aparte porque este script REESCRIBE Acceso.dc.html: leer la salida
# como entrada haria que la segunda corrida no encontrara nada que reemplazar.
# El nombre no termina en .dc.html a proposito, para que el canvas no lo tome
# como un artboard mas.
base = pathlib.Path("Acceso.base.html").read_text(encoding="utf-8")

PIE = '''  <p style="position: relative; margin: 22px 0 0; font-size: 12.5px; line-height: 1.55; font-weight: 400; text-align: center; color: #8494CC; text-wrap: pretty; flex-shrink: 0;">Al continuar aceptás los <a href="#">Términos</a> y la <a href="#">Política de privacidad</a>.</p>
'''
BOTONES = '''  <div style="position: relative; display: flex; align-items: center; justify-content: center; gap: 10px; height: 60px; border-radius: 18px; background: #FFFFFF;'''
assert PIE in base and BOTONES in base


def fila(aceptado, alertado=False):
    if aceptado:
        casilla = ('<div style="display:flex;align-items:center;justify-content:center;width:23px;height:23px;'
                   'border-radius:7px;background:#5D8EF9;flex-shrink:0;">'
                   '<svg viewBox="0 0 24 24" width="14" height="14" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">'
                   '<path d="M5 12.5 9.5 17 19 7.5" stroke="#FFFFFF" stroke-width="2.8" stroke-linecap="round" stroke-linejoin="round"></path></svg></div>')
    else:
        borde = "#FFBE1B" if alertado else "rgba(220,231,254,0.45)"
        casilla = (f'<div style="width:23px;height:23px;border-radius:7px;border:2px solid {borde};'
                   'flex-shrink:0;box-sizing:border-box;"></div>')
    borde_caja = "#FFBE1B" if alertado else ("rgba(125,166,255,0.38)" if aceptado else "rgba(220,231,254,0.20)")
    fondo = "rgba(93,142,249,0.12)" if aceptado else "rgba(255,255,255,0.06)"
    texto = ('Acepto la <span style="color:#DCE7FE;text-decoration:underline;">política de privacidad</span>'
             if not aceptado else 'Política de privacidad aceptada')
    aviso = ('' if not alertado else
             '<div style="display:flex;align-items:center;gap:7px;margin-top:10px;padding-left:35px;">'
             '<svg viewBox="0 0 24 24" width="13" height="13" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">'
             '<circle cx="12" cy="12" r="9" stroke="#FFBE1B" stroke-width="2"></circle>'
             '<path d="M12 7.4v5.4" stroke="#FFBE1B" stroke-width="2.2" stroke-linecap="round"></path>'
             '<circle cx="12" cy="16.4" r="1.2" fill="#FFBE1B"></circle></svg>'
             '<span style="font-size:11.5px;font-weight:600;color:#FFBE1B;">Leela y aceptala para continuar</span></div>')
    flecha = ('' if aceptado else
              '<svg viewBox="0 0 24 24" width="17" height="17" fill="none" style="flex-shrink:0;" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">'
              '<path d="M9.5 5.5 16 12l-6.5 6.5" stroke="#8494CC" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"></path></svg>')
    return f'''  <div style="position: relative; padding: 14px 16px; border-radius: 18px; background: {fondo}; border: 1px solid {borde_caja}; margin-bottom: 18px; flex-shrink: 0;">
    <div style="display: flex; align-items: center; gap: 12px;">
      {casilla}
      <span style="flex-grow:1;min-width:0;font-size:14px;font-weight:500;color:#DCE7FE;">{texto}</span>{flecha}
    </div>{aviso}
  </div>

'''


def acceso(nombre, aceptado, alertado=False):
    s = base.replace(PIE, "").replace(BOTONES, fila(aceptado, alertado) + BOTONES, 1)
    if not aceptado:
        s = s.replace('border-radius: 18px; background: #FFFFFF; box-shadow: 0 12px 28px rgba(3,8,30,0.45);',
                      'border-radius: 18px; background: rgba(255,255,255,0.32);')
        s = s.replace('background: rgba(255,255,255,0.10); border: 1.5px solid rgba(220,231,254,0.34);',
                      'background: rgba(255,255,255,0.05); border: 1.5px solid rgba(220,231,254,0.14); opacity: 0.55;')
        s = s.replace('<span style="font-size: 17.5px; font-weight: 600; color: #0E1738;">',
                      '<span style="font-size: 17.5px; font-weight: 600; color: rgba(14,23,56,0.55);">')
    pathlib.Path(nombre).write_text(s, encoding="utf-8")


acceso("Acceso.dc.html", aceptado=True)
acceso("AccesoBloqueado.dc.html", aceptado=False, alertado=True)

# ── la card modal ───────────────────────────────────────────────────────────
def seccion(titulo, cuerpo, acento=None):
    barra = "" if acento is None else f'border-left:3px solid {acento};padding-left:13px;'
    color_t = acento or "#1E2230"
    return (f'<div style="margin-top:22px;{barra}">'
            f'<div style="font-size:14.5px;font-weight:700;letter-spacing:-0.1px;color:{color_t};">{titulo}</div>'
            f'<div style="margin-top:6px;font-size:13.5px;line-height:1.55;color:#5C6474;">{cuerpo}</div></div>')

CUERPO = (
    '<div style="font-size:13.5px;line-height:1.55;color:#5C6474;">SeÑAR funciona sin cuenta y sin conexión para traducir '
    'señas. Esto es todo lo que hace con tus datos, en castellano y sin vueltas.</div>'
    + seccion("La cámara", "Se usa para ubicar tus manos en el cuadro. Cada imagen se procesa y se descarta en el momento: "
                           "la app no guarda video ni fotos, ni en el teléfono ni afuera.")
    + seccion("El reconocimiento de señas", "Los dos modelos viven adentro de la app. Corren en tu teléfono, no consultan "
                                            "ningún servidor y funcionan en modo avión.")
    + seccion("El micrófono y la voz",
              "Para traducir voz a texto, SeÑAR le pide el trabajo al servicio de reconocimiento de tu teléfono, y le "
              "pide que lo resuelva sin conexión. <b style=\"color:#1E2230;\">Es una preferencia, no una garantía: el servicio "
              "puede ignorarla y transcribir en sus servidores.</b> La app nunca reintenta por internet a tus espaldas, y "
              "podés apagar la preferencia en Ajustes. Para comprobarlo, poné el teléfono en modo avión y fijate si sigue "
              "transcribiendo.", acento="#B07800")
    + seccion("Tu cuenta", "Sincronizar con Google es opcional. Si lo hacés, se guardan tu nombre, tu foto y tu correo a "
                           "través de Clerk, que es quien maneja el inicio de sesión.")
    + seccion("Las muestras de entrenamiento", "Si grabás señas en el modo entrenamiento, quedan en un archivo dentro de tu "
                                               "teléfono, junto al identificador de tu cuenta. No se envían a ningún lado: "
                                               "salen solo si vos tocás «Exportar».")
    + seccion("Lo que la app no hace", "No publica nada, no muestra publicidad, no comparte tus datos con terceros y no te "
                                       "rastrea entre aplicaciones.")
)

def modal(nombre, al_final):
    desplazamiento = -455 if al_final else -40
    if al_final:
        pie = '''      <div style="display:flex;align-items:center;gap:12px;">
        <div style="display:flex;align-items:center;justify-content:center;width:23px;height:23px;border-radius:7px;background:#3B6AE8;flex-shrink:0;">
          <svg viewBox="0 0 24 24" width="14" height="14" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true"><path d="M5 12.5 9.5 17 19 7.5" stroke="#FFFFFF" stroke-width="2.8" stroke-linecap="round" stroke-linejoin="round"></path></svg>
        </div>
        <span style="flex-grow:1;font-size:14px;font-weight:500;color:#1E2230;">Leí y acepto la política de privacidad</span>
      </div>
      <button style="display:flex;align-items:center;justify-content:center;width:100%;height:52px;margin-top:14px;border:none;border-radius:18px;background:#3B6AE8;cursor:pointer;">
        <span class="b" style="font-size:15.5px;font-weight:600;color:#FFFFFF;">Aceptar y volver</span>
      </button>'''
    else:
        pie = '''      <div style="display:flex;align-items:center;gap:12px;">
        <div style="width:23px;height:23px;border-radius:7px;border:2px solid #C9D3E6;flex-shrink:0;box-sizing:border-box;"></div>
        <span style="flex-grow:1;font-size:14px;font-weight:500;color:#8B93A5;">Leí y acepto la política de privacidad</span>
        <button style="display:flex;align-items:center;gap:5px;padding:8px 12px;border:1px solid #E3E8F2;border-radius:999px;background:#FFFFFF;cursor:pointer;flex-shrink:0;">
          <span class="b" style="font-size:12px;font-weight:600;color:#3B6AE8;">Ir al final</span>
          <svg viewBox="0 0 24 24" width="13" height="13" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true"><path d="M12 5v13m0 0 5-5m-5 5-5-5" stroke="#3B6AE8" stroke-width="2.1" stroke-linecap="round" stroke-linejoin="round"></path></svg>
        </button>
      </div>
      <div style="margin-top:11px;font-size:11.5px;color:#8B93A5;">Seguí leyendo hasta el final para poder aceptar.</div>'''

    contenido = f'''<div style="position:absolute;inset:0;background:radial-gradient(120% 60% at 50% 30%,#24357F 0%,#16215A 48%,#0E1738 100%);"></div>
  <div style="position:absolute;inset:0;background:rgba(6,10,24,0.62);"></div>
  <div style="position:absolute;left:0;right:0;bottom:0;top:76px;box-sizing:border-box;display:flex;flex-direction:column;background:#F5F7FB;border-radius:28px 28px 0 0;overflow:hidden;">
    <div style="display:flex;align-items:center;gap:12px;padding:20px 20px 14px;flex-shrink:0;">
      <h2 class="d" style="flex-grow:1;margin:0;font-size:21px;font-weight:700;letter-spacing:-0.4px;color:#1E2230;">Privacidad</h2>
      <div style="display:flex;align-items:center;justify-content:center;width:34px;height:34px;border-radius:999px;background:#FFFFFF;border:1px solid #E3E8F2;flex-shrink:0;">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true"><path d="M6.5 6.5l11 11m0-11l-11 11" stroke="#3F4553" stroke-width="2.2" stroke-linecap="round"></path></svg>
      </div>
    </div>
    <div style="position:relative;flex-grow:1;overflow:hidden;padding:0 20px;">
      <div style="margin-top:{desplazamiento}px;">{CUERPO}</div>
      <div style="position:absolute;right:7px;top:{'55' if al_final else '6'}%;width:3px;height:38%;border-radius:999px;background:#C9D3E6;"></div>
    </div>
    <div style="box-sizing:border-box;padding:16px 20px 26px;background:#FFFFFF;border-top:1px solid #E3E8F2;flex-shrink:0;">
{pie}
    </div>
  </div>'''
    escribir(nombre, contenido, envoltura=suelto)


modal("TerminosLeyendo.dc.html", al_final=False)
modal("TerminosFinal.dc.html", al_final=True)
print("listo")
