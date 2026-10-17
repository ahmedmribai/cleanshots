package com.ironforge.cleanshots.data.scan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HashUtilsTest {
    private val a = 0x0F0F_F0F0_3C3C_C3C3L

    @Test
    fun hammingCountsDifferingBits() {
        assertEquals(0, HashUtils.hamming(a, a))
        assertEquals(3, HashUtils.hamming(a, a xor 0b10101L))
    }

    @Test
    fun bkTreeReturnsEveryIndexWithinDistanceIncludingExactDuplicates() {
        val tree = HashUtils.BKTree()
        tree.add(a, 0)
        tree.add(a, 1)
        tree.add(a xor 0b11L, 2)
        tree.add(a.inv(), 3)
        assertEquals(setOf(0, 1, 2), tree.query(a, 5).toSet())
        assertEquals(setOf(0, 1), tree.query(a, 0).toSet())
    }

    @Test
    fun groupsNearDuplicatesAndIgnoresUnrelatedImages() {
        val hashes = listOf(a, a xor 0b1L, 0x5555_AAAA_5555_AAAAL, a)
        val groups = HashUtils.groupNearDuplicates(hashes, 5)
        assertEquals(1, groups.size)
        assertEquals(setOf(0, 1, 3), groups.single().toSet())
    }

    @Test
    fun doesNotChainThroughIntermediateNeighbours() {
        val far = a xor 0b1111_1111L // 8 bits from a
        val mid = a xor 0b0001_1111L // 5 bits from a, 3 bits from far
        val groups = HashUtils.groupNearDuplicates(listOf(a, mid, far), 5)
        // far is within 5 of mid but not of the leader a, so it must not be pulled into a's group.
        assertEquals(listOf(setOf(0, 1)), groups.map { it.toSet() })
    }

    @Test
    fun flatImageHashesAreNeverGrouped() {
        assertTrue(HashUtils.groupNearDuplicates(listOf(0L, 0L, -1L, -1L), 5).isEmpty())
    }

    @Test
    fun longChainDoesNotOverflowTheStack() {
        val hashes = List(20_000) { i -> (i.toLong() * 0x9E3779B97F4A7C15uL.toLong()) }
        HashUtils.groupNearDuplicates(hashes, 5)
    }
}
