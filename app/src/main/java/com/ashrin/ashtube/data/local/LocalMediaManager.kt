package com.ashrin.ashtube.data.local

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.ashrin.ashtube.data.local.entities.LocalMediaEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Handles user-selected local media indexing via Android's Storage Access Framework (SAF).
 *
 * Privacy & Security Guarantees:
 * 1. Only reads URIs explicitly chosen by the user in the system document picker.
 * 2. Never scans broad or unrelated storage.
 * 3. Persists read permission via ContentResolver.takePersistableUriPermission.
 */
class LocalMediaManager(private val context: Context) {

    suspend fun indexSingleFile(uri: Uri): LocalMediaEntity? = withContext(Dispatchers.IO) {
        takePersistablePermission(uri)
        extractMediaMetadata(uri)
    }

    suspend fun indexDirectoryTree(treeUri: Uri): List<LocalMediaEntity> = withContext(Dispatchers.IO) {
        takePersistablePermission(treeUri)
        val entities = mutableListOf<LocalMediaEntity>()

        try {
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
            )

            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)

                while (cursor.moveToNext()) {
                    val childDocId = cursor.getString(idCol)
                    val name = cursor.getString(nameCol) ?: "Video"
                    val mime = cursor.getString(mimeCol) ?: ""
                    val size = if (!cursor.isNull(sizeCol)) cursor.getLong(sizeCol) else 0L

                    val isVideo = mime.startsWith("video/") ||
                        name.endsWith(".mp4", ignoreCase = true) ||
                        name.endsWith(".mkv", ignoreCase = true) ||
                        name.endsWith(".webm", ignoreCase = true) ||
                        name.endsWith(".avi", ignoreCase = true)

                    if (isVideo) {
                        val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childDocId)
                        val entity = extractMediaMetadata(documentUri, fallbackName = name, fallbackSize = size, fallbackMime = mime)
                        if (entity != null) {
                            entities.add(entity)
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Handle gracefully when reading folder documents fails
        }

        entities
    }

    fun releasePermission(uri: Uri) {
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.releasePersistableUriPermission(uri, flags)
        } catch (_: Exception) {
            // Ignore if permission was already revoked or expired
        }
    }

    private fun takePersistablePermission(uri: Uri) {
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (_: Exception) {
            // Some URI providers may not support persistable flags; proceed anyway
        }
    }

    private fun extractMediaMetadata(
        uri: Uri,
        fallbackName: String? = null,
        fallbackSize: Long = 0L,
        fallbackMime: String = "video/*",
    ): LocalMediaEntity? {
        var displayName = fallbackName
        var sizeBytes = fallbackSize

        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex != -1 && !cursor.isNull(nameIndex)) {
                        displayName = cursor.getString(nameIndex)
                    }
                    if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                        sizeBytes = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (_: Exception) {}

        val finalName = displayName ?: uri.lastPathSegment ?: "Local Video"
        var durationMs = 0L
        var resolution: String? = null

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            if (durationStr != null) {
                durationMs = durationStr.toLongOrNull() ?: 0L
            }
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            if (width != null && height != null) {
                resolution = "${width}x${height}"
            }
        } catch (_: Exception) {
            // Fallback gracefully if retriever fails to read codecs
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }

        return LocalMediaEntity(
            uri = uri.toString(),
            displayName = finalName,
            mimeType = fallbackMime,
            sizeBytes = sizeBytes,
            durationMs = durationMs,
            resolution = resolution,
        )
    }
}
