package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.CleanHistoryDao
import com.example.data.dao.QuarantinedFileDao
import com.example.data.entities.CleanHistory
import com.example.data.entities.QuarantinedFile

@Database(
    entities = [QuarantinedFile::class, CleanHistory::class],
    version = 1,
    exportSchema = false
)
abstract class NexaPurgeDatabase : RoomDatabase() {
    abstract fun quarantinedFileDao(): QuarantinedFileDao
    abstract fun cleanHistoryDao(): CleanHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: NexaPurgeDatabase? = null

        fun getDatabase(context: Context): NexaPurgeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NexaPurgeDatabase::class.java,
                    "nexapurge_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
