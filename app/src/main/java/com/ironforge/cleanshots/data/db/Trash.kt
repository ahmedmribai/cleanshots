package com.ironforge.cleanshots.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert

@Entity(tableName = "trash")
data class TrashEntry(
    @PrimaryKey val mediaId: Long,
    val contentUri: String,
    val displayName: String,
    val size: Long,
    val trashedAt: Long = System.currentTimeMillis()
)

@Dao
interface TrashDao {
    @Upsert
    suspend fun upsert(entry: TrashEntry)

    @Upsert
    suspend fun upsertAll(entries: List<TrashEntry>)

    @Query("SELECT * FROM trash ORDER BY trashedAt DESC")
    suspend fun getAll(): List<TrashEntry>

    @Query("DELETE FROM trash WHERE mediaId IN (:ids)")
    suspend fun remove(ids: List<Long>)

    @Query("DELETE FROM trash WHERE trashedAt < :olderThan")
    suspend fun pruneOlderThan(olderThan: Long)
}
