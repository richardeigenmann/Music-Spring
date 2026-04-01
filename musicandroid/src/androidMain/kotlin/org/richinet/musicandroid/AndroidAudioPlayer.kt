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

    private var currentTrack: Track? = null

    init {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            controller = controllerFuture?.get()
            controller?.addListener(this)
            updateState()
        }, MoreExecutors.directExecutor())

        // Periodically update progress
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
        _playbackState.value = _playbackState.value.copy(
            isPlaying = player.isPlaying,
            isLoading = player.playbackState == Player.STATE_BUFFERING,
            duration = player.duration.coerceAtLeast(0)
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
        val player = controller ?: return
        val file = track.files.firstOrNull() ?: return
        
        currentTrack = track
        
        val metadata = MediaMetadata.Builder()
            .setTitle(track.trackName)
            .setArtist(track.getArtist())
            .setAlbumTitle(track.getAlbum())
            .build()

        val localUri = findLocalUri(file.fileName)
        val uriToPlay = localUri ?: android.net.Uri.parse(apiService.getStreamUrl(file.fileId))

        if (localUri != null) {
            android.util.Log.i("AndroidAudioPlayer", "Playing local file: ${file.fileName}")
        } else {
            android.util.Log.i("AndroidAudioPlayer", "Streaming remote file: ${file.fileName}")
        }

        val mediaItem = MediaItem.Builder()
            .setMediaId(track.trackId.toString())
            .setUri(uriToPlay)
            .setMediaMetadata(metadata)
            .build()

        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
        
        _playbackState.value = _playbackState.value.copy(track = track)
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

        context.contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media._ID))
                return android.content.ContentUris.withAppendedId(collection, id)
            }
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

    // Player.Listener
    override fun onIsPlayingChanged(isPlaying: Boolean) {
        updateState()
    }

    override fun onPlaybackStateChanged(state: Int) {
        updateState()
    }
}
