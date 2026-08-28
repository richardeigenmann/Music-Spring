# Proposal: Android Lockscreen Artwork Support

## Problem Statement
The Android player widget on the lock screen currently displays the track name, artist, and album, but it does not show the track's artwork (album art). This makes the playback experience feel incomplete compared to other music applications.

## Proposed Solution
Enhance the `PlaybackService` and `AndroidAudioPlayer` to include artwork in the `MediaMetadata` for each `MediaItem`. 

### Strategy:
1.  **Local-First**: If a track is available locally (cached), `AndroidAudioPlayer` SHALL extract the embedded artwork using `MediaMetadataRetriever` and set it as `artworkData` in the `MediaMetadata`. This is the primary source of truth for artwork, ensuring it is available offline and avoids unnecessary network requests.
2.  **Network Fallback**: If the track is being streamed and not available locally, the app SHALL set the `artworkUri` to the backend image endpoint (`api/trackFileImage/{fileId}`).
3.  **Placeholder**: If both local extraction and network fetching fail, the app SHALL rely on the backend's placeholder or a local resource fallback.

## Scope
- Modify `AndroidAudioPlayer.startPlaybackForCurrentTrack` to:
    - Set `artworkUri` using `apiService.getTrackImageUrl(fileId)`.
    - If the track is cached, extract `embeddedPicture` bytes and set `artworkData`.
- Ensure the `PlaybackService` (MediaSession) correctly propagates this metadata to the system.

## Non-Goals
- Changing the lock screen widget layout itself (we will use the system-provided media widget).

## Resolved Questions
1. **Placeholder Handling**: The app SHALL display a default placeholder image (e.g., a generic music note icon) if both local extraction and the backend API fail to provide an image.
2. **Local Playback**: To preserve bandwidth and ensure offline support, the app MUST pull artwork locally from the MP3 file whenever the file is available on the device.
3. **In-App Player (PlayerScreen)**: The `PlayerScreen` (Now Playing screen) SHOULD also be updated to prioritize local artwork extraction for consistency with the lock screen widget.
4. **Artwork URI Validity**: No authentication is required for the backend artwork endpoint at this time.
