package com.ashrin.ashtube.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ashrin.ashtube.data.MockCatalogRepository
import kotlinx.coroutines.delay

class DownloadWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    companion object {
        const val CHANNEL_ID = "ashtube_download_channel"
        const val CHANNEL_NAME = "AshTube Media Downloads"
        const val NOTIFICATION_ID = 2001

        const val KEY_DOWNLOAD_ID = "download_id"
        const val KEY_MEDIA_ID = "media_id"
        const val KEY_TITLE = "title"
        const val KEY_QUALITY = "quality"
    }

    override suspend fun doWork(): Result {
        val downloadId = inputData.getString(KEY_DOWNLOAD_ID) ?: return Result.failure()
        val title = inputData.getString(KEY_TITLE) ?: "AshTube Video"
        val mediaId = inputData.getString(KEY_MEDIA_ID) ?: ""

        createNotificationChannel()

        // Promote to foreground service for long-running compliant download
        val foregroundInfo = createForegroundInfo(title, 0)
        try {
            setForeground(foregroundInfo)
        } catch (_: Exception) {
            // Foreground promote may fail on API 34+ if lacking special foreground service permissions; continue safely
        }

        val totalBytes = 850_000_000L
        var currentBytes = 0L

        // Chunked download loop simulating range downloads
        for (step in 1..10) {
            if (isStopped) {
                MockCatalogRepository.pauseDownload(downloadId)
                return Result.success()
            }

            delay(800L)
            currentBytes = (totalBytes * step) / 10
            val progressPercent = (step * 10)

            MockCatalogRepository.updateDownloadProgress(
                id = downloadId,
                progress = step / 10f,
                bytesDownloaded = currentBytes,
            )

            try {
                setForeground(createForegroundInfo(title, progressPercent))
            } catch (_: Exception) {
            }
        }

        MockCatalogRepository.markDownloadCompleted(downloadId)
        return Result.success(workDataOf("completed" to true, "downloadId" to downloadId))
    }

    private fun createForegroundInfo(title: String, progressPercent: Int): ForegroundInfo {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Downloading $title")
            .setContentText("$progressPercent% completed")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progressPercent, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Shows progress of active AshTube background downloads"
            }
            manager?.createNotificationChannel(channel)
        }
    }
}
