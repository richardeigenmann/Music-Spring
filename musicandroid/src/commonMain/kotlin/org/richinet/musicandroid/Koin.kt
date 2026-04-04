package org.richinet.musicandroid

import org.koin.dsl.module

fun createCommonModule(baseUrl: String, playlistSync: PlaylistSync, audioPlayer: AudioPlayer, imageResolver: ImageResolver) = module {
    single { ApiService(baseUrl) } 
    single { playlistSync }
    single { audioPlayer }
    single { imageResolver }
    factory { TrackViewModel(get()) }
}
