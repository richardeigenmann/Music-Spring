package org.richinet.musicbackend.service

import org.richinet.musicbackend.data.entity.Track
import org.springframework.stereotype.Service
import java.util.*

@Service
class TrackDataService {

    fun serializeTrack(track: Track): Map<String, Any?> {
        val trackData = LinkedHashMap<String, Any?>()
        trackData["trackId"] = track.id
        trackData["trackName"] = track.name

        // Group by type name
        val tagsByType = track.trackTags?.mapNotNull { it.tag }?.groupBy { it.tagType?.name } ?: emptyMap()

        tagsByType.forEach { (typeName, tags) ->
            if (typeName != null) {
                val tagNames = tags.mapNotNull { it.name }
                if (tagNames.size == 1) {
                    trackData[typeName] = tagNames[0]
                } else if (tagNames.isNotEmpty()) {
                    trackData[typeName] = tagNames
                }
            }
        }

        if (!track.trackFiles.isNullOrEmpty()) {
            val filesData = ArrayList<Map<String, Any?>>()
            track.trackFiles?.forEach { file ->
                val fileInfo = LinkedHashMap<String, Any?>()
                fileInfo["fileId"] = file.id
                fileInfo["fileName"] = file.fileName
                fileInfo["duration"] = file.duration
                filesData.add(fileInfo)
            }
            trackData["files"] = filesData
        }
        return trackData
    }
}
