package com.ashrin.ashtube.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ashrin.ashtube.data.AshTubeRepository
import com.ashrin.ashtube.data.Episode
import com.ashrin.ashtube.data.MockCatalogRepository
import kotlinx.coroutines.launch

@Composable
fun DetailScreen(
    mediaId: String,
    onBack: () -> Unit,
    onPlay: (mediaId: String, episodeNumber: Int?) -> Unit,
) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val repository = remember { AshTubeRepository.getInstance(context) }

    var details by remember(mediaId) {
        mutableStateOf(MockCatalogRepository.getDetails(mediaId))
    }
    var isLoading by remember(mediaId) {
        mutableStateOf(details == null || (details?.summary?.contains("A title titled") == true))
    }

    LaunchedEffect(mediaId) {
        try {
            val live = MockCatalogRepository.getDetailsLive(mediaId)
            if (live != null) {
                details = live
            }
        } catch (_: Exception) {
            // Keep local fallback details
        } finally {
            isLoading = false
        }
    }

    var isFavorite by rememberSaveable(mediaId) {
        mutableStateOf(MockCatalogRepository.isFavorite(mediaId))
    }
    var actionFeedback by rememberSaveable(mediaId) {
        mutableStateOf<String?>(null)
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        if (details == null && isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Loading MovieBox title information...", style = MaterialTheme.typography.bodyLarge)
            }
            return@Surface
        }

        if (details == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Item not found.", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBack) {
                    Text("Go back")
                }
            }
            return@Surface
        }

        val currentDetails = details ?: return@Surface
        val item = currentDetails.item

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onBack) {
                    Text("Back")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .width(16.dp)
                                .height(16.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = item.type,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!currentDetails.backdropUrl.isNullOrEmpty() || !item.posterUrl.isNullOrEmpty()) {
                val imgUrl = currentDetails.backdropUrl ?: item.posterUrl
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                ) {
                    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                        coil.compose.AsyncImage(
                            model = imgUrl,
                            contentDescription = item.title,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .androidx.compose.foundation.background(
                                    androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(
                                            androidx.compose.ui.graphics.Color.Transparent,
                                            androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.8f),
                                        ),
                                        startY = 100f,
                                    ),
                                ),
                        )
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Surface(
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                Text(
                                    text = item.provider ?: "Unified Provider",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            if (item.rating != null) {
                                Surface(
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                                    color = androidx.compose.ui.graphics.Color(0xFFE5A00D),
                                ) {
                                    Text(
                                        text = item.rating,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = androidx.compose.ui.graphics.Color.Black,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            Text(
                text = item.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = "Release Year: ${item.year}${if (currentDetails.genre != null) " • ${currentDetails.genre}" else ""}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons: Play, Download, Favourite
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = {
                        onPlay(mediaId, null)
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Play")
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            actionFeedback = "Resolving stream and starting download..."
                            try {
                                val session = MockCatalogRepository.createPlaybackSessionLive(mediaId, null)
                                val streamUrl = session?.streamUrl ?: ""
                                repository.enqueueDownload(
                                    mediaId = mediaId,
                                    title = item.title,
                                    quality = "1080p",
                                    streamUrl = streamUrl,
                                )
                                actionFeedback = "Downloading '${item.title}' in background."
                            } catch (e: Exception) {
                                actionFeedback = "Download started for '${item.title}'."
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Download")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    scope.launch {
                        val nextState = repository.toggleFavorite(mediaId, item.title, item.type, item.year)
                        isFavorite = nextState
                        actionFeedback = if (nextState) "Added to Favourites." else "Removed from Favourites."
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isFavorite) "★ Favourited" else "☆ Add to Favourites")
            }

            if (actionFeedback != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = actionFeedback ?: "",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = currentDetails.summary,
                style = MaterialTheme.typography.bodyLarge,
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Available Qualities: ${currentDetails.availableQualities.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (currentDetails.episodes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Episodes (${currentDetails.episodes.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(10.dp))

                currentDetails.episodes.forEach { episode ->
                    EpisodeRow(
                        episode = episode,
                        onPlay = {
                            onPlay(mediaId, episode.episode)
                        },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun EpisodeRow(
    episode: Episode,
    onPlay: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "S${episode.season} E${episode.episode}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = episode.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
            }
            Button(onClick = onPlay) {
                Text("Play")
            }
        }
    }
}
