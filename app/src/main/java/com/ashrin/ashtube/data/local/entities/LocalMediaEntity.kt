package com.ashrin.ashtube.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a user-selected local video file indexed via Android's Storage Access Framework (SAF).
 * AshTube never scans storage arbitrarily; only files explicitly chosen by the user are indexed.
 */
@Entity(tableName = "local_media")
data class LocalMediaEntity(
    @PrimaryKey
    val uri: String,
    val displayName: String,
    val mimeType: String = "video/*",
    val sizeBytes: Long = 0L,
    val durationMs: Long = 0L,
    val resolution: String? = null,
    val lastPlayedPositionMs: Long = 0L,
    val addedAt: Long = System.currentTimeMillis(),
)
