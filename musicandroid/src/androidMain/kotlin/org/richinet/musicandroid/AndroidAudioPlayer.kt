package org.richinet.musicandroid

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
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
    private val apiService: ApiService,
    private val localFileResolver: LocalFileResolver,
    private val queueManager: DownloadQueueManager
) : AudioPlayer, Player.Listener {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val workManager = WorkManager.getInstance(context)

    private val _playbackState = MutableStateFlow(PlaybackState())
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val history = mutableListOf<List<Track>>()
    private var historyIndex = -1
    private var currentPlaylist = listOf<Track>()
    private var currentPlaylistName = ""

    private var cacheUpdateJob: Job? = null

    init {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            controller = controllerFuture?.get()
            controller?.addListener(this)
            updateState()
        }, MoreExecutors.directExecutor())

        // Observe LocalFileResolver for cache changes
        scope.launch {
            localFileResolver.foundFileNames.collect { names ->
                _playbackState.value = _playbackState.value.copy(
                    cachedFileNames = names,
                    cacheNonce = _playbackState.value.cacheNonce + 1
                )
                updateCacheProgress()
            }
        }

        // Observe WorkManager for downloads
        scope.launch {
            workManager.getWorkInfosByTagFlow("track_download").collect { workInfos ->
                val isDownloading = workInfos.any { it.state == androidx.work.WorkInfo.State.ENQUEUED || it.state == androidx.work.WorkInfo.State.RUNNING }

                // Refresh local file resolver cache if any download succeeded
                if (workInfos.any { it.state == androidx.work.WorkInfo.State.SUCCEEDED }) {
                    localFileResolver.refreshCache()
                }

                _playbackState.value = _playbackState.value.copy(
                    isDownloading = isDownloading
                )
            }
        }

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

        // Update cache progress asynchronously to avoid blocking Main thread
        // Cancel previous job if it exists to avoid piled up background work
        cacheUpdateJob?.cancel()
        cacheUpdateJob = scope.launch(Dispatchers.IO) {
            updateCacheProgress()
        }
    }

    private fun updateCacheProgress() {
        if (currentPlaylist.isEmpty()) {
            _playbackState.value = _playbackState.value.copy(cacheProgress = 0f)
            return
        }

        val cachedCount = currentPlaylist.count { isCached(it) }
        val progress = cachedCount.toFloat() / currentPlaylist.size.toFloat()

        _playbackState.value = _playbackState.value.copy(cacheProgress = progress)
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

        // Move media item creation to background thread
        scope.launch(Dispatchers.Default) {
            val mediaItems = tracks.map { track ->
                val file = track.files.firstOrNull()
                val metadata = MediaMetadata.Builder()
                    .setTitle(track.trackName)
                    .setArtist(track.getArtist())
                    .setAlbumTitle(track.getAlbum())
                    .build()

                val uri = if (file != null) {
                    localFileResolver.findLocalUri(file.fileName) ?: android.net.Uri.parse(apiService.getStreamUrl(file.fileId))
                } else {
                    android.net.Uri.EMPTY
                }

                MediaItem.Builder()
                    .setMediaId(track.trackId.toString())
                    .setUri(uri)
                    .setMediaMetadata(metadata)
                    .build()
            }

            launch(Dispatchers.Main) {
                player.addMediaItems(mediaItems)
                player.prepare()
                player.play()
                updateState()

                // Cache the first few tracks
                tracks.take(3).forEach { cacheTrack(it) }
            }
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
                localFileResolver.findLocalUri(file.fileName) ?: android.net.Uri.parse(apiService.getStreamUrl(file.fileId))
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

    override fun isCached(track: Track): Boolean {
        val file = track.files.firstOrNull() ?: return false
        val uri = localFileResolver.findLocalUri(file.fileName)
        return uri != null
    }

    override fun cacheTrack(track: Track) {
        if (isCached(track)) {
            updateState()
            return
        }

        queueManager.enqueue(listOf(track))
        startWork()
    }

    private fun startWork() {
        val downloadRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .addTag("track_download")
            .setInputData(Data.Builder()
                .putString("baseUrl", apiService.baseUrl)
                .build())
            .build()

        // Start 3 parallel workers
        for (i in 1..3) {
            workManager.enqueueUniqueWork(
                "downloader_$i",
                androidx.work.ExistingWorkPolicy.KEEP,
                downloadRequest
            )
        }
    }

    override fun cacheQueue() {
        currentPlaylist.forEach { cacheTrack(it) }
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

        // Lookahead caching: cache the next 3 tracks
        val currentIndex = controller?.currentMediaItemIndex ?: return
        if (currentIndex >= 0) {
            for (i in 1..3) {
                val nextIndex = currentIndex + i
                if (nextIndex < currentPlaylist.size) {
                    cacheTrack(currentPlaylist[nextIndex])
                }
            }
        }
    }
}
