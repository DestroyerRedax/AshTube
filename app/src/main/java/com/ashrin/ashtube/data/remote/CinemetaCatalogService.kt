package com.ashrin.ashtube.data.remote

import com.ashrin.ashtube.data.Episode
import com.ashrin.ashtube.data.MediaDetails
import com.ashrin.ashtube.data.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Universal media catalog service powered by Cinemeta.
 * Provides real titles, HD posters, ratings, overviews, and full episode maps for films and TV shows.
 */
object CinemetaCatalogService {
    private const val CINEMETA_API = "https://v3-cinemeta.strem.io"
    private const val DIRECT_CATALOG = "https://cinemeta-catalogs.strem.io/top/catalog"
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    private fun fetchJson(urlString: String): JSONObject? {
        var currentUrl = urlString
        for (redirect in 0..3) {
            try {
                val conn = URL(currentUrl).openConnection() as HttpURLConnection
                conn.setRequestProperty("User-Agent", USER_AGENT)
                conn.instanceFollowRedirects = true
                conn.connectTimeout = 6000
                conn.readTimeout = 6000

                val code = conn.responseCode
                if (code in listOf(301, 302, 303, 307, 308)) {
                    val location = conn.getHeaderField("Location")
                    if (!location.isNullOrEmpty()) {
                        currentUrl = location
                        continue
                    }
                }

                if (code in 200..299) {
                    val text = conn.inputStream.bufferedReader().use { it.readText() }
                    return JSONObject(text)
                }
            } catch (_: Exception) {
                break
            }
        }
        return null
    }

    suspend fun fetchPopularMovies(): List<MediaItem> = withContext(Dispatchers.IO) {
        val json = fetchJson("$DIRECT_CATALOG/movie/top/skip=0.json") 
            ?: fetchJson("$CINEMETA_API/catalog/movie/top.json")
        parseMetas(json, "Movie")
    }

    suspend fun fetchPopularSeries(): List<MediaItem> = withContext(Dispatchers.IO) {
        val json = fetchJson("$DIRECT_CATALOG/series/top/skip=0.json")
            ?: fetchJson("$CINEMETA_API/catalog/series/top.json")
        parseMetas(json, "Series")
    }

    suspend fun search(query: String): List<MediaItem> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext emptyList()
        val enc = URLEncoder.encode(trimmed, "UTF-8")

        val items = mutableListOf<MediaItem>()

        val movieJson = fetchJson("$CINEMETA_API/catalog/movie/top/search=$enc.json")
        items.addAll(parseMetas(movieJson, "Movie"))

        val seriesJson = fetchJson("$CINEMETA_API/catalog/series/top/search=$enc.json")
        items.addAll(parseMetas(seriesJson, "Series"))

        items.distinctBy { it.id }
    }

    suspend fun getDetails(id: String): MediaDetails? = withContext(Dispatchers.IO) {
        val isSeries = id.startsWith("series_") || id.contains("tv")
        val cleanId = id.removePrefix("series_").removePrefix("movie_")

        // Try movie first, then series
        val typePath = if (isSeries) "series" else "movie"
        var json = fetchJson("$CINEMETA_API/meta/$typePath/$cleanId.json")
        var resolvedType = if (isSeries) "Series" else "Movie"

        if (json?.optJSONObject("meta") == null && !isSeries) {
            json = fetchJson("$CINEMETA_API/meta/series/$cleanId.json")
            if (json?.optJSONObject("meta") != null) {
                resolvedType = "Series"
            }
        }

        val meta = json?.optJSONObject("meta") ?: return@withContext null
        val title = meta.optString("name").ifEmpty { "Unknown Title" }
        val year = meta.optString("year").take(4).ifEmpty { "2024" }
        val poster = meta.optString("poster").ifEmpty { null }
        val background = meta.optString("background").ifEmpty { poster }
        val desc = meta.optString("description").ifEmpty { "No overview available for this title." }
        val ratingRaw = meta.optString("imdbRating").ifEmpty { null }
        val rating = if (ratingRaw != null) "★ $ratingRaw" else null

        val genresArr = meta.optJSONArray("genre")
        val genre = if (genresArr != null && genresArr.length() > 0) {
            (0 until genresArr.length()).joinToString(" • ") { genresArr.optString(it) }
        } else null

        val episodes = mutableListOf<Episode>()
        val videosArr = meta.optJSONArray("videos")
        if (videosArr != null) {
            for (i in 0 until videosArr.length()) {
                val v = videosArr.optJSONObject(i) ?: continue
                val se = v.optInt("season", 1)
                val ep = v.optInt("number", i + 1)
                val epTitle = v.optString("name").ifEmpty { "Episode $ep" }
                if (se > 0) {
                    episodes.add(Episode(season = se, episode = ep, title = epTitle))
                }
            }
        }

        MediaDetails(
            item = MediaItem(
                id = cleanId,
                title = title,
                type = resolvedType,
                year = year,
                posterUrl = poster,
                provider = "Cinemeta",
                rating = rating,
            ),
            summary = desc,
            episodes = episodes,
            availableQualities = listOf("1080p", "720p", "480p", "360p"),
            backdropUrl = background,
            genre = genre,
        )
    }

    private fun parseMetas(json: JSONObject?, defaultType: String): List<MediaItem> {
        val list = mutableListOf<MediaItem>()
        val metas = json?.optJSONArray("metas") ?: return list
        for (i in 0 until metas.length()) {
            val m = metas.optJSONObject(i) ?: continue
            val id = m.optString("imdb_id").ifEmpty { m.optString("id") }
            val name = m.optString("name")
            if (id.isEmpty() || name.isEmpty()) continue

            val rawType = m.optString("type")
            val type = if (rawType.equals("series", ignoreCase = true)) "Series" else defaultType
            val yearRaw = m.optString("releaseInfo").ifEmpty { m.optString("year") }
            val year = yearRaw.take(4).ifEmpty { "2024" }
            val poster = m.optString("poster").ifEmpty { null }
            val ratingRaw = m.optString("imdbRating").ifEmpty { null }
            val rating = if (ratingRaw != null) "★ $ratingRaw" else null

            list.add(
                MediaItem(
                    id = id,
                    title = name,
                    type = type,
                    year = year,
                    posterUrl = poster,
                    provider = "Cinemeta",
                    rating = rating,
                )
            )
        }
        return list
    }
}
