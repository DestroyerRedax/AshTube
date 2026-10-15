package com.ashrin.ashtube.data

data class MediaItem(
    val id: String,
    val title: String,
    val type: String,
    val year: String,
)

data class Episode(
    val season: Int,
    val episode: Int,
    val title: String,
)

data class MediaDetails(
    val item: MediaItem,
    val summary: String,
    val episodes: List<Episode> = emptyList(),
    val availableQualities: List<String> = listOf("Auto", "1080p", "720p", "480p"),
)

data class HistoryEntry(
    val mediaId: String,
    val title: String,
    val type: String,
    val year: String,
    val timestamp: Long = System.currentTimeMillis(),
)

data class HomeSection(
    val title: String,
    val items: List<MediaItem>,
)

/**
 * Development-only content and state for the Android UI. This repository has no
 * network access and will be replaced by the Rust bridge and local Room persistence in later phases.
 */
object MockCatalogRepository {
    private val trending = listOf(
        MediaItem("mock-movie-1", "Ashes of Tomorrow", "Movie", "2026"),
        MediaItem("mock-series-1", "Signal Harbor", "Series", "2026"),
        MediaItem("mock-anime-1", "Paper Skies", "Anime", "2026"),
    )

    private val movies = listOf(
        MediaItem("mock-movie-2", "Night Circuit", "Movie", "2026"),
        MediaItem("mock-movie-3", "The Quiet Atlas", "Movie", "2025"),
        MediaItem("mock-movie-4", "Driftwood", "Movie", "2025"),
    )

    private val series = listOf(
        MediaItem("mock-series-2", "Northern Line", "Series", "2026"),
        MediaItem("mock-series-3", "Second Signal", "Series", "2025"),
        MediaItem("mock-series-4", "Long Weekend", "Series", "2025"),
    )

    private val anime = listOf(
        MediaItem("mock-anime-2", "Lantern City", "Anime", "2026"),
        MediaItem("mock-anime-3", "Orbit Notes", "Anime", "2025"),
        MediaItem("mock-anime-4", "Blue Paper", "Anime", "2025"),
    )

    private val kids = listOf(
        MediaItem("mock-kids-1", "Cloud Club", "Kids", "2026"),
        MediaItem("mock-kids-2", "Pocket Planet", "Kids", "2025"),
        MediaItem("mock-kids-3", "Little Lanterns", "Kids", "2025"),
    )

    // In-memory sets to model favourites and history contracts
    private val favoriteIds = mutableSetOf<String>()
    private val history = mutableListOf<HistoryEntry>()

    fun homeSections(): List<HomeSection> {
        val sections = mutableListOf(
            HomeSection("Trending now", trending),
            HomeSection("Popular movies", movies),
            HomeSection("Popular series", series),
            HomeSection("Anime", anime),
            HomeSection("Kids", kids),
        )

        val favs = getFavorites()
        if (favs.isNotEmpty()) {
            sections.add(0, HomeSection("Your Favourites", favs))
        }

        return sections
    }

    fun allItems(): List<MediaItem> =
        listOf(trending, movies, series, anime, kids).flatten().distinctBy(MediaItem::id)

    fun findItem(id: String): MediaItem? =
        allItems().find { it.id == id }

    fun search(query: String): List<MediaItem> {
        val normalized = query.trim()
        if (normalized.isEmpty()) return emptyList()
        return allItems().filter { item -> item.title.contains(normalized, ignoreCase = true) }
    }

    fun getDetails(id: String): MediaDetails? {
        val item = findItem(id) ?: return null
        val episodes = if (item.type == "Series" || item.type == "Anime") {
            listOf(
                Episode(1, 1, "First Light"),
                Episode(1, 2, "High Tide"),
                Episode(1, 3, "Echo Chamber"),
                Episode(1, 4, "Clear Horizon"),
            )
        } else {
            emptyList()
        }
        val summary = "A local mock ${item.type.lowercase()} titled '${item.title}' (${item.year}). " +
            "This title is used for testing layout, navigation, and local state contracts in AshTube."
        return MediaDetails(
            item = item,
            summary = summary,
            episodes = episodes,
        )
    }

    fun isFavorite(id: String): Boolean = favoriteIds.contains(id)

    fun toggleFavorite(id: String): Boolean {
        return if (favoriteIds.contains(id)) {
            favoriteIds.remove(id)
            false
        } else {
            favoriteIds.add(id)
            true
        }
    }

    fun getFavorites(): List<MediaItem> =
        favoriteIds.mapNotNull { findItem(it) }

    fun recordHistory(id: String) {
        val item = findItem(id) ?: return
        history.removeAll { it.mediaId == id }
        history.add(0, HistoryEntry(item.id, item.title, item.type, item.year))
    }

    fun getHistory(): List<HistoryEntry> = history.toList()
}
