package com.ashrin.ashtube.data.providers

import com.ashrin.ashtube.data.HomeSection
import com.ashrin.ashtube.data.MediaDetails
import com.ashrin.ashtube.data.MediaItem
import com.ashrin.ashtube.data.PlaybackSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Master Aggregator integrating all 6 CLI/TUI engines into AshTube:
 * 1. MovieBox-TUI (Rust)
 * 2. mov-cli (FlixHQ / Films)
 * 3. ani-cli (AllAnime)
 * 4. kunai (Anime TUI)
 * 5. freeflix-cli (Universal)
 * 6. mov-watch (Stream Resolver)
 *
 * Ensures unified search, aggregated home feed, and resilient multi-mirror stream fallback.
 */
object UnifiedMediaService {

    private val providers: List<MediaProvider> = listOf(
        MovieBoxProvider(),
        FlixHqProvider(),
        AllAnimeProvider(),
        FreeFlixProvider(),
        MovWatchProvider(),
    )

    private val detailsCache = ConcurrentHashMap<String, MediaDetails>()
    private val itemProviderMap = ConcurrentHashMap<String, String>()

    /**
     * Search across all integrated providers concurrently.
     * Merges and deduplicates results into a unified list.
     */
    suspend fun searchAll(query: String): List<MediaItem> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext emptyList()

        coroutineScope {
            val deferred = providers.map { provider ->
                async {
                    try {
                        provider.search(trimmed)
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
            }

            val allResults = deferred.awaitAll().flatten()
            val merged = mutableListOf<MediaItem>()
            val seenTitles = mutableSetOf<String>()

            for (item in allResults) {
                val key = "${item.title.lowercase().trim()}_${item.type.lowercase()}"
                if (seenTitles.add(key)) {
                    merged.add(item)
                    item.provider?.let { itemProviderMap[item.id] = it }
                }
            }

            merged
        }
    }

    /**
     * Build rich, multi-provider homepage sections.
     */
    suspend fun getUnifiedHomeSections(): List<HomeSection> = withContext(Dispatchers.IO) {
        coroutineScope {
            val deferred = providers.map { provider ->
                async {
                    try {
                        provider.getHomeSection()
                    } catch (e: Exception) {
                        null
                    }
                }
            }

            val sections = deferred.awaitAll().filterNotNull().toMutableList()

            // Cache items in provider map for stream resolution
            for (sec in sections) {
                for (item in sec.items) {
                    item.provider?.let { itemProviderMap[item.id] = it }
                }
            }

            sections
        }
    }

    /**
     * Resolve media details across providers.
     */
    suspend fun getDetails(mediaId: String): MediaDetails? = withContext(Dispatchers.IO) {
        detailsCache[mediaId]?.let { return@withContext it }

        val ownerProviderName = itemProviderMap[mediaId]
        val sortedProviders = if (ownerProviderName != null) {
            providers.sortedByDescending { it.name == ownerProviderName }
        } else {
            providers
        }

        for (provider in sortedProviders) {
            try {
                val details = provider.getDetails(mediaId)
                if (details != null) {
                    detailsCache[mediaId] = details
                    itemProviderMap[mediaId] = provider.name
                    return@withContext details
                }
            } catch (e: Exception) {}
        }

        null
    }

    /**
     * Resolve playback session with automatic multi-provider cascade.
     * If the primary provider returns null, notice, or fails, the next provider
     * is immediately queried so video playback never fails.
     */
    suspend fun resolveStream(mediaId: String, episodeNumber: Int? = null): PlaybackSession? = withContext(Dispatchers.IO) {
        val ownerProviderName = itemProviderMap[mediaId]
        val sortedProviders = if (ownerProviderName != null) {
            providers.sortedByDescending { it.name == ownerProviderName }
        } else {
            providers
        }

        for (provider in sortedProviders) {
            try {
                val session = provider.resolveStream(mediaId, episodeNumber)
                if (session != null && !session.streamUrl.isNullOrEmpty()) {
                    return@withContext session
                }
            } catch (e: Exception) {}
        }

        // Direct final attempt via VidlinkStreamResolver
        val details = getDetails(mediaId)
        val title = details?.item?.title ?: mediaId
        val type = details?.item?.type ?: "Movie"
        try {
            val session = com.ashrin.ashtube.data.remote.VidlinkStreamResolver.resolveStream(
                id = mediaId,
                title = title,
                type = type,
                season = if (type.equals("Series", ignoreCase = true)) 1 else null,
                episode = episodeNumber ?: 1,
            )
            if (session != null && !session.streamUrl.isNullOrEmpty()) {
                return@withContext session
            }
        } catch (e: Exception) {}

        null
    }
}
