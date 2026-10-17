package com.ironforge.cleanshots.data.scan

import android.graphics.Bitmap

object BlurUtils {
    const val DEFAULT_THRESHOLD = 100.0

    fun laplacianVariance(src: Bitmap): Double {
        val w = src.width
        val h = src.height
        if (w < 3 || h < 3) return Double.MAX_VALUE
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)
        val gray = IntArray(w * h) { i ->
            val c = pixels[i]
            ((77 * ((c shr 16) and 255) + 150 * ((c shr 8) and 255) + 29 * (c and 255)) shr 8)
        }
        var mean = 0.0
        var m2 = 0.0
        var n = 0L
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val c = gray[y * w + x]
                val lap = 4 * c - gray[y * w + x - 1] - gray[y * w + x + 1] -
                        gray[(y - 1) * w + x] - gray[(y + 1) * w + x]
                n++
                val delta = lap - mean
                mean += delta / n
                m2 += delta * (lap - mean)
            }
        }
        return if (n == 0L) Double.MAX_VALUE else m2 / n
    }

    fun isBlurry(variance: Double, threshold: Double = DEFAULT_THRESHOLD): Boolean = variance < threshold
}
