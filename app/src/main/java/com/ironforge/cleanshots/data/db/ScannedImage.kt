package com.ironforge.cleanshots.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ironforge.cleanshots.data.Cleanable

@Entity(tableName = "media_scan")
data class ScannedImage(
    @PrimaryKey override val mediaId: Long,
    override val contentUri: String,
    override val displayName: String,
    override val size: Long,
    val dateAdded: Long,
    val dateModified: Long,
    val width: Int,
    val height: Int,
    val bucket: String,
    val relativePath: String,
    val dhash: Long? = null,
    val blurScore: Float? = null,
    val scannedAt: Long = System.currentTimeMillis()
) : Cleanable
