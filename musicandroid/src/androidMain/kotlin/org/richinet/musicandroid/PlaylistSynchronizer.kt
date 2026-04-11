package org.richinet.musicandroid

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.io.File
import android.os.Environment

class PlaylistSynchronizer(
    private val context: Context,
    private val apiService: ApiService,
    private val localFileResolver: LocalFileResolver
) {

    fun syncTrack(track: Track) {
        val workManager = WorkManager.getInstance(context)
        val file = track.files.firstOrNull() ?: return
        
        val downloadRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .addTag("track_download")
            .setInputData(Data.Builder()
                .putLong("trackId", track.trackId)
                .putLong("fileId", file.fileId)
                .putString("fileName", file.fileName)
                .putString("url", apiService.getStreamUrl(file.fileId))
                .build())
            .build()

        // Use unique work to queue them sequentially for the same file
        workManager.enqueueUniqueWork(
            "download_${file.fileName}",
            androidx.work.ExistingWorkPolicy.KEEP,
            downloadRequest
        )
    }

    fun registerPlaylist(tagName: String, tracks: List<Track>) {
        val m3uContent = StringBuilder("#EXTM3U\n")
        tracks.forEach { track ->
            val file = track.files.firstOrNull() ?: return@forEach
            m3uContent.append("#EXTINF:${file.duration.toLong()},${track.getArtist()} - ${track.trackName}\n")
            m3uContent.append("${file.fileName}\n")
        }

        val fileName = "$tagName.m3u"
        val contentResolver = context.contentResolver

        // Use MediaStore.Audio.Playlists for better visibility in music players
        val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            android.provider.MediaStore.Audio.Playlists.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            android.provider.MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI
        }

        val details = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Audio.Playlists.DISPLAY_NAME, fileName)
            put(android.provider.MediaStore.Audio.Playlists.MIME_TYPE, "audio/x-mpegurl")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(android.provider.MediaStore.Audio.Playlists.RELATIVE_PATH, "Music/Music-Spring")
            }
        }

        // Delete existing playlist with the same name if it exists
        contentResolver.delete(collection, "${android.provider.MediaStore.Audio.Playlists.DISPLAY_NAME} = ?", arrayOf(fileName))

        val uri = contentResolver.insert(collection, details) ?: return

        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(m3uContent.toString().toByteArray())
            }
        } catch (e: Exception) {
            android.util.Log.e("PlaylistSynchronizer", "Failed to write playlist: ${e.message}")
        }
    }

    fun syncPlaylist(tagName: String, tracks: List<Track>) {
        tracks.forEach { syncTrack(it) }
        registerPlaylist(tagName, tracks)
    }
}
