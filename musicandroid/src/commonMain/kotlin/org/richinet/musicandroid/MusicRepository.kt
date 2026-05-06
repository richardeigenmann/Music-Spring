package org.richinet.musicandroid

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.richinet.musicandroid.db.MusicDatabase

class MusicRepository(
    private val apiService: ApiService,
    private val database: MusicDatabase
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val queries = database.musicDatabaseQueries

    suspend fun getTags(): List<Tag> {
        val localTags = queries.selectAllTags().executeAsList()
        return if (localTags.isNotEmpty()) {
            localTags.map { Tag(it.tagId, it.tagName, it.tagTypeId, it.tagTypeName, it.tagTypeEdit) }
        } else {
            // If empty, try to sync or just return empty
            emptyList()
        }
    }

    suspend fun getTracksByTag(tagId: Long): List<Track> {
        val entities = queries.selectTracksByTag(tagId).executeAsList()
        return entities.map { entity ->
            val files = queries.selectFilesForTrack(entity.trackId).executeAsList().map {
                TrackFile(it.fileId, it.fileName, it.fileLocation, it.duration)
            }
            val metadata = json.decodeFromString<Map<String, JsonElement>>(entity.metadataJson)
            Track(entity.trackId, entity.trackName, files, metadata)
        }
    }

    suspend fun getTrack(trackId: Long): Track? {
        val entity = queries.selectTrackById(trackId).executeAsOneOrNull() ?: return null
        val files = queries.selectFilesForTrack(entity.trackId).executeAsList().map {
            TrackFile(it.fileId, it.fileName, it.fileLocation, it.duration)
        }
        val metadata = json.decodeFromString<Map<String, JsonElement>>(entity.metadataJson)
        return Track(entity.trackId, entity.trackName, files, metadata)
    }

    /**
     * Synchronizes only the list of tags. This is lightweight.
     */
    suspend fun syncTags() {
        val tags = apiService.getTags()
        database.transaction {
            // We don't clear everything, just update the tags table
            // But to keep it clean and handle deletions on server:
            queries.clearTags()
            tags.forEach { tag ->
                queries.insertTag(tag.tagId, tag.tagName, tag.tagTypeId, tag.tagTypeName, tag.tagTypeEdit)
            }
        }
    }

    /**
     * Synchronizes tracks for a specific tag.
     */
    suspend fun syncTracksByTag(tagId: Long) {
        val tracks = apiService.getTracksByTag(tagId)
        val tags = getTags() // Needed to build associations
        val tagMap = tags.associateBy { it.tagTypeName to it.tagName }

        database.transaction {
            // Remove existing associations for this tag to handle server-side removals
            queries.deleteTrackTagsByTagId(tagId)

            tracks.forEach { track ->
                // Insert/Update the track and its files
                upsertTrackInternal(track, tagMap)
                // Link this track to the current tag
                queries.insertTrackTag(track.trackId, tagId)
            }
        }
    }

    private fun upsertTrackInternal(track: Track, tagMap: Map<Pair<String, String>, Tag>) {
        queries.insertTrack(track.trackId, track.trackName, json.encodeToString(track.metadata))
        
        // Update files (clear and re-insert to handle changes)
        queries.deleteTrackFilesByTrackId(track.trackId)
        track.files.forEach { file ->
            queries.insertTrackFile(file.fileId, track.trackId, file.fileName, file.fileLocation, file.duration)
        }

        // Update all tag associations for this track based on metadata
        queries.deleteTrackTagsByTrackId(track.trackId)
        track.metadata.forEach { (typeName, element) ->
            val names = when (element) {
                is JsonArray -> element.map { it.jsonPrimitive.content }
                is JsonPrimitive -> listOf(element.content)
                else -> emptyList()
            }
            names.forEach { name ->
                tagMap[typeName to name]?.let { tag ->
                    queries.insertTrackTag(track.trackId, tag.tagId)
                }
            }
        }
    }

    suspend fun syncAll() {
        // syncAll is now just syncTags to prevent timeout/bloat
        syncTags()
    }
}
