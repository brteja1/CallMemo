package com.example.androidcallnotes.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class CallNoteRepository(
    private val dao: CallNoteDao,
) {
    fun getNotesForNumber(phoneNumber: String): Flow<List<CallNote>> {
        return dao.getNotesForNumber(phoneNumber)
    }

    suspend fun insertNote(note: CallNote) = withContext(Dispatchers.IO) {
        dao.insertNote(note)
    }

    suspend fun deleteNote(note: CallNote) = withContext(Dispatchers.IO) {
        dao.deleteNote(note)
    }
}

