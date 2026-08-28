# Specs: Android Lockscreen Artwork

## User Scenarios

### Scenario 1: Playing a cached track while online
**Given** a track is cached locally
**And** the Android app is connected to the internet
**When** the user starts playback
**Then** the lock screen widget SHALL display the artwork extracted from the local MP3 file.

### Scenario 2: Streaming a track
**Given** the track is not available locally
**And** the Android app is connected to the internet
**When** the user starts playback
**Then** the lock screen widget SHALL display the track artwork fetched from the backend API.

### Scenario 3: Playing a track with no artwork
**Given** a track has no embedded artwork and no artwork on the backend
**When** the user starts playback
**Then** the lock screen widget SHALL display the default placeholder image provided by the backend or a local fallback.

## Technical Requirements
- The app MUST set `MediaMetadata.artworkUri` for all media items.
- The app SHOULD set `MediaMetadata.artworkData` if the file is available locally to support offline display.
- Artwork extraction MUST NOT block the UI thread.
