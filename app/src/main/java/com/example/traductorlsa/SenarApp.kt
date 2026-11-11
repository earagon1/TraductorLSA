package com.example.traductorlsa

import android.app.Application
import android.content.pm.ApplicationInfo
import com.example.traductorlsa.ml.Metricas
import java.io.File

class SenarApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Las métricas del pipeline (logcat SENAR_METRICS) solo en builds
        // depurables: en release no se mide ni se escribe nada.
        Metricas.activas = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (Metricas.activas) Metricas.archivo = File(filesDir, "metricas/senar_metrics.jsonl")

        // Clerk (la cuenta) solo si la persona eligió usarla: traducir no conecta a nada.
        Cuenta.inicializarSiCorresponde(this)
    }
}
