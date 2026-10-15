package com.ashrin.ashtube.data.remote

import android.util.Base64
import com.ashrin.ashtube.data.Episode
import com.ashrin.ashtube.data.HomeSection
import com.ashrin.ashtube.data.MediaDetails
import com.ashrin.ashtube.data.MediaItem
import com.ashrin.ashtube.data.PlaybackSession
import com.ashrin.ashtube.data.SubtitleTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Remote provider service integrating the open-source MovieBox-TUI backend.
 * Provides live search, homepage catalogues, detailed episode metadata, and stream resolution.
 */
object MovieBoxRemoteService {

    private val HOST_POOL = listOf(
        "https://api6.aoneroom.com",
        "https://api5.aoneroom.com",
        "https://api4.aoneroom.com",
        "https://api.inmoviebox.com",
    )

    private val SECRET_BYTES = byteArrayOf(
        0xef.toByte(), 0xa8.toByte(), 0x91.toByte(), 0x97.toByte(), 0x4e.toByte(),
        0xec.toByte(), 0xd3.toByte(), 0x14.toByte(), 0x8d.toByte(), 0xf6.toByte(),
        0x3a.toByte(), 0xa6.toByte(), 0x11.toByte(), 0x60.toByte(), 0x2d.toByte(),
        0xef.toByte(), 0xd1.toByte(), 0x01.toByte(), 0x25.toByte(), 0x9b.toByte(),
        0xa5.toByte(), 0x21.toByte(), 0x02.toByte(), 0x2c.toByte(), 0x57.toByte(),
        0xae.toByte(), 0x05.toByte(), 0x66.toByte(), 0xbd.toByte(), 0x8e.toByte(),
    )

    private const val USER_AGENT =
        "com.community.oneroom/50020120 (Linux; U; Android 12; en_US; 22101316G; Build/S1B.220414.015; Cronet/135.0.7012.3)"

    private const val CLIENT_INFO =
        """{"package_name":"com.community.oneroom","version_name":"4.0.01.0813.03","version_code":50020120,"os":"android","os_version":"12","install_ch":"ps","device_id":"a1b2c3d4e5f60718293a4b5c6d7e8f90","gaid":"12345678-1234-1234-1234-123456789012","brand":"Redmi","model":"22101316G","system_language":"en","net":"NETWORK_WIFI","region":"US","timezone":"America/New_York","sp_code":"40401","X-Play-Mode":"2"}"""

    @Volatile
    private var sessionToken: String? = null

    @Volatile
    private var activeHostIndex = 0

    private fun md5(data: ByteArray): String {
        val md = MessageDigest.getInstance("MD5")
        return md.digest(data).joinToString("") { "%02x".format(it) }
    }

    private fun hmacMd5(data: String): String {
        val mac = Mac.getInstance("HmacMD5")
        mac.init(SecretKeySpec(SECRET_BYTES, "HmacMD5"))
        val raw = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(raw, Base64.NO_WRAP)
    }

    private fun buildCanonicalUrl(urlStr: String): String {
        val uri = URI(urlStr)
        val path = uri.path ?: ""
        val rawQuery = uri.query
        if (rawQuery.isNullOrEmpty()) return path
        val params = rawQuery.split("&").filter { it.isNotEmpty() }
        val sortedParams = params.sorted()
        return "$path?${sortedParams.joinToString("&")}"
    }

    private fun buildCanonicalString(
        method: String,
        accept: String = "application/json",
        contentType: String = "application/json",
        urlStr: String,
        body: String?,
        timestampMs: Long,
    ): String {
        val canonicalUrl = buildCanonicalUrl(urlStr)
        val bodyBytes = body?.toByteArray(Charsets.UTF_8)
        val bodyLen = bodyBytes?.size?.toString() ?: ""
        val bodyHash = if (bodyBytes != null) md5(bodyBytes) else ""
        return "$method\n$accept\n$contentType\n$bodyLen\n$timestampMs\n$bodyHash\n$canonicalUrl"
    }

    private suspend fun ensureSession(): String? = withContext(Dispatchers.IO) {
        if (!sessionToken.isNullOrEmpty()) return@withContext sessionToken

        for (i in HOST_POOL.indices) {
            val host = HOST_POOL[(activeHostIndex + i) % HOST_POOL.size]
            try {
                val path = "/wefeed-mobile-bff/user-api/visitor-login"
                val body = "{}"
                val ts = System.currentTimeMillis()
                val revTs = ts.toString().reversed()
                val clientToken = "$ts,${md5(revTs.toByteArray(Charsets.UTF_8))}"
                val canonical = buildCanonicalString("POST", "application/json", "application/json", "$host$path", body, ts)
                val sig = "$ts|2|${hmacMd5(canonical)}"

                val url = URL("$host$path")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 8000
                    readTimeout = 8000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", USER_AGENT)
                    setRequestProperty("x-client-info", CLIENT_INFO)
                    setRequestProperty("x-client-token", clientToken)
                    setRequestProperty("x-tr-signature", sig)
                    setRequestProperty("x-tr-timestamp", ts.toString())
                    setRequestProperty("x-client-status", "0")
                    setRequestProperty("x-forwarded-for", "103.145.23.45")
                }

                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

                if (conn.responseCode in 200..299) {
                    val respText = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(respText)
                    if (json.optInt("code", -1) == 0) {
                        val token = json.optJSONObject("data")?.optString("token")
                        if (!token.isNullOrEmpty()) {
                            sessionToken = token
                            activeHostIndex = (activeHostIndex + i) % HOST_POOL.size
                            return@withContext token
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        null
    }

    private suspend fun executeRequest(
        method: String,
        pathWithQuery: String,
        body: String? = null,
    ): JSONObject? = withContext(Dispatchers.IO) {
        val token = ensureSession() ?: return@withContext null

        for (i in HOST_POOL.indices) {
            val host = HOST_POOL[(activeHostIndex + i) % HOST_POOL.size]
            val fullUrl = "$host$pathWithQuery"
            try {
                val ts = System.currentTimeMillis()
                val revTs = ts.toString().reversed()
                val clientToken = "$ts,${md5(revTs.toByteArray(Charsets.UTF_8))}"
                val canonical = buildCanonicalString(method, "application/json", "application/json", fullUrl, body, ts)
                val sig = "$ts|2|${hmacMd5(canonical)}"

                val conn = (URL(fullUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = method
                    connectTimeout = 8000
                    readTimeout = 8000
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $token")
                    setRequestProperty("User-Agent", USER_AGENT)
                    setRequestProperty("x-client-info", CLIENT_INFO)
                    setRequestProperty("x-client-token", clientToken)
                    setRequestProperty("x-tr-signature", sig)
                    setRequestProperty("x-tr-timestamp", ts.toString())
                    setRequestProperty("x-client-status", "0")
                    setRequestProperty("x-forwarded-for", "103.145.23.45")
                    if (body != null) {
                        doOutput = true
                    }
                }

                if (body != null) {
                    conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                }

                if (conn.responseCode in 200..299) {
                    val respText = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(respText)
                    if (json.optInt("code", -1) == 0) {
                        return@withContext json
                    } else if (json.optInt("code", -1) in listOf(401, 403, 407)) {
                        sessionToken = null // Token expired; refresh on retry
                    }
                }
            } catch (_: Exception) {}
        }
        null
    }

    /**
     * Search live movies, TV series, and shows from the MovieBox-TUI backend.
     */
    suspend fun search(query: String, page: Int = 1): List<MediaItem> = withContext(Dispatchers.IO) {
        val normalized = query.trim()
        if (normalized.isEmpty()) return@withContext emptyList()

        val payload = JSONObject().apply {
            put("keyword", normalized)
            put("page", page)
            put("perPage", 15)
            put("subjectType", 0)
        }.toString()

        val response = executeRequest("POST", "/wefeed-mobile-bff/subject-api/search/v2", payload) ?: return@withContext emptyList()
        val data = response.optJSONObject("data") ?: return@withContext emptyList()
        val results = data.optJSONArray("results") ?: return@withContext emptyList()

        val items = mutableListOf<MediaItem>()
        for (i in 0 until results.length()) {
            val topic = results.optJSONObject(i) ?: continue
            val subjects = topic.optJSONArray("subjects") ?: continue
            for (j in 0 until subjects.length()) {
                val sub = subjects.optJSONObject(j) ?: continue
                val id = sub.optString("subjectId")
                val title = sub.optString("title")
                if (id.isEmpty() || title.isEmpty()) continue
                val stype = sub.optInt("subjectType", 1)
                val type = if (stype == 2) "Series" else "Movie"
                val releaseDate = sub.optString("releaseDate")
                val year = if (releaseDate.length >= 4) releaseDate.substring(0, 4) else "2024"
                val coverObj = sub.optJSONObject("cover")
                val posterUrl = coverObj?.optString("url")?.ifEmpty { null } ?: sub.optString("cover").ifEmpty { null }
                val rating = sub.optDouble("imdbRating", 0.0).let { if (it > 0) "★ %.1f".format(it) else null }
                items.add(
                    MediaItem(
                        id = id,
                        title = title,
                        type = type,
                        year = year,
                        posterUrl = posterUrl,
                        provider = "MovieBox",
                        rating = rating,
                    ),
                )
            }
        }
        items.distinctBy { it.id }
    }

    /**
     * Fetch full title details including summary, cast, and episode lists.
     */
    suspend fun getDetails(subjectId: String): MediaDetails? = withContext(Dispatchers.IO) {
        val detailsJson = executeRequest("GET", "/wefeed-mobile-bff/subject-api/get?subjectId=$subjectId")
        val data = detailsJson?.optJSONObject("data") ?: return@withContext null

        val title = data.optString("title").ifEmpty { "Unknown Title" }
        val stype = data.optInt("subjectType", 1)
        val type = if (stype == 2) "Series" else "Movie"
        val releaseDate = data.optString("releaseDate")
        val year = if (releaseDate.length >= 4) releaseDate.substring(0, 4) else "2026"
        val summary = data.optString("description").ifEmpty { "No overview available for this title." }
        val coverObj = data.optJSONObject("cover")
        val posterUrl = coverObj?.optString("url")?.ifEmpty { null } ?: data.optString("cover").ifEmpty { null }
        val bannerObj = data.optJSONObject("banner") ?: data.optJSONObject("cover")
        val backdropUrl = bannerObj?.optString("url")?.ifEmpty { null } ?: data.optString("banner").ifEmpty { null }
        val rating = data.optDouble("imdbRating", 0.0).let { if (it > 0) "★ %.1f".format(it) else null }
        val genre = data.optString("genre").ifEmpty { null }

        val episodes = mutableListOf<Episode>()
        if (stype == 2) {
            val seasonJson = executeRequest("GET", "/wefeed-mobile-bff/subject-api/season-info?subjectId=$subjectId")
            val seasonData = seasonJson?.optJSONObject("data")
            val seasonsArr = seasonData?.optJSONArray("seasons")
            if (seasonsArr != null) {
                for (s in 0 until seasonsArr.length()) {
                    val sObj = seasonsArr.optJSONObject(s) ?: continue
                    val seNum = sObj.optInt("se", s + 1)
                    val maxEp = sObj.optInt("maxEp", 0)
                    for (ep in 1..maxEp) {
                        episodes.add(Episode(season = seNum, episode = ep, title = "Episode $ep"))
                    }
                }
            }
        }

        MediaDetails(
            item = MediaItem(
                id = subjectId,
                title = title,
                type = type,
                year = year,
                posterUrl = posterUrl,
                provider = "MovieBox",
                rating = rating,
            ),
            summary = summary,
            episodes = episodes,
            availableQualities = listOf("Auto", "1080p", "720p", "480p"),
            backdropUrl = backdropUrl,
            genre = genre,
        )
    }

    /**
     * Checks if a stream URL points to the known 22-second deprecation/update notice video.
     */
    fun isDeprecationNoticeUrl(url: String?): Boolean {
        if (url.isNullOrEmpty()) return true
        val lower = url.lowercase()
        return lower.contains("1c7de0bd3393702d9191801f15f88f8d")
            || lower.contains("9a0461bc39da389663bf3dbb17091d3f")
            || lower.contains("b164fbfb4347792950bdfbfb563d39d9")
            || lower.contains("80da52722ee7de7a4303290af6783424")
            || lower.contains("/notice.mp4")
            || (lower.contains("macdn.aoneroom.com") && lower.contains("/other/"))
    }

    /**
     * Resolves the real DASH MPD manifest URL from the signed CloudFront cookie.
     */
    fun resolveDashManifestFromPolicy(signCookie: String?): String? {
        if (signCookie.isNullOrEmpty()) return null
        for (part in signCookie.split(";")) {
            val trimmed = part.trim()
            if (trimmed.contains("urlprefix=")) {
                val prefixPart = trimmed.substringAfter("urlprefix=")
                val b64Token = prefixPart.substringBefore(":").trim().replace('-', '+').replace('_', '/')
                try {
                    val decoded = String(Base64.decode(b64Token, Base64.DEFAULT), Charsets.UTF_8)
                    val base = decoded.trimEnd('*', '/')
                    if (base.contains("/dash/")) {
                        return "$base/index.mpd"
                    }
                } catch (_: Exception) {}
            }
            if (trimmed.startsWith("CloudFront-Policy=")) {
                val policyRaw = trimmed.substringAfter("CloudFront-Policy=").trim()
                    .replace('-', '+').replace('_', '=').replace('~', '/')
                try {
                    val decoded = String(Base64.decode(policyRaw, Base64.DEFAULT), Charsets.UTF_8)
                    val json = JSONObject(decoded)
                    val resource = json.optJSONArray("Statement")
                        ?.optJSONObject(0)
                        ?.optString("Resource") ?: ""
                    val base = resource.trimEnd('*', '/')
                    if (base.contains("/dash/")) {
                        return "$base/index.mpd"
                    }
                } catch (_: Exception) {}
            }
        }
        return null
    }

    /**
     * Resolve live video streams and multilingual subtitles.
     * Rejects deprecation notice videos and ensures playable HD stream is always returned.
     */
    suspend fun getPlaybackSession(subjectId: String, episodeNumber: Int? = null): PlaybackSession? = withContext(Dispatchers.IO) {
        val details = getDetails(subjectId) ?: return@withContext null
        val ep = episodeNumber ?: if (details.episodes.isNotEmpty()) details.episodes.first().episode else 0
        val se = if (details.episodes.isNotEmpty()) details.episodes.first().season else 0

        val candidateStreams = mutableListOf<String>()

        // 1. Query play-info/v2
        val playInfoPath = if (se == 0 && ep == 0) {
            "/wefeed-mobile-bff/subject-api/play-info/v2?subjectId=$subjectId"
        } else {
            "/wefeed-mobile-bff/subject-api/play-info/v2?ep=$ep&se=$se&subjectId=$subjectId"
        }

        val playInfoJson = executeRequest("GET", playInfoPath)
        val data = playInfoJson?.optJSONObject("data")

        if (data != null) {
            val streams = data.optJSONArray("streams")
            if (streams != null) {
                for (i in 0 until streams.length()) {
                    val stream = streams.optJSONObject(i) ?: continue
                    val signCookie = stream.optString("signCookie")
                    val dashUrl = resolveDashManifestFromPolicy(signCookie)
                    if (!dashUrl.isNullOrEmpty()) {
                        candidateStreams.add(dashUrl)
                    }

                    val directUrl = stream.optString("url")
                    if (directUrl.isNotEmpty() && !isDeprecationNoticeUrl(directUrl)) {
                        candidateStreams.add(directUrl)
                    }
                }
            }
        }

        // 2. Query resource API for direct community/server streams
        val resourcePath = if (se == 0 && ep == 0) {
            "/wefeed-mobile-bff/subject-api/resource?page=1&perPage=10&subjectId=$subjectId"
        } else {
            "/wefeed-mobile-bff/subject-api/resource?page=1&perPage=10&subjectId=$subjectId&se=$se&ep=$ep"
        }
        val resourceJson = executeRequest("GET", resourcePath)
        val resourceList = resourceJson?.optJSONObject("data")?.optJSONArray("list")
        if (resourceList != null) {
            for (i in 0 until resourceList.length()) {
                val item = resourceList.optJSONObject(i) ?: continue
                val resLink = item.optString("resourceLink").ifEmpty { item.optString("url") }
                if (resLink.isNotEmpty() && !isDeprecationNoticeUrl(resLink)) {
                    candidateStreams.add(resLink)
                }
            }
        }

        // 3. Multilingual subtitles
        val subtitles = mutableListOf<SubtitleTrack>(
            SubtitleTrack("sub-none", "None", "Off"),
        )
        val captions = data?.optJSONArray("extCaptions")
        if (captions != null) {
            for (c in 0 until captions.length()) {
                val cap = captions.optJSONObject(c) ?: continue
                val lanName = cap.optString("lanName").ifEmpty { cap.optString("lan", "Unknown") }
                val url = cap.optString("url")
                if (url.isNotEmpty()) {
                    subtitles.add(SubtitleTrack(id = url, language = lanName, label = lanName))
                }
            }
        }

        // 4. Return valid direct candidate or delegate to Vidlink real streaming resolver
        val directCandidate = candidateStreams.firstOrNull()
        if (directCandidate != null) {
            val epTitle = if (ep > 0) "Season $se, Episode $ep" else null
            return@withContext PlaybackSession(
                mediaId = subjectId,
                title = details.item.title,
                type = details.item.type,
                episodeTitle = epTitle,
                seasonNumber = if (se > 0) se else null,
                episodeNumber = if (ep > 0) ep else null,
                durationMs = 7200000L,
                startPositionMs = 0L,
                streamUrl = directCandidate,
                availableQualities = listOf("Auto", "1080p", "720p", "480p"),
                availableSubtitles = subtitles,
            )
        }

        VidlinkStreamResolver.resolveStream(
            id = subjectId,
            title = details.item.title,
            type = details.item.type,
            season = if (se > 0) se else 1,
            episode = if (ep > 0) ep else 1,
        )
    }

    /**
     * Fetch trending categories and real movies for the Home feed.
     */
    suspend fun getHomepageSections(): List<HomeSection> = withContext(Dispatchers.IO) {
        val homeJson = executeRequest("GET", "/wefeed-mobile-bff/tab-operating?page=1&tabId=2&version=")
        val data = homeJson?.optJSONObject("data") ?: return@withContext emptyList()
        val itemsArr = data.optJSONArray("items") ?: return@withContext emptyList()

        val sections = mutableListOf<HomeSection>()
        for (i in 0 until itemsArr.length()) {
            val block = itemsArr.optJSONObject(i) ?: continue
            val rawTitle = block.optString("title").ifEmpty { block.optString("name") }
            val subjects = block.optJSONArray("subjects") ?: continue
            if (subjects.length() == 0) continue

            val cleanTitle = rawTitle.replace(Regex("[^a-zA-Z0-9 ]"), "").trim().ifEmpty { "Trending Movies" }
            val mediaItems = mutableListOf<MediaItem>()

            for (j in 0 until subjects.length()) {
                val sub = subjects.optJSONObject(j) ?: continue
                val id = sub.optString("subjectId")
                val title = sub.optString("title")
                if (id.isEmpty() || title.isEmpty()) continue
                val stype = sub.optInt("subjectType", 1)
                val type = if (stype == 2) "Series" else "Movie"
                val releaseDate = sub.optString("releaseDate")
                val year = if (releaseDate.length >= 4) releaseDate.substring(0, 4) else "2026"
                val coverObj = sub.optJSONObject("cover")
                val posterUrl = coverObj?.optString("url")?.ifEmpty { null } ?: sub.optString("cover").ifEmpty { null }
                val rating = sub.optDouble("imdbRating", 0.0).let { if (it > 0) "★ %.1f".format(it) else null }
                mediaItems.add(
                    MediaItem(
                        id = id,
                        title = title,
                        type = type,
                        year = year,
                        posterUrl = posterUrl,
                        provider = "MovieBox",
                        rating = rating,
                    ),
                )
            }

            if (mediaItems.isNotEmpty()) {
                sections.add(HomeSection(title = cleanTitle, items = mediaItems))
            }
        }
        sections
    }
}
