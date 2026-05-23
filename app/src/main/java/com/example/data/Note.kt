package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val tagColorHex: String = "#FF7043", // Default orange/peach tone
    val category: String = "Estudio",
    val timestamp: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false
)
