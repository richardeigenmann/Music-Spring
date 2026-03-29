# User Stories for Testing

These stories define the core functional requirements from a user perspective. Each should be verified manually or automated via E2E tests.

## 1. Import & Library Management
- **US1: Bulk Import:** As a user, I want to trigger a bulk import of a folder, so that all new tracks are automatically detected and added to the database. Covered by 02_add-tracks.cy.ts
- **US2: Unclassified Detection:** As a user, I want to easily identify tracks that have not been assigned to a group, so that I can maintain a tidy library.
- **US3: Metadata Accuracy:** As a user, I want the import process to accurately extract track metadata (title, artist, album), so that searching works correctly.

## 2. Organization & Search
- **US4: Search by Keyword:** As a user, I want to search my library by artist or title, so that I can quickly find a specific track.
Covered by 03_search-for-track.cy.ts
- **US5: Grouping/Categorization:** As a user, I want to move tracks into groups (playlists/genres), so that I can organize my music effectively.
Covered by 04_classify-some-tracks

## 3. Playback & Experience
- **US6: Queueing:** As a user, I want to add tracks to a playback queue, so that I can listen to a sequence of songs.
- **US7: Playback Control:** As a user, I want to play/pause/skip tracks, so that I can have complete control over my listening session. 
Covered by 06_play-pause-skip.cy.ts
- **US8: Persistent State:** As a user, I want the player state (current track, queue) to persist across browser refreshes, so that I don't lose my place.

## 4. Maintenance
- **US9: Database Integrity:** As a user, I want to identify and resolve broken file paths (e.g., moved files), so that my database doesn't contain "dead" entries.

## 5. Mixer & Advanced Filtering
- **US10: Logic-Based Filtering:** As a user, I want to drag-and-drop groups into "Must Include", "Can Include", and "Must Not Include" buckets, so that I can create dynamic mixes based on complex criteria. Covered by 05_create-playlist-from-mixer.cy.ts
- **US11: Save Filter as Playlist:** As a user, I want to save the result of my current filter as a new, static playlist, so that I can easily revisit that specific collection. Covered by 05_create-playlist-from-mixer.cy.ts
- **US12: Play Filtered Results:** As a user, I want to immediately play the result of my complex filter search, so that I can start listening to the dynamic mix without additional steps.

## 6. Export & Data Management
- **US13: Export as ZIP/M3U:** As a user, I want to export a group or playlist as an M3U file or a ZIP archive, so that I can use my curated lists in other music software.
- **US14: System Sync/Clear:** As a user, I want to save my data and later resore it after the database has been wiped or rebuilt, so that I can don't lose my track classifications. Covered by Cypress test 07_save_and_restore.cy.ts


