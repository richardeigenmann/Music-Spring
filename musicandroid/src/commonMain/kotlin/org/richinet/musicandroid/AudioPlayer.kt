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
    val historySize: Int = 0
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
}
