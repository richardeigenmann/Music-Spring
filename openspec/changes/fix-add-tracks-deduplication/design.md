# Design: Hash-Based Duplicate Detection & Re-homing

## 1. Fingerprint Algorithm
To quickly fingerprint MP3 files without reading potentially hundreds of gigabytes across network/NFS storage:
- **Algorithm**: Concatenate File Size + MD5 of first 64KB (or full file if < 64KB).
- **Format**: `"<file_length_in_bytes>:<md5_hex_of_first_64kb>"` (e.g. `10485760:d41d8cd98f00b204e9800998ecf8427e`).
- Fits within `VARCHAR(64)` (max length is ~20 digits + 1 colon + 32 hex = ~53 chars).
- High performance, low I/O, virtually zero collision probability when combined with exact file size.

## 2. Database Schema
- **Entity**: `TrackFile` (`track_file` table).
- **Column**: `file_hash` (`VARCHAR(64)`, nullable).
- **Index**: `idx_track_file_hash` on `file_hash` column.
- **Constraints**: No database-level UNIQUE constraint to allow flexibility. Application logic handles uniqueness and detection.

## 3. Repository
- `TrackFileRepository`:
  - `fun findByFileHash(fileHash: String): List<TrackFile>`

## 4. Scan / Import Flow (`processNewMp3File`)
When `processNewMp3File(file: File)` runs:
1. Compute the fingerprint for `file`: `val hash = computeFileHash(file)`.
2. Look up existing `TrackFile` records matching `hash` using `trackFileRepository.findByFileHash(hash)`.
3. If matches are found:
   - For each matching record:
     - Check if the file referenced by the DB record still physically exists on disk (using `musicDirectory + existingRecord.fileLocation + existingRecord.fileName`).
     - **Case A: The file at the existing DB location does NOT exist on disk anymore**:
       - It was moved or renamed!
       - Update existing `TrackFile`: set `fileName` and `fileLocation` to the current `file`'s name and relative location.
       - Save the updated `TrackFile`.
       - Log an INFO message about the moved/renamed file re-homing.
       - Skip importing as a new track (it has been reconciled).
     - **Case B: The file at the existing DB location DOES exist on disk**:
       - It is a true duplicate.
       - If `file.name != existingRecord.fileName`:
         - Log a WARN: e.g., `"Duplicate audio file detected with different name: '${file.name}' matches existing track file '${existingRecord.fileName}' (Hash: $hash). Skipping import."`
       - Skip importing as a new track.
4. If no matches found (or if all matches were handled / none matched):
   - Proceed with existing logic to create/find `Track`, create `TrackFile`, populate tags, and store `trackFile.fileHash = hash`.

## 5. Backfill & Integrity Repair Endpoint (`POST /music/updateFileHashes`)
- **Controller**: `MusicDbController`.
- **Endpoint**: `@PostMapping("/updateFileHashes")` returning `202 Accepted` immediately (fire-and-forget asynchronous background task).
- **Service**: `MusicImportService.updateAllFileHashes()`.
  - Runs in a background thread or async executor.
  - Queries **ALL** `TrackFile` records in the database.
  - For each `TrackFile`:
    - Resolves physical path on disk: `File(musicDirectory, "${record.fileLocation}/${record.fileName}".replace("//", "/"))`.
    - If file exists on disk:
      - Computes fingerprint.
      - If `record.fileHash != computedHash`:
        - Updates `record.fileHash = computedHash` and saves.
    - If file does NOT exist on disk:
      - Logs a WARN message indicating an integrity violation: missing physical file on disk for `trackFile.id`, expected path, and associated track.
  - Logs summary and completion when all records have been evaluated.

## 6. Files Changed
- `openspec/changes/fix-add-tracks-deduplication/`: specs, design, tasks
- `musicbackend/src/main/kotlin/org/richinet/musicbackend/data/entity/TrackFile.kt`
- `musicbackend/src/main/kotlin/org/richinet/musicbackend/data/repository/TrackFileRepository.kt`
- `musicbackend/src/main/kotlin/org/richinet/musicbackend/service/MusicImportService.kt`
- `musicbackend/src/main/kotlin/org/richinet/musicbackend/controller/MusicDbController.kt`
