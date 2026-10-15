package com.ashrin.ashtube.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey
    val id: String,
    val mediaId: String,
    val title: String,
    val quality: String,
    val progress: Float,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val state: String,
    val localFilePath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
