package com.ashrin.ashtube.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ashrin.ashtube.data.DownloadItem
import com.ashrin.ashtube.data.DownloadState
import com.ashrin.ashtube.data.HistoryEntry
import com.ashrin.ashtube.data.MediaItem
import com.ashrin.ashtube.data.MockCatalogRepository

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.ashrin.ashtube.data.AshTubeRepository
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(
    onOpenDetails: (String) -> Unit,
    onPlayMedia: (String) -> Unit = {},
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("History", "Favourites", "Downloads", "Local")

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Library",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            PrimaryTabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) },
                    )
                }
            }

            when (selectedTab) {
                0 -> HistoryTab(onOpenDetails = onOpenDetails)
                1 -> FavouritesTab(onOpenDetails = onOpenDetails)
                2 -> DownloadsTab(onOpenDetails = onOpenDetails)
                3 -> LocalVideosTab(onPlayMedia = onPlayMedia)
            }
        }
    }
}

@Composable
private fun HistoryTab(onOpenDetails: (String) -> Unit) {
    var history by rememberSaveable { mutableStateOf(MockCatalogRepository.getHistory()) }

    if (history.isEmpty()) {
        EmptyStateView(
            title = "No watch history",
            description = "Titles you play will appear here so you can easily continue watching.",
        )
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${history.size} titles in history",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = {
                        MockCatalogRepository.clearHistory()
                        history = emptyList()
                    },
                ) {
                    Text("Clear history")
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(history, key = HistoryEntry::mediaId) { entry ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenDetails(entry.mediaId) },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = entry.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "${entry.type} • ${entry.year}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = "Resume",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FavouritesTab(onOpenDetails: (String) -> Unit) {
    var favourites by rememberSaveable { mutableStateOf(MockCatalogRepository.getFavorites()) }

    if (favourites.isEmpty()) {
        EmptyStateView(
            title = "No favourites yet",
            description = "Add titles to your favourites from their details page to access them quickly here.",
        )
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${favourites.size} favourite items",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = {
                        MockCatalogRepository.clearFavorites()
                        favourites = emptyList()
                    },
                ) {
                    Text("Clear all")
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(favourites, key = MediaItem::id) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenDetails(item.id) },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "${item.type} • ${item.year}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = "★ Favourited",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadsTab(onOpenDetails: (String) -> Unit) {
    var downloads by rememberSaveable { mutableStateOf(MockCatalogRepository.getDownloads()) }

    if (downloads.isEmpty()) {
        EmptyStateView(
            title = "No downloads in queue",
            description = "Tap 'Download' on any video details screen to prepare files for offline access.",
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(downloads, key = DownloadItem::id) { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                val downloadedMb = item.bytesDownloaded / (1024 * 1024)
                                val totalMb = item.totalBytes / (1024 * 1024)
                                val stateLabel = when (item.state) {
                                    DownloadState.QUEUED -> "Queued"
                                    DownloadState.DOWNLOADING -> "Downloading (${(item.progress * 100).toInt()}%)"
                                    DownloadState.PAUSED -> "Paused"
                                    DownloadState.COMPLETED -> "Completed (Offline)"
                                    DownloadState.FAILED -> "Failed: ${item.error ?: "Network error"}"
                                }
                                Text(
                                    text = "${item.quality} • $stateLabel • $downloadedMb MB / $totalMb MB",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (item.state == DownloadState.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { item.progress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            when (item.state) {
                                DownloadState.DOWNLOADING -> {
                                    TextButton(
                                        onClick = {
                                            MockCatalogRepository.pauseDownload(item.id)
                                            downloads = MockCatalogRepository.getDownloads()
                                        },
                                    ) {
                                        Text("Pause")
                                    }
                                }
                                DownloadState.PAUSED -> {
                                    TextButton(
                                        onClick = {
                                            MockCatalogRepository.resumeDownload(item.id)
                                            downloads = MockCatalogRepository.getDownloads()
                                        },
                                    ) {
                                        Text("Resume")
                                    }
                                }
                                DownloadState.FAILED -> {
                                    TextButton(
                                        onClick = {
                                            MockCatalogRepository.retryDownload(item.id)
                                            downloads = MockCatalogRepository.getDownloads()
                                        },
                                    ) {
                                        Text("Retry")
                                    }
                                }
                                DownloadState.COMPLETED -> {
                                    TextButton(
                                        onClick = {
                                            onOpenDetails(item.mediaId)
                                        },
                                    ) {
                                        Text("Play")
                                    }
                                }
                                DownloadState.QUEUED -> {}
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            TextButton(
                                onClick = {
                                    MockCatalogRepository.removeDownload(item.id)
                                    downloads = MockCatalogRepository.getDownloads()
                                },
                            ) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateView(
    title: String,
    description: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LocalVideosTab(
    onPlayMedia: (String) -> Unit,
) {
    val context = LocalContext.current
    val repository = remember { AshTubeRepository.getInstance(context) }
    val scope = rememberCoroutineScope()
    val localVideos by repository.localMediaFlow.collectAsState(initial = emptyList())

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                repository.indexLocalFile(uri)
            }
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { treeUri ->
        if (treeUri != null) {
            scope.launch {
                repository.indexLocalDirectory(treeUri)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = { filePickerLauncher.launch(arrayOf("video/*")) },
                modifier = Modifier.weight(1f),
            ) {
                Text("+ Add Video")
            }
            OutlinedButton(
                onClick = { folderPickerLauncher.launch(null) },
                modifier = Modifier.weight(1f),
            ) {
                Text("+ Add Folder")
            }
        }

        if (localVideos.isEmpty()) {
            EmptyStateView(
                title = "No local videos added",
                description = "AshTube uses Android's Storage Access Framework (SAF) so you can pick videos or video folders. Only files you explicitly select are indexed; unrelated storage is never scanned.",
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(localVideos, key = { it.uri }) { video ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        ) {
                            Text(
                                text = video.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val detailsText = listOfNotNull(
                                formatFileSize(video.sizeBytes).takeIf { it.isNotEmpty() },
                                formatDuration(video.durationMs).takeIf { it.isNotEmpty() },
                                video.resolution,
                            ).joinToString(" • ")

                            if (detailsText.isNotEmpty()) {
                                Text(
                                    text = detailsText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            if (video.lastPlayedPositionMs > 0 && video.durationMs > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                val progress = (video.lastPlayedPositionMs.toFloat() / video.durationMs.toFloat()).coerceIn(0f, 1f)
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                Button(
                                    onClick = { onPlayMedia(video.uri) },
                                ) {
                                    Text("Play")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            repository.removeLocalMedia(video.uri)
                                        }
                                    },
                                ) {
                                    Text("Remove")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return ""
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1000) {
        String.format(java.util.Locale.US, "%.2f GB", mb / 1024.0)
    } else {
        String.format(java.util.Locale.US, "%.1f MB", mb)
    }
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0) return ""
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(java.util.Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
    }
}
