package org.richinet.musicandroid

import android.content.Context
import android.net.Uri
import android.content.ContentUris
import android.os.Build
import android.provider.MediaStore
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors
import android.media.MediaMetadataRetriever
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidPlaylistSync(
    private val context: Context,
    private val apiService: ApiService,
    private val localFileResolver: LocalFileResolver
) : PlaylistSync {
    private val synchronizer = PlaylistSynchronizer(context, apiService, localFileResolver)

    override fun syncTrack(track: Track) {
        synchronizer.syncTrack(track)
    }

    override fun registerPlaylist(tagName: String, tracks: List<Track>) {
        synchronizer.registerPlaylist(tagName, tracks)
    }

    override fun sync(tagName: String, tracks: List<Track>) {
        synchronizer.syncPlaylist(tagName, tracks)
    }
}

class AndroidImageResolver(
    private val context: Context,
    private val apiService: ApiService,
    private val localFileResolver: LocalFileResolver
) : ImageResolver {
    override fun getTrackImageSource(track: Track): Any? {
        val file = track.files.firstOrNull() ?: return null

        // We prefer the network URL for images to ensure high quality and consistency.
        // Coil handles caching this URL locally, so it works offline if previously loaded.
        // Using the local audio file URI often fails to provide artwork if not embedded.
        return apiService.getTrackImageUrl(file.fileId)
    }
}

class AndroidPictureChecker(
    private val context: Context,
    private val localFileResolver: LocalFileResolver
) : PictureChecker {
    override suspend fun getLocalFiles(): List<String> {
        localFileResolver.refreshCache()
        return localFileResolver.foundFileNames.value.toList()
    }

    override suspend fun deleteLocalFile(fileName: String): Boolean = withContext(Dispatchers.IO) {
        val uri = localFileResolver.findLocalUri(fileName) ?: return@withContext false
        try {
            val deleted = context.contentResolver.delete(uri, null, null) > 0
            if (deleted) {
                Log.i("PictureChecker", "Deleted file: $fileName")
                localFileResolver.refreshCache()
            }
            deleted
        } catch (e: Exception) {
            Log.e("PictureChecker", "Failed to delete $fileName: ${e.message}")
            false
        }
    }

    override suspend fun checkLocalFiles(
        fileNames: List<String>,
        startIndex: Int,
        onProgress: (Int, Int, String) -> Unit
    ): List<String> = withContext(Dispatchers.IO) {
        val missingPictures = mutableListOf<String>()
        val total = fileNames.size
        var consecutiveSystemFailures = 0

        for (i in startIndex until total) {
            val fileName = fileNames[i]
            
            if (consecutiveSystemFailures >= 10) {
                Log.e("PictureChecker", "Aborting: Native media service has failed to recover after multiple attempts.")
                break
            }

            onProgress(i + 1, total, fileName)
            
            // Standard pacing
            kotlinx.coroutines.delay(100)
            
            // Batch pause: Every 20 files, take a longer break for the system to catch up
            if (i > startIndex && (i - startIndex) % 20 == 0) {
                Log.d("PictureChecker", "Batch limit reached. Pausing 1s for system cleanup...")
                kotlinx.coroutines.delay(1000)
            }

            val uri = localFileResolver.findLocalUri(fileName) ?: continue

            var artFound = false
            var metadataRead = false
            var dataSourceSet = false

            // Attempt to set data source with recovery logic
            var attempt = 0
            while (!dataSourceSet && attempt < 2) {
                if (attempt > 0) {
                    Log.w("PictureChecker", "System service stall suspected. Attempting service reset and 5s cooldown...")
                    
                    // Attempt to 'poke' the MediaStore to re-establish binder health
                    try {
                        context.contentResolver.query(uri, arrayOf(MediaStore.Audio.Media._ID), null, null, null)?.close()
                        System.gc() // Suggest a GC for any unreleased native handles
                    } catch (e: Exception) {
                        Log.d("PictureChecker", "Service poke failed: ${e.message}")
                    }
                    
                    kotlinx.coroutines.delay(5000)
                }
                attempt++

                val retriever = MediaMetadataRetriever()
                try {
                    // Strategy: Try Uri first
                    try {
                        retriever.setDataSource(context, uri)
                        dataSourceSet = true
                    } catch (e: Exception) {
                        // Fallback: Try PFD
                        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                            retriever.setDataSource(pfd.fileDescriptor, 0, pfd.statSize)
                            dataSourceSet = true
                        }
                    }

                    if (dataSourceSet) {
                        val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                        val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                        val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                        
                        metadataRead = title != null || artist != null || album != null
                        Log.d("PictureChecker", "Checking $fileName: Title='$title', Artist='$artist', Album='$album'")

                        val art = retriever.embeddedPicture
                        if (art != null && art.isNotEmpty()) {
                            artFound = true
                            Log.d("PictureChecker", "Picture found for $fileName (${art.size} bytes)")
                        }
                    }
                } catch (e: Exception) {
                    Log.d("PictureChecker", "Retriever attempt $attempt failed for $fileName: ${e.message}")
                } finally {
                    try {
                        retriever.release()
                    } catch (e: Exception) { /* ignore */ }
                }
            }

            if (dataSourceSet) {
                consecutiveSystemFailures = 0
                if (!artFound) {
                    missingPictures.add(fileName)
                    Log.w("PictureChecker", "MISSING_PICTURE: $fileName (${if (metadataRead) "Metadata found, no picture" else "No metadata found"})")
                }
            } else {
                consecutiveSystemFailures++
                missingPictures.add(fileName)
                Log.e("PictureChecker", "MISSING_PICTURE: $fileName (System service failed to open file after cooldown - URI: $uri)")
            }
        }

        // Final summary to logcat
        Log.i("PictureChecker", "--- PICTURE CHECK SUMMARY ---")
        missingPictures.forEach { Log.i("PictureChecker", "MISSING_PICTURE: $it") }
        Log.i("PictureChecker", "Total missing/unreadable: ${missingPictures.size} out of $total files checked.")
        
        missingPictures
    }
}

actual fun createDataStore(context: Any?): DataStore<Preferences> {
    val ctx = context as Context
    return PreferenceDataStoreFactory.create {
        ctx.filesDir.resolve("settings.preferences_pb")
    }
}

@Composable
actual fun RequestPermissions(onGranted: () -> Unit, onDenied: () -> Unit) {
    val context = LocalContext.current
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        android.Manifest.permission.READ_MEDIA_AUDIO
    } else {
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) onGranted() else onDenied()
        }
    )

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            onGranted()
        } else {
            launcher.launch(permission)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun QrScanner(onResult: (String) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
            if (!granted) {
                onDismiss()
            }
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan QR Code") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close")
                    }
                }
            )
        }
    ) { padding ->
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val executor = ContextCompat.getMainExecutor(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val scanner = BarcodeScanning.getClient(
                            BarcodeScannerOptions.Builder()
                                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                                .build()
                        )

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                scanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        for (barcode in barcodes) {
                                            barcode.rawValue?.let {
                                                onResult(it)
                                            }
                                        }
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            }
                        }

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, executor)
                    previewView
                },
                modifier = Modifier.fillMaxSize().padding(padding)
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Camera permission required")
            }
        }
    }
}
