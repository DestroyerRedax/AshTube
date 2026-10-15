package com.ashrin.ashtube.data.providers

import com.ashrin.ashtube.data.HomeSection
import com.ashrin.ashtube.data.MediaDetails
import com.ashrin.ashtube.data.MediaItem
import com.ashrin.ashtube.data.PlaybackSession

/**
 * Universal interface for streaming & scraper providers:
 * 1. MovieBox-TUI
 * 2. mov-cli (FlixHQ / Films)
 * 3. mov-watch (Vidsrc / Embeds)
 * 4. freeflix-cli (Multi-catalog)
 * 5. kunai (Anime TUI)
 * 6. ani-cli (AllAnime / Hianime)
 */
interface MediaProvider {
    val name: String
    val priority: Int

    suspend fun isAvailable(): Boolean
    suspend fun search(query: String): List<MediaItem>
    suspend fun getHomeSection(): HomeSection?
    suspend fun getDetails(id: String): MediaDetails?
    suspend fun resolveStream(id: String, episodeNumber: Int? = null): PlaybackSession?
}
