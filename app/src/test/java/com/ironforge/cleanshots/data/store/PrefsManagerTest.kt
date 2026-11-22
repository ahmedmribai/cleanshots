package com.ironforge.cleanshots.data.store

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrefsManagerTest {
    @Test
    fun premiumUsersAreNeverBlockedByFreeDeleteLimit() {
        assertFalse(PrefsManager.exceedsFreeLimit(isPremium = true, used = 20, adding = 1))
        assertFalse(PrefsManager.exceedsFreeLimit(isPremium = true, used = 500, adding = 100))
    }

    @Test
    fun freeUsersCanReachButNotExceedLimit() {
        assertFalse(PrefsManager.exceedsFreeLimit(isPremium = false, used = 19, adding = 1))
        assertTrue(PrefsManager.exceedsFreeLimit(isPremium = false, used = 20, adding = 1))
    }

    @Test
    fun zeroAdditionalDeletesDoNotExceedLimit() {
        assertFalse(PrefsManager.exceedsFreeLimit(isPremium = false, used = 20, adding = 0))
    }
}
