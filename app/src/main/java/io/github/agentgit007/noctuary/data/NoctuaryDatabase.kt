/*
 * Noctuary — a dream journal for Android.
 * Copyright (C) 2026 the Noctuary contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package io.github.agentgit007.noctuary.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Dream::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class NoctuaryDatabase : RoomDatabase() {

    abstract fun dreamDao(): DreamDao

    companion object {
        @Volatile
        private var instance: NoctuaryDatabase? = null

        fun get(context: Context): NoctuaryDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    NoctuaryDatabase::class.java,
                    "noctuary.db"
                ).build().also { instance = it }
            }
    }
}
