package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import com.example.data.entities.QuarantinedFile
import kotlinx.coroutines.flow.Flow

@Dao
interface QuarantinedFileDao {
    @Query("SELECT * FROM quarantined_files ORDER BY timestamp DESC")
    fun getAllQuarantinedFiles(): Flow<List<QuarantinedFile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: QuarantinedFile): Long

    @Delete
    suspend fun deleteFile(file: QuarantinedFile)

    @Query("SELECT * FROM quarantined_files WHERE id = :id LIMIT 1")
    suspend fun getFileById(id: Int): QuarantinedFile?

    @Query("DELETE FROM quarantined_files")
    suspend fun deleteAll()
}
