package org.richinet.musicandroid

import org.koin.dsl.module

fun createCommonModule(
    settingsRepository: SettingsRepository,
    apiService: ApiService,
    playlistSync: PlaylistSync,
    audioPlayer: AudioPlayer,
    imageResolver: ImageResolver
) = module {
    single { settingsRepository }
    single { apiService } 
    single { playlistSync }
    single { audioPlayer }
    single { imageResolver }
    factory { TrackViewModel(get()) }
    factory { SettingsScreenModel(get()) }
}
