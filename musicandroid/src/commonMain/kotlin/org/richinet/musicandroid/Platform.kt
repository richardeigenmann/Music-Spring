package org.richinet.musicandroid

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

import androidx.compose.runtime.Composable

interface PlaylistSync {
    fun syncTrack(track: Track)
    fun registerPlaylist(tagName: String, tracks: List<Track>)
    // Deprecated: used by old SyncScreen
    fun sync(tagName: String, tracks: List<Track>)
}

interface ImageResolver {
    fun getTrackImageSource(track: Track): Any?
}

interface PictureChecker {
    suspend fun getLocalFiles(): List<String>
    suspend fun deleteLocalFile(fileName: String): Boolean
    suspend fun checkLocalFiles(
        fileNames: List<String>, 
        startIndex: Int = 0,
        onProgress: (Int, Int, String) -> Unit
    ): List<String>
}

expect fun createDataStore(context: Any? = null): DataStore<Preferences>

@Composable
expect fun QrScanner(onResult: (String) -> Unit, onDismiss: () -> Unit)

@Composable
expect fun RequestPermissions(onGranted: () -> Unit, onDenied: () -> Unit)
