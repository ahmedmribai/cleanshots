package com.ironforge.cleanshots.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_scan")
data class ScannedImage(
    @PrimaryKey val mediaId: Long,
    val contentUri: String,
    val displayName: String,
    val size: Long,
    val dateAdded: Long,
    val dateModified: Long,
    val width: Int,
    val height: Int,
    val bucket: String,
    val relativePath: String,
    val dhash: Long? = null,
    val blurScore: Float? = null,
    val scannedAt: Long = System.currentTimeMillis()
)
