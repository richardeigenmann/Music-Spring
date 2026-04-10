package org.richinet.musicandroid

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocalFileResolver(private val context: Context) {
    private val _foundFileNames = MutableStateFlow<Set<String>>(emptySet())
    val foundFileNames = _foundFileNames.asStateFlow()

    private val uriCache = ConcurrentHashMap<String, Uri>()
    private var lastScanTime = 0L

    fun findLocalUri(fileName: String): Uri? {
        // Basic refresh logic
        if (uriCache.isEmpty() || System.currentTimeMillis() - lastScanTime > 30000) {
            refreshCache()
        }
        return uriCache[fileName]
    }

    fun refreshCache() {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME
        )

        // Filter by the relative path to limit results
        val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
        } else {
            "${MediaStore.Audio.Media.DATA} LIKE ?"
        }
        val selectionArgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf("%Music/Music-Spring%")
        } else {
            arrayOf("%/Music/Music-Spring/%")
        }

        val effectiveContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.createAttributionContext("media_playback")
        } else {
            context
        }

        try {
            effectiveContext.contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)

                val newCache = mutableMapOf<String, Uri>()
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val uri = ContentUris.withAppendedId(collection, id)
                    newCache[name] = uri
                }
                uriCache.clear()
                uriCache.putAll(newCache)
                _foundFileNames.value = newCache.keys.toSet()
                lastScanTime = System.currentTimeMillis()
                android.util.Log.d("LocalFileResolver", "Cache refreshed: ${newCache.size} files found")
            }
        } catch (e: Exception) {
            android.util.Log.e("LocalFileResolver", "Failed to refresh cache: ${e.message}")
        }
    }

    fun clearCache() {
        uriCache.clear()
        _foundFileNames.value = emptySet()
        lastScanTime = 0
    }
}
