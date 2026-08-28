# Tasks: Android Lockscreen Artwork

- [x] Create a helper function in `LocalFileResolver` to extract and downscale `embeddedPicture` from a `Uri`.
- [x] Update `AndroidAudioPlayer.startPlaybackForCurrentTrack` to populate `artworkData` (local) and `artworkUri` (fallback) in `MediaMetadata`.
- [x] Update `AndroidImageResolver` in `Platform.kt` to also prioritize local artwork extraction for the in-app player.
- [x] Verify playback on a device/emulator and check the lock screen widget.
- [x] Test offline behavior with a cached track.
- [x] Test behavior with a track that has no artwork.
