
package com.example.traductorlsa.features

interface SequenceBuffer {
    fun push(frame: FloatArray)
    fun recent(n: Int): List<FloatArray>
    fun clear()
    fun normalizeTo(seq: List<FloatArray>, targetT: Int, D: Int): List<FloatArray>
}

class SequenceBufferImpl : SequenceBuffer {
    private val manual = mutableListOf<FloatArray>()

    override fun push(frame: FloatArray) {
        manual.add(frame)
        if (manual.size > 30) manual.removeAt(0)
    }

    override fun recent(n: Int): List<FloatArray> = manual.takeLast(n)

    override fun clear() { manual.clear() }

    override fun normalizeTo(seq: List<FloatArray>, targetT: Int, D: Int): List<FloatArray> {
        val current = seq.size
        if (current == targetT) return seq

        val out = ArrayList<FloatArray>(targetT)
        return if (current < targetT) {
            val idxs = linspace(0.0, (current - 1).toDouble(), targetT)
            idxs.forEach { v ->
                val li = kotlin.math.floor(v).toInt()
                val ui = kotlin.math.ceil(v).toInt()
                val w = (v - li).toFloat()
                if (li == ui) out.add(seq[li].clone()) else {
                    val lower = seq[li]; val upper = seq[ui]
                    val dst = FloatArray(D)
                    for (k in 0 until D) dst[k] = (1 - w) * lower[k] + w * upper[k]
                    out.add(dst)
                }
            }
            out
        } else {
            val step = current.toDouble() / targetT.toDouble()
            var i = 0.0
            while (out.size < targetT) {
                out.add(seq[i.toInt()].clone())
                i += step
            }
            out
        }
    }

    private fun linspace(start: Double, end: Double, num: Int): DoubleArray {
        if (num == 1) return doubleArrayOf(start)
        val step = (end - start) / (num - 1)
        return DoubleArray(num) { i -> start + step * i }
    }
}
