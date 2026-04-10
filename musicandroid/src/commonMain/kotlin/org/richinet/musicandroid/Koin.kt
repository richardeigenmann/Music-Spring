package org.richinet.musicandroid

import org.koin.dsl.module

fun createCommonModule() = module {
    // These will be provided by the platform-specific modules
    // single { settingsRepository }
    // single { apiService }
    // single { playlistSync }
    // single { audioPlayer }
    // single { imageResolver }

    factory { TrackViewModel(get()) }
    factory { SettingsScreenModel(get()) }
}
