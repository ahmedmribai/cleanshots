package com.ironforge.cleanshots.data.store

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.ds by preferencesDataStore("cleanshots_prefs")

class PrefsManager(private val context: Context) {
    private object Keys {
        val PREMIUM = booleanPreferencesKey("premium")
        val FREE_DELETE_COUNT = intPreferencesKey("free_delete_count")
    }

    val premium: Flow<Boolean> = context.ds.data.map { it[Keys.PREMIUM] ?: false }
    val freeDeleteCount: Flow<Int> = context.ds.data.map { it[Keys.FREE_DELETE_COUNT] ?: 0 }

    suspend fun setPremium(v: Boolean) { context.ds.edit { it[Keys.PREMIUM] = v } }
    suspend fun incrementDeletes(by: Int) {
        context.ds.edit { it[Keys.FREE_DELETE_COUNT] = ((it[Keys.FREE_DELETE_COUNT] ?: 0) + by).coerceAtLeast(0) }
    }

    companion object {
        const val FREE_LIMIT = 20

        fun exceedsFreeLimit(isPremium: Boolean, used: Int, adding: Int): Boolean =
            !isPremium && used + adding > FREE_LIMIT
    }
}
