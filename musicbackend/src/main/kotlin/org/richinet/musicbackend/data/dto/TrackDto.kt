package org.richinet.musicbackend.data.dto

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Data Transfer Object for a music track, including dynamic metadata")
class TrackDto(
    @Schema(description = "Unique identifier for the track")
    val trackId: Long?,
    @Schema(description = "Name of the track")
    var trackName: String?,
    @Schema(description = "List of files associated with this track")
    val files: List<TrackFileDto> = emptyList()
) {
    private val metadata = mutableMapOf<String, Any?>()

    /**
     * Get all additional metadata fields.
     * These are dynamic and represent tag types from the database.
     */
    @JsonAnyGetter
    fun getMetadata(): Map<String, Any?> = metadata

    /**
     * Add an additional metadata field.
     */
    @JsonAnySetter
    fun addMetadata(key: String, value: Any?) {
        metadata[key] = value
    }
}
