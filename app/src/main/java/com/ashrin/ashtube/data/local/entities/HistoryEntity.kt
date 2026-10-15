package com.ashrin.ashtube.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey
    val mediaId: String,
    val title: String,
    val type: String,
    val year: String,
    val positionMs: Long = 0L,
    val durationMs: Long = 7200000L,
    val lastWatchedAt: Long = System.currentTimeMillis(),
)
