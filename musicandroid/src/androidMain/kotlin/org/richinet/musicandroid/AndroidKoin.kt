package org.richinet.musicandroid

import org.koin.dsl.module
import android.content.Context
import io.ktor.client.HttpClient

fun createAndroidModule(context: Context) = module {
    single { HttpClient() }
    single { LocalFileResolver(context) }
    single { DownloadQueueManager() }
    single<SettingsRepository> { SettingsRepository(createDataStore(context)) }
    single<ApiService> { ApiService(get()) }
    single<PlaylistSync> { AndroidPlaylistSync(get(), get(), get()) }
    single<AudioPlayer> { AndroidAudioPlayer(get(), get(), get(), get()) }
    single<ImageResolver> { AndroidImageResolver(get(), get(), get()) }
}
