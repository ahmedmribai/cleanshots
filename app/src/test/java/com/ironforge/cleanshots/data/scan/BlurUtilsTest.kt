package com.ironforge.cleanshots.data.scan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlurUtilsTest {
    private val size = 64

    private fun plane(f: (x: Int, y: Int) -> Int) = IntArray(size * size) { f(it % size, it / size) }

    private fun score(gray: IntArray) = BlurUtils.laplacianVariance(gray, size, size)

    @Test
    fun sharpCheckerboardIsNotBlurry() {
        val gray = plane { x, y -> if ((x / 8 + y / 8) % 2 == 0) 40 else 220 }
        assertFalse(BlurUtils.isBlurry(score(gray)))
    }

    @Test
    fun smoothGradientWithGoodContrastIsBlurry() {
        val gray = plane { x, _ -> 40 + x * 3 }
        assertTrue(BlurUtils.isBlurry(score(gray)))
    }

    @Test
    fun blurringASharpImageLowersItsScore() {
        val sharp = plane { x, y -> if ((x / 8 + y / 8) % 2 == 0) 40 else 220 }
        val blurred = IntArray(size * size) { i ->
            val x = i % size
            val y = i / size
            var sum = 0
            for (dy in -2..2) for (dx in -2..2) {
                sum += sharp[(y + dy).coerceIn(0, size - 1) * size + (x + dx).coerceIn(0, size - 1)]
            }
            sum / 25
        }
        assertTrue(score(blurred) < score(sharp))
    }

    @Test
    fun darkImageIsUnmeasurableNotBlurry() {
        val gray = plane { x, _ -> 5 + x / 8 }
        assertEquals(BlurUtils.UNMEASURABLE, score(gray), 0.0)
        assertFalse(BlurUtils.isBlurry(score(gray)))
    }

    @Test
    fun flatImageIsUnmeasurableNotBlurry() {
        assertEquals(BlurUtils.UNMEASURABLE, score(plane { _, _ -> 128 }), 0.0)
    }

    @Test
    fun tooSmallImageIsUnmeasurable() {
        assertEquals(BlurUtils.UNMEASURABLE, BlurUtils.laplacianVariance(IntArray(4), 2, 2), 0.0)
    }
}
