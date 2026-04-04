package org.richinet.musicandroid

import android.content.Context
import android.net.Uri
import android.content.ContentUris
import android.provider.MediaStore

class AndroidPlaylistSync(private val context: Context, private val apiService: ApiService) : PlaylistSync {
    private val synchronizer = PlaylistSynchronizer(context, apiService)
    
    override fun sync(tagName: String, tracks: List<Track>) {
        synchronizer.syncPlaylist(tagName, tracks)
    }
}

class AndroidImageResolver(private val context: Context, private val apiService: ApiService) : ImageResolver {
    override fun getTrackImageSource(track: Track): Any? {
        val file = track.files.firstOrNull() ?: return null
        
        // 1. Try to find local file in MediaStore
        val localUri = findLocalUri(file.fileName)
        if (localUri != null) return localUri
        
        // 2. Fallback to network URL
        return apiService.getTrackImageUrl(file.fileId)
    }

    private fun findLocalUri(fileName: String): Uri? {
        val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(MediaStore.Audio.Media._ID)
        val selection = "${MediaStore.Audio.Media.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(fileName)

        try {
            context.contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                    return ContentUris.withAppendedId(collection, id)
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        return null
    }
}
