package com.example.androidcallnotes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [CallNote::class],
    version = 1,
    exportSchema = false,
)
abstract class CallNotesDatabase : RoomDatabase() {
    abstract fun callNoteDao(): CallNoteDao

    companion object {
        @Volatile
        private var INSTANCE: CallNotesDatabase? = null

        fun getInstance(context: Context): CallNotesDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CallNotesDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }

        private const val DATABASE_NAME = "call_notes.db"
    }
}

