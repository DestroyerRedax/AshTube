package com.ashrin.ashtube.data.providers

import com.ashrin.ashtube.data.HomeSection
import com.ashrin.ashtube.data.MediaDetails
import com.ashrin.ashtube.data.MediaItem
import com.ashrin.ashtube.data.PlaybackSession
import com.ashrin.ashtube.data.remote.MovieBoxRemoteService

/**
 * Provider 1: MovieBox-TUI (Rust / Android backend)
 * Specializes in global cinema, Bollywood/regional releases, and trending web series.
 */
class MovieBoxProvider : MediaProvider {
    override val name: String = "MovieBox"
    override val priority: Int = 100

    override suspend fun isAvailable(): Boolean = true

    override suspend fun search(query: String): List<MediaItem> {
        return try {
            MovieBoxRemoteService.search(query).map {
                it.copy(provider = name)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun getHomeSection(): HomeSection? {
        return try {
            val sections = MovieBoxRemoteService.getHomepageSections()
            sections.firstOrNull()?.let {
                it.copy(
                    title = "Trending on MovieBox",
                    items = it.items.map { item -> item.copy(provider = name) },
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun getDetails(id: String): MediaDetails? {
        return try {
            val details = MovieBoxRemoteService.getDetails(id)
            details?.copy(
                item = details.item.copy(provider = name),
            )
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun resolveStream(id: String, episodeNumber: Int?): PlaybackSession? {
        return try {
            MovieBoxRemoteService.getPlaybackSession(id, episodeNumber)
        } catch (_: Exception) {
            null
        }
    }
}
