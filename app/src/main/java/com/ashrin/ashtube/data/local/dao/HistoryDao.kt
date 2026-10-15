package com.ashrin.ashtube.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ashrin.ashtube.data.local.entities.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY lastWatchedAt DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history ORDER BY lastWatchedAt DESC")
    suspend fun getAllHistoryList(): List<HistoryEntity>

    @Query("SELECT positionMs FROM history WHERE mediaId = :mediaId LIMIT 1")
    suspend fun getPosition(mediaId: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: HistoryEntity)

    @Query("DELETE FROM history WHERE mediaId = :mediaId")
    suspend fun deleteById(mediaId: String)

    @Query("DELETE FROM history")
    suspend fun clearAll()
}
