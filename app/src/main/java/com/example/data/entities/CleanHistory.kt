package com.example.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clean_history")
data class CleanHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val cleanedSize: Long,
    val filesCount: Int,
    val isAutoClean: Boolean = false,
    val isRootClean: Boolean = false
)
