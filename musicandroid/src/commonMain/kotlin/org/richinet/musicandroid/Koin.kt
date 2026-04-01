package org.richinet.musicandroid

import org.koin.dsl.module

fun createCommonModule(baseUrl: String, playlistSync: PlaylistSync, audioPlayer: AudioPlayer) = module {
    single { ApiService(baseUrl) } 
    single { playlistSync }
    single { audioPlayer }
    factory { TrackViewModel(get()) }
}
