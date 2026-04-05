package org.richinet.musicandroid

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

import androidx.compose.runtime.Composable

interface PlaylistSync {
    fun sync(tagName: String, tracks: List<Track>)
}

interface ImageResolver {
    fun getTrackImageSource(track: Track): Any?
}

expect fun createDataStore(context: Any? = null): DataStore<Preferences>

@Composable
expect fun QrScanner(onResult: (String) -> Unit, onDismiss: () -> Unit)
