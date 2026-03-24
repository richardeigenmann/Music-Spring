package org.richinet.musicbackend.service

import org.richinet.musicbackend.data.entity.Track
import org.springframework.stereotype.Service
import java.util.*

@Service
class TrackDataService {

    fun serializeTrack(track: Track): Map<String, Any?> {
        val trackData = LinkedHashMap<String, Any?>()
        trackData["TrackId"] = track.trackId
        trackData["TrackName"] = track.trackName

        // Group by type name
        val groupsByType = track.trackGroups?.mapNotNull { it.group }?.groupBy { it.groupType?.groupTypeName } ?: emptyMap()

        groupsByType.forEach { (typeName, groups) ->
            if (typeName != null) {
                val groupNames = groups.mapNotNull { it.groupName }
                if (groupNames.size == 1) {
                    trackData[typeName] = groupNames[0]
                } else if (groupNames.isNotEmpty()) {
                    trackData[typeName] = groupNames
                }
            }
        }

        if (!track.trackFiles.isNullOrEmpty()) {
            val filesData = ArrayList<Map<String, Any?>>()
            track.trackFiles?.forEach { file ->
                val fileInfo = LinkedHashMap<String, Any?>()
                fileInfo["FileId"] = file.fileId
                fileInfo["FileName"] = file.fileName
                fileInfo["Duration"] = file.duration
                filesData.add(fileInfo)
            }
            trackData["Files"] = filesData
        }
        return trackData
    }
}
