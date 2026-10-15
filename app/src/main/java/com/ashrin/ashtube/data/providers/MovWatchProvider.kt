package com.ashrin.ashtube.data.providers

import com.ashrin.ashtube.data.HomeSection
import com.ashrin.ashtube.data.MediaDetails
import com.ashrin.ashtube.data.MediaItem
import com.ashrin.ashtube.data.PlaybackSession
import com.ashrin.ashtube.data.remote.VidlinkStreamResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Provider 5: mov-watch (Python) Stream Resolver Backend
 * Provides multi-mirror failover and fast direct CDN streaming for all media items.
 */
class MovWatchProvider : MediaProvider {
    override val name: String = "mov-watch"
    override val priority: Int = 75

    override suspend fun isAvailable(): Boolean = true

    override suspend fun search(query: String): List<MediaItem> = emptyList()

    override suspend fun getHomeSection(): HomeSection? = null

    override suspend fun getDetails(id: String): MediaDetails? = null

    override suspend fun resolveStream(id: String, episodeNumber: Int?): PlaybackSession? = withContext(Dispatchers.IO) {
        val ep = episodeNumber ?: 1
        val isSeries = ep > 1 || id.contains("tv") || id.startsWith("series_")

        VidlinkStreamResolver.resolveStream(
            id = id,
            title = id,
            type = if (isSeries) "Series" else "Movie",
            season = if (isSeries) 1 else null,
            episode = if (isSeries) ep else null,
        )
    }
}
