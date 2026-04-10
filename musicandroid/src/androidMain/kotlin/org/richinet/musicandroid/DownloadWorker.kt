package org.richinet.musicandroid

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
                val fullUrl = "$url/stream/${task.fileId}"
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

        val bodyChannel = response.bodyAsChannel()
        val contentResolver = applicationContext.contentResolver

        val musicCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val trackDetails = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/Music-Spring")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
        }

        val trackUri = contentResolver.insert(musicCollection, trackDetails)
            ?: throw Exception("Failed to insert MediaStore record")

        try {
            val effectiveContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                applicationContext.createAttributionContext("media_playback")
            } else {
                applicationContext
            }

            effectiveContext.contentResolver.openOutputStream(trackUri)?.use { outputStream ->
                val buffer = ByteArray(8192)
                while (!bodyChannel.isClosedForRead) {
                    val read = bodyChannel.readAvailable(buffer)
                    if (read > 0) {
                        outputStream.write(buffer, 0, read)
                    } else if (read == -1) {
                        break
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                trackDetails.clear()
                trackDetails.put(MediaStore.Audio.Media.IS_PENDING, 0)
                contentResolver.update(trackUri, trackDetails, null, null)
            }
        } catch (e: Exception) {
            contentResolver.delete(trackUri, null, null)
            throw e
        }
    }
}
