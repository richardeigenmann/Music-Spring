package org.richinet.musicandroid

interface PlaylistSync {
    fun sync(tagName: String, tracks: List<Track>)
}

interface ImageResolver {
    fun getTrackImageSource(track: Track): Any?
}
