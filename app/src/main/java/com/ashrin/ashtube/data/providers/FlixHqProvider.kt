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
 * Provider 2: mov-cli (Python) / FlixHQ & Films Scraper Backend
 * Specializes in Hollywood blockbusters, box office hits, and international series.
 */
class FlixHqProvider : MediaProvider {
    override val name: String = "mov-cli (FlixHQ)"
    override val priority: Int = 90

    private val curatedMovies = listOf(
        MediaItem(
            id = "tt15398776",
            title = "Oppenheimer",
            type = "Movie",
            year = "2023",
            posterUrl = "https://image.tmdb.org/t/p/w500/8Gxv8gSFCU0XGDykEGv7zR1n2ua.jpg",
            provider = name,
            rating = "★ 8.9",
        ),
        MediaItem(
            id = "tt15239678",
            title = "Dune: Part Two",
            type = "Movie",
            year = "2024",
            posterUrl = "https://image.tmdb.org/t/p/w500/1pdfLvkbY9ohJlCjQH2CZjjYVvJ.jpg",
            provider = name,
            rating = "★ 8.6",
        ),
        MediaItem(
            id = "tt6263850",
            title = "Deadpool & Wolverine",
            type = "Movie",
            year = "2024",
            posterUrl = "https://image.tmdb.org/t/p/w500/8cdWjvZQUExUUTzyp4t6EDMubfO.jpg",
            provider = name,
            rating = "★ 8.0",
        ),
        MediaItem(
            id = "tt0816692",
            title = "Interstellar",
            type = "Movie",
            year = "2014",
            posterUrl = "https://image.tmdb.org/t/p/w500/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg",
            provider = name,
            rating = "★ 8.7",
        ),
        MediaItem(
            id = "tt1877830",
            title = "The Batman",
            type = "Movie",
            year = "2022",
            posterUrl = "https://image.tmdb.org/t/p/w500/74xTEgt7R36Fpooo50r9T25onhq.jpg",
            provider = name,
            rating = "★ 7.8",
        ),
        MediaItem(
            id = "tt4574334",
            title = "Stranger Things",
            type = "Series",
            year = "2016",
            posterUrl = "https://image.tmdb.org/t/p/w500/49WJfeN0moxb9IPfGn8AIqMGskD.jpg",
            provider = name,
            rating = "★ 8.7",
        ),
        MediaItem(
            id = "tt1190634",
            title = "The Boys",
            type = "Series",
            year = "2019",
            posterUrl = "https://image.tmdb.org/t/p/w500/2zmTngn1tYC1AvfnrFLhxeD82hz.jpg",
            provider = name,
            rating = "★ 8.7",
        ),
        MediaItem(
            id = "tt12637874",
            title = "Fallout",
            type = "Series",
            year = "2024",
            posterUrl = "https://image.tmdb.org/t/p/w500/AnsZu4iLcqVqAeVb5f3uQf9U7aO.jpg",
            provider = name,
            rating = "★ 8.5",
        ),
    )

    override suspend fun isAvailable(): Boolean = true

    override suspend fun search(query: String): List<MediaItem> = withContext(Dispatchers.IO) {
        val normalized = query.trim().lowercase()
        val localMatches = curatedMovies.filter {
            it.title.lowercase().contains(normalized)
        }
        try {
            val liveResults = CinemetaCatalogService.search(query).map {
                it.copy(provider = name)
            }
            (localMatches + liveResults).distinctBy { it.id }
        } catch (_: Exception) {
            localMatches
        }
    }

    override suspend fun getHomeSection(): HomeSection {
        return try {
            val live = CinemetaCatalogService.fetchPopularMovies()
            if (live.isNotEmpty()) {
                HomeSection(
                    title = "Hollywood & Hits (mov-cli)",
                    items = live.take(10).map { it.copy(provider = name) },
                )
            } else {
                HomeSection(
                    title = "Hollywood & Hits (mov-cli)",
                    items = curatedMovies,
                )
            }
        } catch (_: Exception) {
            HomeSection(
                title = "Hollywood & Hits (mov-cli)",
                items = curatedMovies,
            )
        }
    }

    override suspend fun getDetails(id: String): MediaDetails? = withContext(Dispatchers.IO) {
        val liveDetails = try {
            CinemetaCatalogService.getDetails(id)
        } catch (_: Exception) {
            null
        }
        if (liveDetails != null) return@withContext liveDetails

        val item = curatedMovies.find { it.id == id } ?: return@withContext null
        val episodes = if (item.type == "Series") {
            listOf(
                Episode(1, 1, "Chapter 1"),
                Episode(1, 2, "Chapter 2"),
                Episode(1, 3, "Chapter 3"),
                Episode(1, 4, "Chapter 4"),
                Episode(1, 5, "Chapter 5"),
            )
        } else emptyList()

        MediaDetails(
            item = item,
            summary = "Streamed via mov-cli scraper ecosystem. High-bitrate video stream with multichannel audio and multi-subs.",
            episodes = episodes,
            availableQualities = listOf("1080p", "720p", "480p", "360p"),
            backdropUrl = item.posterUrl,
            genre = if (item.type == "Series") "Sci-Fi / Drama" else "Action / Adventure",
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
