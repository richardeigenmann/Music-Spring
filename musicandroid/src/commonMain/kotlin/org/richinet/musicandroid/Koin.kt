package org.richinet.musicandroid

import org.koin.dsl.module

fun createCommonModule() = module {
    single { MusicRepository(get(), get()) }
    factory { TrackViewModel(get(), get()) }
    factory { SettingsScreenModel(get()) }
}
