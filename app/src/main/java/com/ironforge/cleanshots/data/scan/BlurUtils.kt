package com.ironforge.cleanshots.data.scan

import android.graphics.Bitmap

object BlurUtils {
    const val DEFAULT_THRESHOLD = 100.0

    /** Score used for images that cannot be judged (too small, too dark or too flat); never below any threshold. */
    const val UNMEASURABLE = 1_000_000.0

    private const val MIN_MEAN_LUMA = 35.0
    private const val MIN_LUMA_STDDEV = 18.0

    fun laplacianVariance(src: Bitmap): Double {
        val w = src.width
        val h = src.height
        if (w < 3 || h < 3) return UNMEASURABLE
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)
        val gray = IntArray(w * h) { i ->
            val c = pixels[i]
            ((77 * ((c shr 16) and 255) + 150 * ((c shr 8) and 255) + 29 * (c and 255)) shr 8)
        }
        return laplacianVariance(gray, w, h)
    }

    /**
     * Variance of the Laplacian over an 8-bit luma plane. Night shots and flat scenes have little
     * edge energy even when in focus, so they are reported as [UNMEASURABLE] instead of blurry.
     */
    fun laplacianVariance(gray: IntArray, w: Int, h: Int): Double {
        if (w < 3 || h < 3) return UNMEASURABLE
        require(gray.size >= w * h) { "gray plane smaller than ${w}x$h" }
        if (!hasUsableContrast(gray, w * h)) return UNMEASURABLE
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
        return if (n == 0L) UNMEASURABLE else m2 / n
    }

    private fun hasUsableContrast(gray: IntArray, count: Int): Boolean {
        var mean = 0.0
        var m2 = 0.0
        for (i in 0 until count) {
            val delta = gray[i] - mean
            mean += delta / (i + 1)
            m2 += delta * (gray[i] - mean)
        }
        return mean >= MIN_MEAN_LUMA && Math.sqrt(m2 / count) >= MIN_LUMA_STDDEV
    }

    fun isBlurry(variance: Double, threshold: Double = DEFAULT_THRESHOLD): Boolean = variance < threshold
}
