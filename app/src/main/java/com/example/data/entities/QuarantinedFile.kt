package com.example.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quarantined_files")
data class QuarantinedFile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val filename: String,
    val originalPath: String,
    val tempTrashPath: String,
    val size: Long,
    val timestamp: Long = System.currentTimeMillis()
)
