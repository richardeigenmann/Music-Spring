package org.richinet.musicandroid

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.io.File
import android.os.Environment

class PlaylistSynchronizer(private val context: Context, private val apiService: ApiService) {

    fun syncPlaylist(tagName: String, tracks: List<Track>) {
        val workManager = WorkManager.getInstance(context)

        tracks.forEach { track ->
            val file = track.files.firstOrNull() ?: return@forEach
            val downloadRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(Data.Builder()
                    .putLong("trackId", track.trackId)
                    .putLong("fileId", file.fileId)
                    .putString("fileName", file.fileName)
                    .putString("url", apiService.getStreamUrl(file.fileId))
                    .build())
                .build()
            
            workManager.enqueue(downloadRequest)
        }

        registerPlaylistInMediaStore(tagName, tracks)
    }

    private fun registerPlaylistInMediaStore(tagName: String, tracks: List<Track>) {
        val m3uContent = StringBuilder("#EXTM3U\n")
        tracks.forEach { track ->
            val file = track.files.firstOrNull() ?: return@forEach
            m3uContent.append("#EXTINF:${file.duration.toLong()},${track.getArtist()} - ${track.trackName}\n")
            // Use just the file name since we download all files to the same "Music/Music-Spring" folder
            m3uContent.append("${file.fileName}\n")
        }

        val fileName = "$tagName.m3u"
        val contentResolver = context.contentResolver
        
        val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            android.provider.MediaStore.Files.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            android.provider.MediaStore.Files.getContentUri("external")
        }

        val details = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Files.FileColumns.DISPLAY_NAME, fileName)
            put(android.provider.MediaStore.Files.FileColumns.MIME_TYPE, "audio/x-mpegurl")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(android.provider.MediaStore.Files.FileColumns.RELATIVE_PATH, "Music/Music-Spring")
            }
        }

        // Delete existing playlist with the same name if it exists
        contentResolver.delete(collection, "${android.provider.MediaStore.Files.FileColumns.DISPLAY_NAME} = ?", arrayOf(fileName))

        val uri = contentResolver.insert(collection, details) ?: return
        
        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(m3uContent.toString().toByteArray())
            }
        } catch (e: Exception) {
            android.util.Log.e("PlaylistSynchronizer", "Failed to write playlist: ${e.message}")
        }
    }
}
