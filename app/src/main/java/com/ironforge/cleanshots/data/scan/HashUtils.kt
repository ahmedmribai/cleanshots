package com.ironforge.cleanshots.data.scan

import android.graphics.Bitmap

object HashUtils {
    fun dHash(src: Bitmap): Long {
        val tiny = Bitmap.createScaledBitmap(src, 9, 8, true)
        val pixels = IntArray(9 * 8)
        tiny.getPixels(pixels, 0, 9, 0, 0, 9, 8)
        if (tiny !== src) tiny.recycle()
        var hash = 0L
        for (y in 0 until 8) {
            for (x in 0 until 8) {
                val l = pixels[y * 9 + x]
                val r = pixels[y * 9 + x + 1]
                val gl = luma(l)
                val gr = luma(r)
                hash = (hash shl 1) or (if (gl < gr) 1L else 0L)
            }
        }
        return hash
    }

    private fun luma(c: Int): Int {
        val r = (c shr 16) and 255
        val g = (c shr 8) and 255
        val b = c and 255
        return (77 * r + 150 * g + 29 * b) shr 8
    }

    fun hamming(a: Long, b: Long): Int = java.lang.Long.bitCount(a xor b)

    class DSU(n: Int) {
        private val p = IntArray(n) { it }
        private val r = IntArray(n)
        fun find(x: Int): Int {
            var v = x
            while (p[v] != v) { p[v] = p[p[v]]; v = p[v] }
            return v
        }
        fun union(a: Int, b: Int) {
            var x = find(a); var y = find(b)
            if (x == y) return
            if (r[x] < r[y]) { val t = x; x = y; y = t }
            p[y] = x
            if (r[x] == r[y]) r[x]++
        }
    }

    class BKTree {
        private class Node(val hash: Long, idx: Int) {
            val idxs = mutableListOf(idx)
            val children = HashMap<Int, Node>()
        }
        private var root: Node? = null

        fun add(hash: Long, idx: Int) {
            var cur = root ?: run { root = Node(hash, idx); return }
            while (true) {
                val d = hamming(hash, cur.hash)
                if (d == 0) { cur.idxs.add(idx); return }
                val next = cur.children[d]
                if (next == null) { cur.children[d] = Node(hash, idx); return }
                cur = next
            }
        }

        fun query(hash: Long, maxDist: Int): List<Int> {
            val out = mutableListOf<Int>()
            val stack = ArrayDeque<Node>()
            root?.let(stack::addLast)
            while (stack.isNotEmpty()) {
                val n = stack.removeLast()
                val d = hamming(hash, n.hash)
                if (d <= maxDist) out.addAll(n.idxs)
                for ((dist, child) in n.children) {
                    if (dist in (d - maxDist)..(d + maxDist)) stack.addLast(child)
                }
            }
            return out
        }
    }

    /** Hashes with almost no set (or unset) bits come from flat images and match each other by accident. */
    private const val MIN_INFORMATIVE_BITS = 6

    private fun isInformative(hash: Long): Boolean {
        val bits = java.lang.Long.bitCount(hash)
        return bits in MIN_INFORMATIVE_BITS..(64 - MIN_INFORMATIVE_BITS)
    }

    /**
     * Leader clustering: an image joins the nearest earlier group leader within [threshold],
     * otherwise it starts its own group. Unlike transitive union-find this cannot chain
     * unrelated images together through intermediate neighbours.
     */
    /** Default distance from tools/benchmark: 95 % of exact-copy sets found, ~2 % lookalikes suggested. */
    const val DUPLICATE_DISTANCE = 4

    fun groupNearDuplicates(hashes: List<Long>, threshold: Int = DUPLICATE_DISTANCE): List<List<Int>> {
        val leaders = BKTree()
        val members = HashMap<Int, MutableList<Int>>()
        hashes.forEachIndexed { i, h ->
            if (!isInformative(h)) return@forEachIndexed
            val leader = leaders.query(h, threshold).minByOrNull { hamming(h, hashes[it]) }
            if (leader == null) {
                leaders.add(h, i)
                members[i] = mutableListOf(i)
            } else {
                members.getValue(leader).add(i)
            }
        }
        return members.values.filter { it.size > 1 }
    }
}
