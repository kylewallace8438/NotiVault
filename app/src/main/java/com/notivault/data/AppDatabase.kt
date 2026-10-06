package com.notivault.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Stored on the phone at /data/data/com.notivault/databases/notivault.db (private to this app).
 *
 * Adding columns later (e.g. "category"): add the field to NotificationEntity, bump `version`,
 * and add `autoMigrations = [AutoMigration(from = 1, to = 2)]` here. Do NOT use
 * fallbackToDestructiveMigration(): it would wipe your history.
 */
@Database(entities = [NotificationEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {

    abstract fun notificationDao(): NotificationDao

    /** Flushes the write-ahead log into the main .db file so a copied file is complete. Call off the main thread. */
    fun checkpoint() {
        openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
    }

    companion object {
        const val NAME = "notivault.db"

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, NAME).build()
    }
}
