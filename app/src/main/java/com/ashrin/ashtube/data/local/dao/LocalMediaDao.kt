package com.ashrin.ashtube.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ashrin.ashtube.data.local.entities.LocalMediaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalMediaDao {
    @Query("SELECT * FROM local_media ORDER BY addedAt DESC")
    fun getAllLocalMedia(): Flow<List<LocalMediaEntity>>

    @Query("SELECT * FROM local_media ORDER BY addedAt DESC")
    suspend fun getAllLocalMediaList(): List<LocalMediaEntity>

    @Query("SELECT * FROM local_media WHERE uri = :uri LIMIT 1")
    suspend fun getByUri(uri: String): LocalMediaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LocalMediaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<LocalMediaEntity>)

    @Query("UPDATE local_media SET lastPlayedPositionMs = :positionMs WHERE uri = :uri")
    suspend fun updateProgress(uri: String, positionMs: Long)

    @Query("DELETE FROM local_media WHERE uri = :uri")
    suspend fun deleteByUri(uri: String)

    @Query("DELETE FROM local_media")
    suspend fun clearAll()
}
