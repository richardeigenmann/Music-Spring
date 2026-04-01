package org.richinet.musicandroid

interface PlaylistSync {
    fun sync(tagName: String, tracks: List<Track>)
}
