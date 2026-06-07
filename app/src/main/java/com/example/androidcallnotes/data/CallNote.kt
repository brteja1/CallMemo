package com.example.androidcallnotes.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "call_notes",
    indices = [Index(value = ["phoneNumber"])]
)
data class CallNote(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val phoneNumber: String,
    val timestamp: Long,
    val noteText: String,
)

