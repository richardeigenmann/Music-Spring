package org.richinet.musicandroid

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import io.ktor.client.HttpClient
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.contentLength
import io.ktor.utils.io.readAvailable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.io.OutputStream

class DownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val client: HttpClient by inject()
    private val queueManager: DownloadQueueManager by inject()
    private val localFileResolver: LocalFileResolver by inject()

    override suspend fun doWork(): Result {
        while (true) {
            val task = queueManager.next() ?: break

            queueManager.markActive(true)
            try {
                val url = inputData.getString("baseUrl") ?: ""
                val fullUrl = "$url/api/trackFile/${task.fileId}"
                android.util.Log.d("DownloadWorker", "Starting download: $fullUrl")
                downloadFile(fullUrl, task.fileName, task.trackId)

                // Set progress with the trackId so AndroidAudioPlayer can update cachedTrackIds
                setProgress(workDataOf("progress" to 100, "trackId" to task.trackId))
            } catch (e: Exception) {
                // Handle retry or failure
                android.util.Log.e("DownloadWorker", "Failed to download ${task.fileName}", e)
            } finally {
                queueManager.markActive(false)
            }
        }
        return Result.success()
    }

    private suspend fun downloadFile(url: String, fileName: String, trackId: Long) {
        val response = client.get(url) {
            onDownload { bytesSentTotal, contentLength ->
                val progress = if (contentLength != null && contentLength > 0) (bytesSentTotal * 100 / contentLength).toInt() else 0
                setProgress(workDataOf("progress" to progress, "trackId" to trackId))
            }
        }

        // 1. Validate Response Status
        if (response.status != HttpStatusCode.OK && response.status != HttpStatusCode.PartialContent) {
            throw Exception("Server returned ${response.status} for $url")
        }

        // 2. Validate Content Type (avoid saving JSON/HTML errors as MP3)
        val contentType = response.contentType()
        if (contentType != null && !contentType.match(ContentType.Audio.MPEG) && 
            !contentType.match(ContentType.Application.OctetStream) &&
            (contentType.match(ContentType.Application.Json) || contentType.match(ContentType.Text.Html))) {
             throw Exception("Unexpected content type $contentType for audio stream")
        }

        // 3. Validate Minimum Length (MP3s are rarely < 1KB, errors often are)
        val expectedLength = response.contentLength() ?: -1L
        if (expectedLength >= 0 && expectedLength < 1024) {
             throw Exception("Response size too small to be audio ($expectedLength bytes)")
        }

        val bodyChannel = response.bodyAsChannel()
        val contentResolver = applicationContext.contentResolver

        val musicCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        // 4. Atomic-like operation: Delete existing, then insert, then write, then commit
        deleteExistingFile(fileName, musicCollection)

        val trackDetails = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/Music-Spring")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
        }

        var trackUri: Uri? = null
        try {
            trackUri = contentResolver.insert(musicCollection, trackDetails)
                ?: throw Exception("Failed to insert MediaStore record")

            val effectiveContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                applicationContext.createAttributionContext("media_playback")
            } else {
                applicationContext
            }

            var totalBytesWritten = 0L
            effectiveContext.contentResolver.openOutputStream(trackUri!!)?.use { outputStream ->
                val buffer = ByteArray(16384)
                while (!bodyChannel.isClosedForRead) {
                    val read = bodyChannel.readAvailable(buffer)
                    if (read > 0) {
                        outputStream.write(buffer, 0, read)
                        totalBytesWritten += read
                    } else if (read == -1) {
                        break
                    }
                }
            }

            // Final sanity check on written data
            if (totalBytesWritten < 1024 && expectedLength != 0L) {
                throw Exception("Download interrupted: only $totalBytesWritten bytes written")
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                trackDetails.clear()
                trackDetails.put(MediaStore.Audio.Media.IS_PENDING, 0)
                contentResolver.update(trackUri!!, trackDetails, null, null)
            }
            
            localFileResolver.refreshCache()
            android.util.Log.i("DownloadWorker", "Successfully downloaded $fileName ($totalBytesWritten bytes)")
        } catch (e: Exception) {
            // CRITICAL: Clean up the record/file if anything went wrong during the transfer
            trackUri?.let { uri ->
                try {
                    contentResolver.delete(uri, null, null)
                } catch (cleanupEx: Exception) {
                    android.util.Log.e("DownloadWorker", "Failed to cleanup partial file: ${cleanupEx.message}")
                }
            }
            throw e
        }
    }

    private fun deleteExistingFile(fileName: String, collection: Uri) {
        val contentResolver = applicationContext.contentResolver
        val projection = arrayOf(MediaStore.Audio.Media._ID)
        val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${MediaStore.Audio.Media.DISPLAY_NAME} = ? AND (${MediaStore.Audio.Media.RELATIVE_PATH} = ? OR ${MediaStore.Audio.Media.RELATIVE_PATH} = ?)"
        } else {
            "${MediaStore.Audio.Media.DISPLAY_NAME} = ?"
        }
        val selectionArgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(fileName, "Music/Music-Spring/", "Music/Music-Spring")
        } else {
            arrayOf(fileName)
        }

        try {
            contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val uri = ContentUris.withAppendedId(collection, id)
                    contentResolver.delete(uri, null, null)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DownloadWorker", "Error deleting existing file $fileName", e)
        }
    }
}
