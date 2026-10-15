package com.ashrin.ashtube.data

import android.content.Context
import android.net.Uri
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ashrin.ashtube.data.local.AshTubeDatabase
import com.ashrin.ashtube.data.local.LocalMediaManager
import com.ashrin.ashtube.data.local.entities.DownloadEntity
import com.ashrin.ashtube.data.local.entities.FavoriteEntity
import com.ashrin.ashtube.data.local.entities.HistoryEntity
import com.ashrin.ashtube.data.local.entities.LocalMediaEntity
import com.ashrin.ashtube.data.local.preferences.AppPreferences
import com.ashrin.ashtube.download.DownloadWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Unified repository orchestrating Room SQLite persistence, DataStore preferences,
 * Storage Access Framework (SAF) local media management, and the AshTube media engine.
 */
class AshTubeRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val database = AshTubeDatabase.getInstance(appContext)
    private val appPreferences = AppPreferences(appContext)
    private val localMediaManager = LocalMediaManager(appContext)
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    // Reactive Data Flows backed by Room SQLite & DataStore
    val favoritesFlow: Flow<List<FavoriteEntity>> = database.favoriteDao().getAllFavorites()
    val historyFlow: Flow<List<HistoryEntity>> = database.historyDao().getAllHistory()
    val downloadsFlow: Flow<List<DownloadEntity>> = database.downloadDao().getAllDownloads()
    val localMediaFlow: Flow<List<LocalMediaEntity>> = database.localMediaDao().getAllLocalMedia()

    val noticeAcceptedFlow: Flow<Boolean> = appPreferences.noticeAcceptedFlow
    val adultAccessFlow: Flow<Boolean> = appPreferences.adultAccessFlow
    val defaultQualityFlow: Flow<String> = appPreferences.defaultQualityFlow
    val themePreferenceFlow: Flow<String> = appPreferences.themePreferenceFlow

    // Synchronize Room local media with in-memory MockCatalogRepository for player resolution
    init {
        repositoryScope.launch {
            localMediaFlow.collect { entities ->
                for (entity in entities) {
                    MockCatalogRepository.registerLocalVideo(
                        LocalVideoItem(
                            uri = entity.uri,
                            displayName = entity.displayName,
                            durationMs = entity.durationMs,
                            sizeBytes = entity.sizeBytes,
                            resolution = entity.resolution,
                            lastPositionMs = entity.lastPlayedPositionMs,
                        ),
                    )
                }
            }
        }
    }

    // Favourites
    suspend fun isFavorite(mediaId: String): Boolean =
        database.favoriteDao().isFavorite(mediaId)

    suspend fun toggleFavorite(mediaId: String, title: String, type: String, year: String): Boolean {
        val currentlyFav = database.favoriteDao().isFavorite(mediaId)
        if (currentlyFav) {
            database.favoriteDao().deleteById(mediaId)
            MockCatalogRepository.toggleFavorite(mediaId)
            return false
        } else {
            database.favoriteDao().insert(
                FavoriteEntity(
                    mediaId = mediaId,
                    title = title,
                    type = type,
                    year = year,
                ),
            )
            MockCatalogRepository.toggleFavorite(mediaId)
            return true
        }
    }

    suspend fun clearFavorites() {
        database.favoriteDao().clearAll()
        MockCatalogRepository.clearFavorites()
    }

    // History and Watch Progress
    suspend fun recordHistory(
        mediaId: String,
        title: String,
        type: String,
        year: String,
        positionMs: Long = 0L,
        durationMs: Long = 7200000L,
    ) {
        database.historyDao().upsert(
            HistoryEntity(
                mediaId = mediaId,
                title = title,
                type = type,
                year = year,
                positionMs = positionMs,
                durationMs = durationMs,
                lastWatchedAt = System.currentTimeMillis(),
            ),
        )
        MockCatalogRepository.recordProgress(mediaId, positionMs, durationMs)
    }

    suspend fun clearHistory() {
        database.historyDao().clearAll()
        MockCatalogRepository.clearHistory()
    }

    // Download Queue
    suspend fun enqueueDownload(
        mediaId: String,
        title: String,
        quality: String = "1080p",
        streamUrl: String = "",
    ): String {
        val downloadId = "dl-${System.currentTimeMillis()}"
        val entity = DownloadEntity(
            id = downloadId,
            mediaId = mediaId,
            title = title,
            quality = quality,
            progress = 0.0f,
            bytesDownloaded = 0L,
            totalBytes = 850_000_000L,
            state = "DOWNLOADING",
        )
        database.downloadDao().insertOrUpdate(entity)
        MockCatalogRepository.enqueueDownload(mediaId, quality)

        val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(
                workDataOf(
                    DownloadWorker.KEY_DOWNLOAD_ID to downloadId,
                    DownloadWorker.KEY_MEDIA_ID to mediaId,
                    DownloadWorker.KEY_TITLE to title,
                    DownloadWorker.KEY_QUALITY to quality,
                    DownloadWorker.KEY_STREAM_URL to streamUrl,
                ),
            )
            .build()

        WorkManager.getInstance(appContext).enqueueUniqueWork(
            downloadId,
            ExistingWorkPolicy.REPLACE,
            workRequest,
        )

        return downloadId
    }

    suspend fun pauseDownload(id: String) {
        database.downloadDao().updateState(id, "PAUSED")
        MockCatalogRepository.pauseDownload(id)
    }

    suspend fun resumeDownload(id: String) {
        database.downloadDao().updateState(id, "DOWNLOADING")
        MockCatalogRepository.resumeDownload(id)
    }

    suspend fun retryDownload(id: String) {
        database.downloadDao().updateState(id, "DOWNLOADING")
        MockCatalogRepository.retryDownload(id)
    }

    suspend fun deleteDownload(id: String) {
        database.downloadDao().deleteById(id)
        MockCatalogRepository.removeDownload(id)
    }

    // Local Media SAF Indexing
    suspend fun indexLocalFile(uri: Uri): LocalMediaEntity? {
        val entity = localMediaManager.indexSingleFile(uri) ?: return null
        database.localMediaDao().insert(entity)
        MockCatalogRepository.registerLocalVideo(
            LocalVideoItem(
                uri = entity.uri,
                displayName = entity.displayName,
                durationMs = entity.durationMs,
                sizeBytes = entity.sizeBytes,
                resolution = entity.resolution,
                lastPositionMs = 0L,
            ),
        )
        return entity
    }

    suspend fun indexLocalDirectory(treeUri: Uri): List<LocalMediaEntity> {
        val entities = localMediaManager.indexDirectoryTree(treeUri)
        if (entities.isNotEmpty()) {
            database.localMediaDao().insertAll(entities)
            for (entity in entities) {
                MockCatalogRepository.registerLocalVideo(
                    LocalVideoItem(
                        uri = entity.uri,
                        displayName = entity.displayName,
                        durationMs = entity.durationMs,
                        sizeBytes = entity.sizeBytes,
                        resolution = entity.resolution,
                        lastPositionMs = 0L,
                    ),
                )
            }
        }
        return entities
    }

    suspend fun removeLocalMedia(uri: String) {
        database.localMediaDao().deleteByUri(uri)
        MockCatalogRepository.removeLocalVideo(uri)
        localMediaManager.releasePermission(Uri.parse(uri))
    }

    suspend fun updateLocalProgress(uri: String, positionMs: Long) {
        database.localMediaDao().updateProgress(uri, positionMs)
        MockCatalogRepository.recordProgress(uri, positionMs, 0L)
    }

    // Preferences & Gates
    suspend fun setNoticeAccepted(accepted: Boolean) {
        appPreferences.setNoticeAccepted(accepted)
    }

    suspend fun setAdultAccess(enabled: Boolean) {
        appPreferences.setAdultAccessEnabled(enabled)
        MockCatalogRepository.setAdultAccessEnabled(enabled)
    }

    suspend fun setDefaultQuality(quality: String) {
        appPreferences.setDefaultQuality(quality)
        MockCatalogRepository.setDefaultQuality(quality)
    }

    suspend fun setThemePreference(theme: String) {
        appPreferences.setThemePreference(theme)
        MockCatalogRepository.setThemePreference(theme)
    }

    companion object {
        @Volatile
        private var INSTANCE: AshTubeRepository? = null

        fun getInstance(context: Context): AshTubeRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = AshTubeRepository(context)
                INSTANCE = instance
                instance
            }
        }
    }
}
