package com.ironforge.cleanshots.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ScannedImage::class, TrashEntry::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scanDao(): ScanDao
    abstract fun trashDao(): TrashDao
}
