package com.ashrin.ashtube.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as Media3MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.ashrin.ashtube.data.MockCatalogRepository
import com.ashrin.ashtube.data.PlaybackSession
import com.ashrin.ashtube.data.SubtitleTrack
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    mediaId: String,
    initialEpisodeNumber: Int? = null,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var activeEpisodeNumber by rememberSaveable(mediaId) { mutableStateOf(initialEpisodeNumber) }
    var session by remember(mediaId, activeEpisodeNumber) {
        mutableStateOf(MockCatalogRepository.createPlaybackSession(mediaId, activeEpisodeNumber))
    }
    var isLoadingSession by remember(mediaId, activeEpisodeNumber) {
        mutableStateOf(session == null || session?.streamUrl == null)
    }

    LaunchedEffect(mediaId, activeEpisodeNumber) {
        try {
            val liveSession = MockCatalogRepository.createPlaybackSessionLive(mediaId, activeEpisodeNumber)
            if (liveSession != null) {
                session = liveSession
            }
        } catch (_: Exception) {
            // Keep local mock session
        } finally {
            isLoadingSession = false
        }
    }

    if (session == null && isLoadingSession) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Resolving MovieBox media stream...", style = MaterialTheme.typography.titleMedium)
            }
        }
        return
    }

    if (session == null) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Unable to load media stream.", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBack) {
                    Text("Return")
                }
            }
        }
        return
    }

    val currentSession = session!!
    var isPlaying by rememberSaveable(mediaId, activeEpisodeNumber) { mutableStateOf(true) }
    var currentPositionMs by rememberSaveable(mediaId, activeEpisodeNumber) {
        mutableLongStateOf(currentSession.startPositionMs)
    }
    var selectedQuality by rememberSaveable { mutableStateOf(MockCatalogRepository.getDefaultQuality()) }
    var selectedSubtitle by rememberSaveable { mutableStateOf("Off") }
    var showQualityDialog by rememberSaveable { mutableStateOf(false) }
    var showSubtitleDialog by rememberSaveable { mutableStateOf(false) }
    var showControls by rememberSaveable { mutableStateOf(true) }

    // Media3 ExoPlayer instance for live stream playback
    val exoPlayer = remember(context, currentSession.streamUrl) {
        val url = currentSession.streamUrl
        if (!url.isNullOrEmpty()) {
            ExoPlayer.Builder(context).build().apply {
                val mediaItem = Media3MediaItem.fromUri(url)
                setMediaItem(mediaItem)
                if (currentPositionMs > 0L) {
                    seekTo(currentPositionMs)
                }
                prepare()
                playWhenReady = isPlaying
            }
        } else null
    }

    // Sync isPlaying state with ExoPlayer
    LaunchedEffect(exoPlayer, isPlaying) {
        exoPlayer?.playWhenReady = isPlaying
    }

    // Progress updater for ExoPlayer and simulation fallback
    LaunchedEffect(exoPlayer, isPlaying, mediaId, activeEpisodeNumber) {
        while (isPlaying) {
            delay(1000L)
            if (exoPlayer != null) {
                if (exoPlayer.isPlaying) {
                    currentPositionMs = exoPlayer.currentPosition
                    val duration = if (exoPlayer.duration > 0) exoPlayer.duration else currentSession.durationMs
                    MockCatalogRepository.recordProgress(mediaId, currentPositionMs, duration)
                }
            } else {
                if (currentPositionMs + 1000L <= currentSession.durationMs) {
                    currentPositionMs += 1000L
                    MockCatalogRepository.recordProgress(mediaId, currentPositionMs, currentSession.durationMs)
                } else {
                    isPlaying = false
                    break
                }
            }
        }
    }

    // Persist position and release player when leaving
    DisposableEffect(mediaId, activeEpisodeNumber, exoPlayer) {
        onDispose {
            MockCatalogRepository.recordProgress(mediaId, currentPositionMs, currentSession.durationMs)
            exoPlayer?.release()
        }
    }

    val nextEpisode = if (activeEpisodeNumber != null) {
        MockCatalogRepository.getNextEpisode(mediaId, activeEpisodeNumber!!)
    } else null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { showControls = !showControls },
    ) {
        if (exoPlayer != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            // Video viewport simulation canvas
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = currentSession.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Bold,
                )
                if (currentSession.episodeTitle != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "S${currentSession.seasonNumber}E${currentSession.episodeNumber} • ${currentSession.episodeTitle}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Stream Quality: $selectedQuality • Subtitles: $selectedSubtitle",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f),
                )
                if (selectedSubtitle != "Off") {
                    Spacer(modifier = Modifier.height(32.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f)),
                    ) {
                        Text(
                            text = "[Subtitle: Stream track for '${currentSession.title}']",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Yellow,
                        )
                    }
                }
            }
        }

        // Overlay Player Controls
        if (showControls) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Top control bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = {
                        MockCatalogRepository.recordProgress(mediaId, currentPositionMs, currentSession.durationMs)
                        exoPlayer?.stop()
                        onBack()
                    }) {
                        Text("← Back", color = Color.White)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isLoadingSession) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .width(16.dp)
                                    .height(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        TextButton(onClick = { showSubtitleDialog = true }) {
                            Text("Subtitles: $selectedSubtitle", color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { showQualityDialog = true }) {
                            Text(selectedQuality, color = Color.White)
                        }
                    }
                }

                // Center playback controls: Rewind 10s, Play/Pause, Forward 10s
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = {
                            currentPositionMs = (currentPositionMs - 10000L).coerceAtLeast(0L)
                            exoPlayer?.seekTo(currentPositionMs)
                        },
                    ) {
                        Text("-10s")
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Button(
                        onClick = {
                            isPlaying = !isPlaying
                            exoPlayer?.playWhenReady = isPlaying
                        },
                    ) {
                        Text(if (isPlaying) "Pause" else "Play")
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Button(
                        onClick = {
                            currentPositionMs = (currentPositionMs + 10000L).coerceAtMost(currentSession.durationMs)
                            exoPlayer?.seekTo(currentPositionMs)
                        },
                    ) {
                        Text("+10s")
                    }
                }

                // Bottom scrubber and episode continuation bar
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (nextEpisode != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            Button(
                                onClick = {
                                    MockCatalogRepository.recordProgress(mediaId, currentPositionMs, currentSession.durationMs)
                                    exoPlayer?.stop()
                                    activeEpisodeNumber = nextEpisode.episode
                                    currentPositionMs = 0L
                                    isPlaying = true
                                },
                            ) {
                                Text("Next: E${nextEpisode.episode} ${nextEpisode.title}")
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = formatTime(currentPositionMs),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                        )
                        Text(
                            text = formatTime(currentSession.durationMs),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                        )
                    }

                    Slider(
                        value = if (currentSession.durationMs > 0) currentPositionMs.toFloat() / currentSession.durationMs else 0f,
                        onValueChange = { fraction ->
                            currentPositionMs = (fraction * currentSession.durationMs).toLong()
                            exoPlayer?.seekTo(currentPositionMs)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    // Quality Selector Dialog
    if (showQualityDialog) {
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = { Text("Select Playback Quality") },
            text = {
                Column {
                    currentSession.availableQualities.forEach { quality ->
                        TextButton(
                            onClick = {
                                selectedQuality = quality
                                showQualityDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(quality, fontWeight = if (quality == selectedQuality) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualityDialog = false }) {
                    Text("Close")
                }
            },
        )
    }

    // Subtitle Selector Dialog
    if (showSubtitleDialog) {
        AlertDialog(
            onDismissRequest = { showSubtitleDialog = false },
            title = { Text("Select Subtitle Track") },
            text = {
                Column {
                    currentSession.availableSubtitles.forEach { track ->
                        TextButton(
                            onClick = {
                                selectedSubtitle = track.label
                                showSubtitleDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(track.label, fontWeight = if (track.label == selectedSubtitle) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSubtitleDialog = false }) {
                    Text("Close")
                }
            },
        )
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
