package org.richinet.musicandroid

import kotlinx.coroutines.flow.StateFlow

data class PlaybackState(
    val track: Track? = null,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val progress: Float = 0f,
    val duration: Long = 0,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val playlistName: String = "",
    val historyIndex: Int = -1,
    val historySize: Int = 0,
    val cacheProgress: Float = 0f, // 0.0 to 1.0
    val cachedTrackIds: Set<Long> = emptySet(),
    val isDownloading: Boolean = false,
    val cacheNonce: Int = 0,
    val cachedFileNames: Set<String> = emptySet()
)

interface AudioPlayer {
    val playbackState: StateFlow<PlaybackState>
    fun playTrack(track: Track)
    fun playPlaylist(tracks: List<Track>, name: String)
    fun togglePlayPause()
    fun seekTo(position: Long)
    fun skipNext()
    fun skipPrevious()
    fun jumpToQueueItem(index: Int)

    // History/Queue methods
    fun goBackHistory()
    fun goForwardHistory()
    fun getQueue(): List<Track>

    // Cache methods
    fun isCached(track: Track): Boolean
    fun cacheTrack(track: Track)
    fun cacheQueue()
}
