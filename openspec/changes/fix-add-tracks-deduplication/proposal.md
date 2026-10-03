# Proposal: Hash-Based Duplicate Detection & File Re-homing (Issue #3)

## Problem
When running "Add Tracks", the system frequently identifies already-imported files as "new" tracks (over 4,000 duplicate tracks were imported when adding only a dozen new files), especially when running against network/NFS-mounted directories or when files are moved or structured into folders. The current check relies purely on string matching of file names and directory paths (`findByFileNameAndFileLocation`), which fails whenever path resolution or directory layout changes.

## Proposed Solution
1. **Fingerprint-Based Deduplication**:
   - Compute a fast fingerprint: `<file_size_in_bytes>:<md5_hex_of_first_64kb>`.
   - Store this fingerprint in a indexed `file_hash` column on `track_file`.
2. **Relocation & Duplicate Disambiguation**:
   - When a matching hash is found in the database during "Add Tracks":
     - Check whether the file at the database's existing location still exists on disk.
     - **Relocated/Renamed**: If the old file does not exist, update the existing `TrackFile` record to the new location and filename.
     - **Duplicate**: If the old file still exists, skip importing. If the filenames differ, log a `WARN` so the user is informed about the duplicate under another name.
3. **Hash Backfill API**:
   - Expose an asynchronous endpoint `POST /music/updateFileHashes` (HTTP 202 Accepted) to backfill missing hashes on existing records without blocking the server.

## Impact & Scope
- Schema: Non-breaking nullable column and index on `track_file`.
- Performance: Minimal overhead (reading only up to 64KB per file + size).
- Out of scope: GUI changes (will be logged and invoked via API/curl), track cleanup tooling (can be handled separately).
