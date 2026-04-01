package org.richinet.musicandroid

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MusicApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        //val baseUrl = "http://10.0.2.2:8002"
        //val baseUrl = "http://octan:8002"
        val baseUrl = "http://octan:8011"
        val apiService = ApiService(baseUrl)
        val playlistSync = AndroidPlaylistSync(this, apiService)
        val audioPlayer = AndroidAudioPlayer(this, apiService)

        startKoin {
            androidContext(this@MusicApplication)
            androidLogger()
            modules(createCommonModule(baseUrl, playlistSync, audioPlayer))
        }
    }
}
