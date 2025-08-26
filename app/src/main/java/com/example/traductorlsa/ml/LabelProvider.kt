
package com.example.traductorlsa.ml

import android.content.Context
import android.util.Log
import org.json.JSONObject

interface LabelProvider {
    val labels: List<String>
}

class LabelProviderImpl(private val context: Context) : LabelProvider {
    override val labels: List<String> by lazy {
        try {
            val jsonStr = context.assets.open("words.json").bufferedReader().use { it.readText() }
            val arr = JSONObject(jsonStr).getJSONArray("word_ids")
            MutableList(arr.length()) { i ->
                val raw = arr.getString(i)
                raw.split("-")[0]
            }
        } catch (t: Throwable) {
            Log.e("LabelProvider", "No se pudo leer words.json: ${t.message}")
            listOf("hola","adios","bien","como_estas","gracias")
        }
    }
}
