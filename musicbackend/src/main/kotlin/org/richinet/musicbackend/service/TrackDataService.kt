package org.richinet.musicbackend.service

import org.richinet.musicbackend.data.dto.TrackDto
import org.richinet.musicbackend.data.dto.TrackFileDto
import org.richinet.musicbackend.data.entity.Track
import org.springframework.stereotype.Service

@Service
class TrackDataService {

    /**
     * Serializes a Track entity into a TrackDto.
     * Metadata (tags) are dynamically added to the DTO.
     */
    fun serializeTrack(track: Track): TrackDto {
        val filesDtoList = track.trackFiles?.map { file ->
            TrackFileDto(
                fileId = file.id,
                fileName = file.fileName,
                fileLocation = file.fileLocation,
                duration = file.duration
            )
        } ?: emptyList()

        val trackDto = TrackDto(
            trackId = track.id,
            trackName = track.name,
            files = filesDtoList
        )

        // Dynamically add tags by their type name
        val tagsByType = track.trackTags?.mapNotNull { it.tag }?.groupBy { it.tagType?.name } ?: emptyMap()

        tagsByType.forEach { (typeName, tags) ->
            if (typeName != null) {
                val tagNames = tags.mapNotNull { it.name }
                if (tagNames.size == 1) {
                    trackDto.addMetadata(typeName, tagNames[0])
                } else if (tagNames.isNotEmpty()) {
                    trackDto.addMetadata(typeName, tagNames)
                }
            }
        }

        return trackDto
    }
}
