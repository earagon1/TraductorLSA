package com.example.traductorlsa

import android.app.Application
import android.content.pm.ApplicationInfo
import com.clerk.api.Clerk
import com.example.traductorlsa.ml.Metricas
import java.io.File

class SenarApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Las métricas del pipeline (logcat SENAR_METRICS) solo en builds
        // depurables: en release no se mide ni se escribe nada.
        Metricas.activas = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (Metricas.activas) Metricas.archivo = File(filesDir, "metricas/senar_metrics.jsonl")

        Clerk.initialize(
            context= this,
            publishableKey = "pk_test_am9pbnQtZ2VsZGluZy0xMi5jbGVyay5hY2NvdW50cy5kZXYk"
        )
    }
}
