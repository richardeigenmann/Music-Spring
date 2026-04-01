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
import io.ktor.utils.io.jvm.javaio.copyTo
import java.io.File

class DownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val client = HttpClient()

    override suspend fun doWork(): Result {
        val trackId = inputData.getLong("trackId", -1L)
        val fileId = inputData.getLong("fileId", -1L)
        val fileName = inputData.getString("fileName") ?: "track_$trackId.mp3"
        val url = inputData.getString("url") ?: return Result.failure()

        if (trackId == -1L || fileId == -1L) return Result.failure()

        return try {
            downloadFile(url, fileName)
            Result.success()
        } catch (e: Exception) {
            Result.failure(workDataOf("error" to e.message))
        }
    }

    private suspend fun downloadFile(url: String, fileName: String) {
        val response = client.get(url) {
            onDownload { bytesSentTotal, contentLength ->
                val progress = if (contentLength > 0) (bytesSentTotal * 100 / contentLength).toInt() else 0
                setProgress(workDataOf("progress" to progress))
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
            contentResolver.openOutputStream(trackUri)?.use { outputStream ->
                bodyChannel.copyTo(outputStream)
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
