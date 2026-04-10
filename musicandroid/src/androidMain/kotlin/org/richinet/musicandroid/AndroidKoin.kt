package org.richinet.musicandroid

import org.koin.dsl.module
import android.content.Context

fun createAndroidModule(context: Context) = module {
    single<SettingsRepository> { SettingsRepository(createDataStore(context)) }
    single<ApiService> { ApiService(get()) }
    single<PlaylistSync> { AndroidPlaylistSync(get(), get()) }
    single<AudioPlayer> { AndroidAudioPlayer(get(), get()) }
    single<ImageResolver> { AndroidImageResolver(get(), get()) }
}
