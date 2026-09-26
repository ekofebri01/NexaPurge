package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entities.CleanHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface CleanHistoryDao {
    @Query("SELECT * FROM clean_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<CleanHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: CleanHistory): Long

    @Query("DELETE FROM clean_history")
    suspend fun deleteAllHistory()
}
