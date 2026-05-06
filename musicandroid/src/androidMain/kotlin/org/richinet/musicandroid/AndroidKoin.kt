package org.richinet.musicandroid

import org.koin.dsl.module
import android.content.Context
import io.ktor.client.HttpClient
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import org.richinet.musicandroid.db.MusicDatabase

fun createAndroidModule(context: Context) = module {
    single { HttpClient() }
    single { 
        val driver = AndroidSqliteDriver(MusicDatabase.Schema, context, "music.db")
        MusicDatabase(driver)
    }
    single<NetworkObserver> { AndroidNetworkObserver(context) }
    single { LocalFileResolver(context) }
    single { DownloadQueueManager() }
    single<SettingsRepository> { SettingsRepository(createDataStore(context)) }
    single<ApiService> { ApiService(get()) }
    single<PlaylistSync> { AndroidPlaylistSync(get(), get(), get()) }
    single<AudioPlayer> { AndroidAudioPlayer(get(), get(), get(), get(), get()) }
    single<ImageResolver> { AndroidImageResolver(get(), get(), get()) }
    single<PictureChecker> { AndroidPictureChecker(get(), get()) }
}
