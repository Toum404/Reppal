package com.reppal.app

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Rappel::class], version = 1, exportSchema = false)
abstract class RappelDatabase : RoomDatabase() {
    abstract fun rappelDao(): RappelDao

    companion object {
        @Volatile
        private var INSTANCE: RappelDatabase? = null

        fun getDatabase(context: Context): RappelDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RappelDatabase::class.java,
                    "rappel_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}