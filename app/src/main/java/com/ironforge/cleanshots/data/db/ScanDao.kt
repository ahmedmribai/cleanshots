package com.ironforge.cleanshots.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface ScanDao {
    @Upsert
    suspend fun upsertAll(items: List<ScannedImage>)

    @Query("SELECT mediaId, dateModified FROM media_scan")
    suspend fun getStamps(): List<Stamp>

    @Query("SELECT * FROM media_scan WHERE dhash IS NOT NULL ORDER BY dateModified DESC")
    suspend fun getAllHashed(): List<ScannedImage>

    @Query("DELETE FROM media_scan WHERE mediaId IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    data class Stamp(val mediaId: Long, val dateModified: Long)
}
