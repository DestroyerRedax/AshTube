package com.ashrin.ashtube.data.remote

import com.ashrin.ashtube.data.PlaybackSession
import com.ashrin.ashtube.data.SubtitleTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * High-performance real content streaming resolver powered by Vidlink and multi-mirror CDN vaults.
 * Resolves full-length 1080p/720p/480p movies, episodes, and anime streams with complete multilingual subtitles.
 */
object VidlinkStreamResolver {
    private const val ENC_API = "https://enc-dec.app/api"
    private const val DB_API = "https://enc-dec.app/db/flix"
    private const val VIDLINK_API = "https://vidlink.pro/api/b"
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    suspend fun resolveStream(
        id: String,
        title: String,
        type: String,
        season: Int? = null,
        episode: Int? = null,
    ): PlaybackSession? = withContext(Dispatchers.IO) {
        val isSeries = type.equals("Series", ignoreCase = true) || (season != null && season > 0) || (episode != null && episode > 0)
        val s = season ?: 1
        val ep = episode ?: 1

        var tmdbId: String? = null
        var imdbId: String? = null

        if (id.startsWith("tt", ignoreCase = true)) {
            imdbId = id
        } else if (id.all { it.isDigit() }) {
            tmdbId = id
        }

        // Try looking up TMDB ID if only IMDb is known
        if (tmdbId == null && imdbId != null) {
            tmdbId = lookupTmdbIdByImdb(imdbId)
        }

        // Search by title if IDs are missing
        if (tmdbId == null && imdbId == null && title.isNotEmpty()) {
            val (foundTmdb, foundImdb) = searchIdsByTitle(title, isSeries)
            tmdbId = foundTmdb
            imdbId = foundImdb
        }

        val session = if (isSeries) {
            val targetId = tmdbId ?: imdbId ?: return@withContext null
            resolveVidlinkTv(id, title, targetId, s, ep)
        } else {
            val targetId = tmdbId ?: imdbId ?: return@withContext null
            resolveVidlinkMovie(id, title, targetId)
        }

        session
    }

    private fun lookupTmdbIdByImdb(imdbId: String): String? {
        try {
            val url = URL("$DB_API/find?imdb_id=$imdbId")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            if (conn.responseCode in 200..299) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val arr = JSONArray(text)
                if (arr.length() > 0) {
                    val info = arr.getJSONObject(0).optJSONObject("info")
                    val tmdb = info?.optString("tmdb_id")
                    if (!tmdb.isNullOrEmpty()) return tmdb
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun searchIdsByTitle(title: String, isSeries: Boolean): Pair<String?, String?> {
        try {
            val encTitle = URLEncoder.encode(title, "UTF-8")
            val typeStr = if (isSeries) "tv" else "movie"
            val url = URL("$DB_API/search?query=$encTitle&type=$typeStr")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            if (conn.responseCode in 200..299) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val arr = JSONArray(text)
                if (arr.length() > 0) {
                    val info = arr.getJSONObject(0).optJSONObject("info")
                    val tmdb = info?.optString("tmdb_id")?.ifEmpty { null }
                    val imdb = info?.optString("imdb_id")?.ifEmpty { null }
                    return Pair(tmdb, imdb)
                }
            }
        } catch (_: Exception) {}
        return Pair(null, null)
    }

    private fun encryptId(text: String): String? {
        try {
            val url = URL("$ENC_API/enc-vidlink?text=${URLEncoder.encode(text, "UTF-8")}")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            if (conn.responseCode in 200..299) {
                val textResp = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(textResp)
                if (json.optInt("status") == 200) {
                    return json.optString("result").ifEmpty { null }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun resolveVidlinkMovie(id: String, title: String, targetId: String): PlaybackSession? {
        val enc = encryptId(targetId) ?: return null
        try {
            val url = URL("$VIDLINK_API/movie/$enc")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.setRequestProperty("Origin", "https://vidlink.pro")
            conn.setRequestProperty("Referer", "https://vidlink.pro/")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            if (conn.responseCode in 200..299) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(text)
                return parseVidlinkResponse(id, title, "Movie", null, null, json)
            }
        } catch (_: Exception) {}
        return null
    }

    private fun resolveVidlinkTv(id: String, title: String, targetId: String, season: Int, episode: Int): PlaybackSession? {
        val enc = encryptId(targetId) ?: return null
        try {
            val url = URL("$VIDLINK_API/tv/$enc/$season/$episode")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.setRequestProperty("Origin", "https://vidlink.pro")
            conn.setRequestProperty("Referer", "https://vidlink.pro/")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            if (conn.responseCode in 200..299) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(text)
                return parseVidlinkResponse(id, title, "Series", season, episode, json)
            }
        } catch (_: Exception) {}
        return null
    }

    private fun parseVidlinkResponse(
        id: String,
        title: String,
        type: String,
        season: Int?,
        episode: Int?,
        json: JSONObject
    ): PlaybackSession? {
        val streamObj = json.optJSONObject("stream") ?: return null

        val playlist = streamObj.optString("playlist")
        val qualitiesObj = streamObj.optJSONObject("qualities")
        val availableQualities = mutableListOf<String>()
        var bestUrl: String? = null

        if (qualitiesObj != null) {
            val qualityKeys = listOf("1080", "720", "480", "360")
            for (q in qualityKeys) {
                val qObj = qualitiesObj.optJSONObject(q)
                val qUrl = qObj?.optString("url")
                if (!qUrl.isNullOrEmpty()) {
                    availableQualities.add("${q}p")
                    if (bestUrl == null) {
                        bestUrl = qUrl
                    }
                }
            }
        }

        val chosenStreamUrl = bestUrl ?: playlist.ifEmpty { null } ?: return null
        if (availableQualities.isEmpty()) {
            availableQualities.add("Auto")
        }

        val subtitles = mutableListOf<SubtitleTrack>(
            SubtitleTrack("sub-none", "None", "Off")
        )
        val captions = json.optJSONArray("captions") ?: streamObj.optJSONArray("captions")
        if (captions != null) {
            for (i in 0 until captions.length()) {
                val c = captions.optJSONObject(i) ?: continue
                val cUrl = c.optString("url")
                val lang = c.optString("language").ifEmpty { c.optString("id", "Subtitle ${i + 1}") }
                if (cUrl.isNotEmpty()) {
                    subtitles.add(SubtitleTrack(id = cUrl, language = lang, label = lang))
                }
            }
        }

        val epTitle = if (episode != null && episode > 0) "Season ${season ?: 1}, Episode $episode" else null

        return PlaybackSession(
            mediaId = id,
            title = title,
            type = type,
            episodeTitle = epTitle,
            seasonNumber = season,
            episodeNumber = episode,
            durationMs = 7200000L,
            startPositionMs = 0L,
            streamUrl = chosenStreamUrl,
            availableQualities = availableQualities,
            availableSubtitles = subtitles
        )
    }
}
