package org.richinet.musicandroid

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AndroidAudioPlayer(
    private val context: Context,
    private val apiService: ApiService
) : AudioPlayer, Player.Listener {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val _playbackState = MutableStateFlow(PlaybackState())
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val history = mutableListOf<List<Track>>()
    private var historyIndex = -1
    private var currentPlaylist = listOf<Track>()
    private var currentPlaylistName = ""

    init {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            controller = controllerFuture?.get()
            controller?.addListener(this)
            updateState()
        }, MoreExecutors.directExecutor())

        scope.launch {
            while (true) {
                if (_playbackState.value.isPlaying) {
                    updateProgress()
                }
                delay(1000)
            }
        }
    }

    private fun updateState() {
        val player = controller ?: return
        val currentMediaItem = player.currentMediaItem
        val trackId = currentMediaItem?.mediaId?.toLongOrNull()
        val track = currentPlaylist.find { it.trackId == trackId }

        _playbackState.value = _playbackState.value.copy(
            track = track,
            isPlaying = player.isPlaying,
            isLoading = player.playbackState == Player.STATE_BUFFERING,
            duration = player.duration.coerceAtLeast(0),
            hasNext = player.hasNextMediaItem(),
            hasPrevious = player.hasPreviousMediaItem(),
            playlistName = currentPlaylistName,
            historyIndex = historyIndex,
            historySize = history.size
        )
    }

    private fun updateProgress() {
        val player = controller ?: return
        if (player.duration > 0) {
            _playbackState.value = _playbackState.value.copy(
                progress = player.currentPosition.toFloat() / player.duration.toFloat(),
                duration = player.duration
            )
        }
    }

    override fun playTrack(track: Track) {
        playPlaylist(listOf(track), "Single Track")
    }

    override fun playPlaylist(tracks: List<Track>, name: String) {
        val player = controller ?: return

        // Add to history
        if (historyIndex < history.size - 1) {
            // Truncate future history if we are in the middle
            val newHistory = history.take(historyIndex + 1).toMutableList()
            newHistory.add(tracks)
            history.clear()
            history.addAll(newHistory)
        } else {
            history.add(tracks)
            if (history.size > 10) history.removeAt(0)
        }
        historyIndex = history.size - 1

        currentPlaylist = tracks
        currentPlaylistName = name

        player.stop()
        player.clearMediaItems()

        val mediaItems = tracks.map { track ->
            val file = track.files.firstOrNull()
            val metadata = MediaMetadata.Builder()
                .setTitle(track.trackName)
                .setArtist(track.getArtist())
                .setAlbumTitle(track.getAlbum())
                .build()

            val uri = if (file != null) {
                findLocalUri(file.fileName) ?: android.net.Uri.parse(apiService.getStreamUrl(file.fileId))
            } else {
                android.net.Uri.EMPTY
            }

            MediaItem.Builder()
                .setMediaId(track.trackId.toString())
                .setUri(uri)
                .setMediaMetadata(metadata)
                .build()
        }

        player.addMediaItems(mediaItems)
        player.prepare()
        player.play()
        updateState()
    }

    private fun findLocalUri(fileName: String): android.net.Uri? {
        val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            android.provider.MediaStore.Audio.Media.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(android.provider.MediaStore.Audio.Media._ID)
        val selection = "${android.provider.MediaStore.Audio.Media.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(fileName)

        try {
            context.contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media._ID))
                    return android.content.ContentUris.withAppendedId(collection, id)
                }
            }
        } catch (e: Exception) {
            // Log error
        }
        return null
    }

    override fun togglePlayPause() {
        val player = controller ?: return
        if (player.isPlaying) player.pause() else player.play()
    }

    override fun seekTo(position: Long) {
        controller?.seekTo(position)
    }

    override fun skipNext() {
        controller?.seekToNext()
    }

    override fun skipPrevious() {
        controller?.seekToPrevious()
    }

    override fun jumpToQueueItem(index: Int) {
        val player = controller ?: return
        if (index in 0 until currentPlaylist.size) {
            player.seekTo(index, 0)
            player.play()
        }
    }

    override fun goBackHistory() {
        if (historyIndex > 0) {
            historyIndex--
            val tracks = history[historyIndex]
            playHistoryPlaylist(tracks)
        }
    }

    override fun goForwardHistory() {
        if (historyIndex < history.size - 1) {
            historyIndex++
            val tracks = history[historyIndex]
            playHistoryPlaylist(tracks)
        }
    }

    private fun playHistoryPlaylist(tracks: List<Track>) {
        val player = controller ?: return
        currentPlaylist = tracks
        // We don't change the name for now, or could store it in history

        player.stop()
        player.clearMediaItems()
        val mediaItems = tracks.map { track ->
            val file = track.files.firstOrNull()
            val uri = if (file != null) {
                findLocalUri(file.fileName) ?: android.net.Uri.parse(apiService.getStreamUrl(file.fileId))
            } else { android.net.Uri.EMPTY }

            MediaItem.Builder()
                .setMediaId(track.trackId.toString())
                .setUri(uri)
                .build()
        }
        player.addMediaItems(mediaItems)
        player.prepare()
        player.play()
        updateState()
    }

    override fun getQueue(): List<Track> = currentPlaylist

    // Player.Listener
    override fun onIsPlayingChanged(isPlaying: Boolean) {
        updateState()
    }

    override fun onPlaybackStateChanged(state: Int) {
        updateState()
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        updateState()
    }
}
