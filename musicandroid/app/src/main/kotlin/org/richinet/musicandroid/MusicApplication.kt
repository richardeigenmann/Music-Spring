package org.richinet.musicandroid

import android.app.Application
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import java.util.concurrent.Executors

class MusicApplication : Application(), Configuration.Provider, ImageLoaderFactory {
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setExecutor(Executors.newFixedThreadPool(3)) // Limit to 3 parallel downloads to save memory
            .build()

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    // Use a smaller percentage of available heap for images (default is 25%)
                    // On a 512MB heap, 15% is ~76MB, leaving more room for ExoPlayer and WorkManager.
                    .maxSizePercent(0.15)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100 * 1024 * 1024) // 100MB
                    .build()
            }
            // Optimization: allow hardware bitmaps to save memory if applicable,
            // though some older devices might have issues, it's generally better for OOM.
            .allowHardware(true)
            .build()
    }

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@MusicApplication)
            androidLogger()
            modules(createAndroidModule(this@MusicApplication), createCommonModule())
        }
    }
}
