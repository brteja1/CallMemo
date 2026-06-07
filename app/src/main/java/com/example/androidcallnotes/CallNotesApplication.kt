package com.example.androidcallnotes

import android.app.Application
import com.example.androidcallnotes.data.CallNoteRepository
import com.example.androidcallnotes.data.CallNotesDatabase

class CallNotesApplication : Application() {
    val database: CallNotesDatabase by lazy {
        CallNotesDatabase.getInstance(this)
    }

    val repository: CallNoteRepository by lazy {
        CallNoteRepository(database.callNoteDao())
    }
}

