# Delta Specification: Duplicate Detection, File Re-homing & Integrity Repair

## ADDED REQUIREMENTS

### Requirement: File Fingerprint Calculation
The system SHALL compute a fast audio file fingerprint using the formula:
`<file_size_in_bytes>:<md5_of_first_64kb>` (or MD5 of all bytes if file is smaller than 64KB).
The fingerprint format SHALL be a string of at most 64 characters.

### Requirement: Hash Storage and Indexing
The `track_file` table SHALL include a nullable column `file_hash VARCHAR(64)` with a database index `idx_track_file_hash`.

### Requirement: Duplicate and Relocation Detection during Import
During track scanning and import:
1. When evaluating an MP3 file, the system SHALL compute its fingerprint.
2. The system SHALL search for existing `track_file` records sharing the same `file_hash`.
3. If an existing record with matching `file_hash` is found:
   - The system SHALL verify if the existing file path (`musicDirectory + fileLocation + fileName`) exists on disk.
   - If the file at the existing record's path does NOT exist on disk:
     - The system SHALL treat the file as moved/renamed.
     - The system SHALL update the existing `track_file`'s `fileName` and `fileLocation` to point to the current file.
     - The system SHALL NOT create a duplicate `Track` or `TrackFile`.
   - If the file at the existing record's path DOES exist on disk:
     - The system SHALL treat the file as a duplicate.
     - If the file names differ, the system SHALL log a WARN message identifying the duplicate file name and the existing file name.
     - The system SHALL NOT import the duplicate file.

### Requirement: Asynchronous File Hash Backfill & Integrity Check Endpoint
The backend SHALL expose `POST /music/updateFileHashes` (or equivalent under MusicDbController) as an integrity repair function which:
1. Returns HTTP `202 Accepted` immediately.
2. Executes asynchronously in the background.
3. Reviews **ALL** `track_file` records in the database:
   - Verifies whether the referenced physical file exists on disk at `musicDirectory + fileLocation + fileName`.
   - If the file exists on disk:
     - Computes the file's current fingerprint.
     - Updates/refreshes the `file_hash` column on the `track_file` record if it is null or differs.
   - If the file does NOT exist on disk:
     - Logs a WARN message indicating an integrity violation (missing file on disk for `track_file` ID, track name, and expected path).
4. Logs progress and completion of the integrity repair and hash update process once finished.
