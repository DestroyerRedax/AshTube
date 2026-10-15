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

data class SubtitleTrack(
    val id: String,
    val language: String,
    val label: String,
)

data class PlaybackSession(
    val mediaId: String,
    val title: String,
    val type: String,
    val episodeTitle: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val durationMs: Long = 7200000L,
    val startPositionMs: Long = 0L,
    val availableQualities: List<String> = listOf("Auto", "1080p", "720p", "480p"),
    val availableSubtitles: List<SubtitleTrack> = listOf(
        SubtitleTrack("sub-none", "None", "Off"),
        SubtitleTrack("sub-en", "English", "English (CC)"),
        SubtitleTrack("sub-bn", "Bengali", "বাংলা (Bengali)"),
        SubtitleTrack("sub-es", "Spanish", "Español"),
        SubtitleTrack("sub-fr", "French", "Français"),
    ),
)

data class HistoryEntry(
    val mediaId: String,
    val title: String,
    val type: String,
    val year: String,
    val positionMs: Long = 0L,
    val durationMs: Long = 7200000L,
    val timestamp: Long = System.currentTimeMillis(),
)

data class HomeSection(
    val title: String,
    val items: List<MediaItem>,
)

enum class DownloadState {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
}

data class DownloadItem(
    val id: String,
    val mediaId: String,
    val title: String,
    val quality: String,
    val progress: Float,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 850_000_000L,
    val state: DownloadState = DownloadState.QUEUED,
    val error: String? = null,
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

    // Strictly isolated adult catalog (18+)
    private val adultFeatured = listOf(
        MediaItem("mock-adult-1", "Midnight Noir", "Adult (18+)", "2026"),
        MediaItem("mock-adult-2", "Velvet Shadow", "Adult (18+)", "2025"),
    )

    private val adultSeries = listOf(
        MediaItem("mock-adult-3", "Neon Dreams", "Adult (18+)", "2026"),
        MediaItem("mock-adult-4", "Rouge City", "Adult (18+)", "2025"),
    )

    // In-memory sets to model favourites, history, downloads, and preferences
    private val favoriteIds = mutableSetOf<String>()
    private val history = mutableListOf<HistoryEntry>()
    private val downloads = mutableListOf<DownloadItem>()
    private var isAdultAccessConfirmed = false
    private var defaultPlaybackQuality = "1080p"
    private var appThemePreference = "Dark"

    fun isAdultAccessEnabled(): Boolean = isAdultAccessConfirmed

    fun setAdultAccessEnabled(enabled: Boolean) {
        isAdultAccessConfirmed = enabled
    }

    fun getDefaultQuality(): String = defaultPlaybackQuality
    fun setDefaultQuality(quality: String) { defaultPlaybackQuality = quality }

    fun getThemePreference(): String = appThemePreference
    fun setThemePreference(theme: String) { appThemePreference = theme }

    /**
     * General Home sections. Strictly excludes Adults (18+) content.
     */
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

    /**
     * Isolated Adults (18+) sections. Returns content only if adult access is confirmed.
     */
    fun adultSections(): List<HomeSection> {
        if (!isAdultAccessConfirmed) return emptyList()
        return listOf(
            HomeSection("Adult Spotlight (18+)", adultFeatured),
            HomeSection("Mature Series (18+)", adultSeries),
        )
    }

    fun allGeneralItems(): List<MediaItem> =
        listOf(trending, movies, series, anime, kids).flatten().distinctBy(MediaItem::id)

    fun allAdultItems(): List<MediaItem> =
        if (isAdultAccessConfirmed) listOf(adultFeatured, adultSeries).flatten().distinctBy(MediaItem::id)
        else emptyList()

    fun findItem(id: String): MediaItem? {
        val general = allGeneralItems().find { it.id == id }
        if (general != null) return general

        if (isAdultAccessConfirmed) {
            return listOf(adultFeatured, adultSeries).flatten().find { it.id == id }
        }
        return null
    }

    /**
     * General search. Strictly filters only general catalogue items.
     */
    fun search(query: String): List<MediaItem> {
        val normalized = query.trim()
        if (normalized.isEmpty()) return emptyList()
        return allGeneralItems().filter { item -> item.title.contains(normalized, ignoreCase = true) }
    }

    /**
     * Isolated search for Adults (18+) section.
     */
    fun searchAdult(query: String): List<MediaItem> {
        if (!isAdultAccessConfirmed) return emptyList()
        val normalized = query.trim()
        if (normalized.isEmpty()) return emptyList()
        return allAdultItems().filter { item -> item.title.contains(normalized, ignoreCase = true) }
    }

    fun getDetails(id: String): MediaDetails? {
        val item = findItem(id) ?: return null
        val episodes = if (item.type == "Series" || item.type == "Anime" || item.title == "Neon Dreams") {
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

    fun clearFavorites() {
        favoriteIds.clear()
    }

    fun recordHistory(id: String) {
        val item = findItem(id) ?: return
        history.removeAll { it.mediaId == id }
        history.add(0, HistoryEntry(item.id, item.title, item.type, item.year))
    }

    fun getHistory(): List<HistoryEntry> = history.toList()

    fun clearHistory() {
        history.clear()
    }

    fun enqueueDownload(mediaId: String, quality: String = "1080p") {
        val item = findItem(mediaId) ?: return
        if (downloads.any { it.mediaId == mediaId }) return
        val id = "dl-${downloads.size + 1}"
        downloads.add(
            DownloadItem(
                id = id,
                mediaId = item.id,
                title = item.title,
                quality = quality,
                progress = 0.45f,
                bytesDownloaded = 382_500_000L,
                totalBytes = 850_000_000L,
                state = DownloadState.DOWNLOADING,
            ),
        )
    }

    fun pauseDownload(id: String) {
        val index = downloads.indexOfFirst { it.id == id }
        if (index != -1) {
            val current = downloads[index]
            downloads[index] = current.copy(state = DownloadState.PAUSED)
        }
    }

    fun resumeDownload(id: String) {
        val index = downloads.indexOfFirst { it.id == id }
        if (index != -1) {
            val current = downloads[index]
            downloads[index] = current.copy(state = DownloadState.DOWNLOADING)
        }
    }

    fun retryDownload(id: String) {
        val index = downloads.indexOfFirst { it.id == id }
        if (index != -1) {
            val current = downloads[index]
            downloads[index] = current.copy(state = DownloadState.DOWNLOADING, error = null)
        }
    }

    fun markDownloadCompleted(id: String) {
        val index = downloads.indexOfFirst { it.id == id }
        if (index != -1) {
            val current = downloads[index]
            downloads[index] = current.copy(
                state = DownloadState.COMPLETED,
                progress = 1.0f,
                bytesDownloaded = current.totalBytes,
            )
        }
    }

    fun updateDownloadProgress(id: String, progress: Float, bytesDownloaded: Long) {
        val index = downloads.indexOfFirst { it.id == id }
        if (index != -1) {
            val current = downloads[index]
            downloads[index] = current.copy(
                progress = progress,
                bytesDownloaded = bytesDownloaded,
                state = if (progress >= 1.0f) DownloadState.COMPLETED else DownloadState.DOWNLOADING,
            )
        }
    }

    fun getDownloads(): List<DownloadItem> = downloads.toList()

    fun removeDownload(id: String) {
        downloads.removeAll { it.id == id }
    }

    private val progressMap = mutableMapOf<String, Long>()

    fun recordProgress(mediaId: String, positionMs: Long, durationMs: Long) {
        progressMap[mediaId] = positionMs
        val item = findItem(mediaId) ?: return
        history.removeAll { it.mediaId == id }
        history.add(0, HistoryEntry(item.id, item.title, item.type, item.year, positionMs, durationMs))
    }

    fun getProgress(mediaId: String): Long = progressMap[mediaId] ?: 0L

    fun createPlaybackSession(mediaId: String, episodeNumber: Int? = null): PlaybackSession? {
        val details = getDetails(mediaId) ?: return null
        val item = details.item
        val episode = if (episodeNumber != null) {
            details.episodes.find { it.episode == episodeNumber }
        } else if (details.episodes.isNotEmpty()) {
            details.episodes.first()
        } else {
            null
        }

        val startPos = getProgress(mediaId)
        recordHistory(mediaId)

        return PlaybackSession(
            mediaId = item.id,
            title = item.title,
            type = item.type,
            episodeTitle = episode?.title,
            seasonNumber = episode?.season,
            episodeNumber = episode?.episode,
            durationMs = 7200000L,
            startPositionMs = startPos,
            availableQualities = details.availableQualities,
        )
    }

    fun getNextEpisode(mediaId: String, currentEpisodeNumber: Int): Episode? {
        val details = getDetails(mediaId) ?: return null
        return details.episodes.find { it.episode == currentEpisodeNumber + 1 }
    }
}
