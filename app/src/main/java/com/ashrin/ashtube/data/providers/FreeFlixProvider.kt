package com.ashrin.ashtube.data.providers

import com.ashrin.ashtube.data.Episode
import com.ashrin.ashtube.data.HomeSection
import com.ashrin.ashtube.data.MediaDetails
import com.ashrin.ashtube.data.MediaItem
import com.ashrin.ashtube.data.PlaybackSession
import com.ashrin.ashtube.data.remote.CinemetaCatalogService
import com.ashrin.ashtube.data.remote.VidlinkStreamResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Provider 4: freeflix-cli (Python) Scraper Backend
 * Specializes in multi-source global films, international award winners, and indie productions.
 */
class FreeFlixProvider : MediaProvider {
    override val name: String = "freeflix-cli"
    override val priority: Int = 80

    private val freeFlixCatalog = listOf(
        MediaItem(
            id = "tt0068646",
            title = "The Godfather",
            type = "Movie",
            year = "1972",
            posterUrl = "https://image.tmdb.org/t/p/w500/3bhkrj58Vtu7enYsRolD1fZdja1.jpg",
            provider = name,
            rating = "★ 9.2",
        ),
        MediaItem(
            id = "tt0245429",
            title = "Spirited Away",
            type = "Movie",
            year = "2001",
            posterUrl = "https://image.tmdb.org/t/p/w500/39wmItIWsg5sZMyRUHLkWBcuVCM.jpg",
            provider = name,
            rating = "★ 8.5",
        ),
        MediaItem(
            id = "tt0468569",
            title = "The Dark Knight",
            type = "Movie",
            year = "2008",
            posterUrl = "https://image.tmdb.org/t/p/w500/qJ2tW6WMUDux911r6m7haRef0WH.jpg",
            provider = name,
            rating = "★ 9.0",
        ),
        MediaItem(
            id = "tt6751668",
            title = "Parasite",
            type = "Movie",
            year = "2019",
            posterUrl = "https://image.tmdb.org/t/p/w500/7IiTTgloJzvGI1TAYymCfbfl3vT.jpg",
            provider = name,
            rating = "★ 8.5",
        ),
        MediaItem(
            id = "tt0110912",
            title = "Pulp Fiction",
            type = "Movie",
            year = "1994",
            posterUrl = "https://image.tmdb.org/t/p/w500/d5iIlFn5s0ImszYzBPb8JPIfbXD.jpg",
            provider = name,
            rating = "★ 8.9",
        ),
        MediaItem(
            id = "tt0903747",
            title = "Breaking Bad",
            type = "Series",
            year = "2008",
            posterUrl = "https://image.tmdb.org/t/p/w500/ztkUQFLlC19CCMYHW9o1zWhJRNq.jpg",
            provider = name,
            rating = "★ 9.5",
        ),
    )

    override suspend fun isAvailable(): Boolean = true

    override suspend fun search(query: String): List<MediaItem> = withContext(Dispatchers.IO) {
        val normalized = query.trim().lowercase()
        val localMatches = freeFlixCatalog.filter {
            it.title.lowercase().contains(normalized)
        }
        try {
            val liveResults = CinemetaCatalogService.search(query).map { it.copy(provider = name) }
            (localMatches + liveResults).distinctBy { it.id }
        } catch (_: Exception) {
            localMatches
        }
    }

    override suspend fun getHomeSection(): HomeSection {
        return HomeSection(
            title = "All-Time Classics (freeflix-cli)",
            items = freeFlixCatalog,
        )
    }

    override suspend fun getDetails(id: String): MediaDetails? = withContext(Dispatchers.IO) {
        val liveDetails = try {
            CinemetaCatalogService.getDetails(id)
        } catch (_: Exception) {
            null
        }
        if (liveDetails != null) return@withContext liveDetails

        val item = freeFlixCatalog.find { it.id == id } ?: return@withContext null
        val episodes = if (item.type == "Series") {
            listOf(
                Episode(1, 1, "Pilot"),
                Episode(1, 2, "Cat's in the Bag..."),
                Episode(1, 3, "...And the Bag's in the River"),
                Episode(1, 4, "Cancer Man"),
            )
        } else emptyList()

        MediaDetails(
            item = item,
            summary = "Streamed via freeflix-cli multi-source repository. Master quality release with crystal audio.",
            episodes = episodes,
            availableQualities = listOf("1080p", "720p", "480p", "360p"),
            backdropUrl = item.posterUrl,
            genre = "Crime / Drama / Masterpiece",
        )
    }

    override suspend fun resolveStream(id: String, episodeNumber: Int?): PlaybackSession? = withContext(Dispatchers.IO) {
        val details = getDetails(id)
        val title = details?.item?.title ?: id
        val type = details?.item?.type ?: "Movie"
        val ep = episodeNumber ?: 1

        VidlinkStreamResolver.resolveStream(
            id = id,
            title = title,
            type = type,
            season = if (type == "Series") 1 else null,
            episode = if (type == "Series") ep else null,
        )
    }
}
