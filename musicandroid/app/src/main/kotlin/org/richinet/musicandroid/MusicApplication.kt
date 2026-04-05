package org.richinet.musicandroid

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MusicApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // We create the instances with the application context
        // ApiService will now get its URL from DataStore (SettingsRepository)
        val settingsRepository = SettingsRepository(createDataStore(this))
        val apiService = ApiService(settingsRepository)
        val playlistSync = AndroidPlaylistSync(this, apiService)
        val audioPlayer = AndroidAudioPlayer(this, apiService)
        val imageResolver = AndroidImageResolver(this, apiService)

        startKoin {
            androidContext(this@MusicApplication)
            androidLogger()
            modules(createCommonModule(settingsRepository, apiService, playlistSync, audioPlayer, imageResolver))
        }
    }
}
