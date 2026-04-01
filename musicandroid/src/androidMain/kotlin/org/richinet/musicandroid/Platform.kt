package org.richinet.musicandroid

import android.content.Context
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class AndroidPlaylistSync(private val context: Context, private val apiService: ApiService) : PlaylistSync {
    private val synchronizer = PlaylistSynchronizer(context, apiService)
    
    override fun sync(tagName: String, tracks: List<Track>) {
        synchronizer.syncPlaylist(tagName, tracks)
    }
}

// We will use Koin to get the dependencies in the actual implementation
// However, 'expect/actual' usually doesn't have easy access to Koin without some boilerplate.
// Let's just define the 'actual' and let Koin provide it.
