package com.example.androidcallnotes.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CallNoteDao {
    @Insert
    suspend fun insertNote(note: CallNote): Long

    @Query(
        """
        SELECT * FROM call_notes
        WHERE phoneNumber = :phoneNumber
        ORDER BY timestamp DESC
        LIMIT 3
        """
    )
    fun getNotesForNumber(phoneNumber: String): Flow<List<CallNote>>

    @Query("SELECT * FROM call_notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<CallNote>>

    @Delete
    suspend fun deleteNote(note: CallNote)
}

