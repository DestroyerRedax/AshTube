package com.ashrin.ashtube.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val mediaId: String,
    val title: String,
    val type: String,
    val year: String,
    val posterUrl: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
)
