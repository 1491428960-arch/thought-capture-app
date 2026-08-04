package com.thoughtcapture.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.thoughtcapture.app.data.dao.ThoughtDao
import com.thoughtcapture.app.data.entity.ThoughtEntry

@Database(entities = [ThoughtEntry::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun thoughtDao(): ThoughtDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "thought_capture.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
