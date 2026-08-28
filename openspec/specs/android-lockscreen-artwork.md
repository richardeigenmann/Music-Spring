# Specification: Android Lockscreen Artwork

## Overview
Defines how track artwork is displayed on the Android system media controls (lock screen widget).

## Behavior
- **Source Priority**: The app MUST prioritize local artwork extraction from cached files over network fetching.
- **Local Extraction**: For cached tracks, the app SHALL extract embedded artwork from the MP3 file using `MediaMetadataRetriever`.
- **Memory Safety**: Extracted artwork MUST be downscaled to under 500KB to ensure compatibility with Android's Binder transaction limits (avoiding `TransactionTooLargeException`).
- **Network Fallback**: For streaming tracks or tracks with no local art, the app SHALL provide the artwork URI from the backend API.
- **Placeholder**: If no artwork is available, a default placeholder icon SHALL be displayed.

## Technical Details
- Implemented in `AndroidAudioPlayer.startPlaybackForCurrentTrack`.
- Extraction logic located in `LocalFileResolver.getEmbeddedPicture`.
- Downscaling logic located in `LocalFileResolver.downscaleImage`.
