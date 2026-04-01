package org.richinet.musicandroid

import kotlinx.coroutines.flow.StateFlow

data class PlaybackState(
    val track: Track? = null,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val progress: Float = 0f,
    val duration: Long = 0
)

interface AudioPlayer {
    val playbackState: StateFlow<PlaybackState>
    fun playTrack(track: Track)
    fun togglePlayPause()
    fun seekTo(position: Long)
    fun skipNext()
    fun skipPrevious()
}
