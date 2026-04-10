package org.richinet.musicandroid

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MusicApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@MusicApplication)
            androidLogger()
            modules(createAndroidModule(this@MusicApplication), createCommonModule())
        }
    }
}
