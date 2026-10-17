package com.ironforge.cleanshots.data.store

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FreeLimitTest {
    private val limit = PrefsManager.FREE_LIMIT

    @Test
    fun deletingUpToTheLimitIsAllowed() {
        assertFalse(PrefsManager.exceedsFreeLimit(isPremium = false, used = 0, adding = limit))
        assertFalse(PrefsManager.exceedsFreeLimit(isPremium = false, used = limit - 1, adding = 1))
    }

    @Test
    fun goingOverTheLimitIsBlocked() {
        assertTrue(PrefsManager.exceedsFreeLimit(isPremium = false, used = limit, adding = 1))
        assertTrue(PrefsManager.exceedsFreeLimit(isPremium = false, used = 0, adding = limit + 1))
    }

    @Test
    fun premiumIsNeverBlocked() {
        assertFalse(PrefsManager.exceedsFreeLimit(isPremium = true, used = limit * 10, adding = 500))
    }
}
