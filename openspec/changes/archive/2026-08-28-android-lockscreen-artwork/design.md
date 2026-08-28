# Design: Android Lockscreen Artwork Support

## Technical Approach

### 1. Metadata Extraction Utility
We need a way to extract artwork as raw bytes from a local Uri. 

**Why ByteArray?**
`MediaMetadata` is passed across process boundaries (IPC) to the Android System UI to render the lock screen widget. `ByteArray` (raw JPEG/PNG data) is the standard format for this, as high-level UI objects like `Bitmap` or `ImageBitmap` cannot be directly transferred across processes.

```kotlin
private fun getArtworkData(context: Context, uri: Uri): ByteArray? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        retriever.embeddedPicture
    } catch (e: Exception) {
        null // Return null to allow fallback to network URI or placeholder
    } finally {
        retriever.release()
    }
}
```

### 2. Updating AndroidAudioPlayer
In `startPlaybackForCurrentTrack`, we will populate `MediaMetadata`. If local extraction fails, we fallback to the network URI. If that also isn't viable (e.g., offline), we use a local placeholder.

```kotlin
val localArtwork = if (uri != Uri.EMPTY) getArtworkData(context, uri) else null

val metadataBuilder = MediaMetadata.Builder()
    .setTitle(t.trackName)
    .setArtist(t.getArtist())
    .setAlbumTitle(t.getAlbum())

if (localArtwork != null) {
    // Use the extracted artwork. While we label it as FRONT_COVER for the system,
    // getArtworkData (via MediaMetadataRetriever) returns the first available 
    // embedded picture in the file, ensuring we show whatever art is present.
    metadataBuilder.setArtworkData(localArtwork, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
} else {
    // Fallback to API URI (backend provides a placeholder if art is missing)
    metadataBuilder.setArtworkUri(Uri.parse(apiService.getTrackImageUrl(file.fileId)))
}

val metadata = metadataBuilder.build()
```

### 3. Handling the Placeholder
- **Online**: The `artworkUri` points to the backend, which is already designed to return a `placeholder.png` if no image is found in the MP3 metadata on the server.
- **Offline & No Local Art**: If `localArtwork` is null and we are offline, the system media widget will typically show a default icon. To provide a custom experience, we will use a local resource placeholder as a final fallback.

## Implementation Details
- **Threading**: Extraction uses `MediaMetadataRetriever`, which can be slow. This MUST happen in the background (already handled by `scope.launch(Dispatchers.Default)` in `AndroidAudioPlayer`).
- **Memory Safety**: To avoid `TransactionTooLargeException` (which occurs when Binder transactions exceed 1MB), we will downscale any extracted artwork so that the `ByteArray` is comfortably under **500KB**.
