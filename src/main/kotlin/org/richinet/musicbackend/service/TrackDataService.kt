package org.richinet.musicbackend.service

import org.richinet.musicbackend.data.entity.Track
import org.springframework.stereotype.Service
import java.util.*

@Service
class TrackDataService {

    fun serializeTrack(track: Track): Map<String, Any?> {
        val trackData = LinkedHashMap<String, Any?>()
        trackData["TrackName"] = track.trackName

        track.trackGroups?.forEach { trackGroup ->
            val group = trackGroup.group
            val groupType = group?.groupType

            if (group != null && groupType != null) {
                val groupTypeName = groupType.groupTypeName
                val groupName = group.groupName

                if (groupTypeName != null && groupName != null) {
                    if (trackData.containsKey(groupTypeName)) {
                        val existing = trackData[groupTypeName]
                        if (existing is MutableList<*>) {
                            @Suppress("UNCHECKED_CAST")
                            (existing as MutableList<String>).add(groupName)
                        } else {
                            val list = ArrayList<String>()
                            list.add(existing as String)
                            list.add(groupName)
                            trackData[groupTypeName] = list
                        }
                    } else {
                        trackData[groupTypeName] = groupName
                    }
                }
            }
        }

        if (!track.trackFiles.isNullOrEmpty()) {
            val filesData = ArrayList<Map<String, Any?>>()
            track.trackFiles?.forEach { file ->
                val fileInfo = LinkedHashMap<String, Any?>()
                fileInfo["FileName"] = file.fileName
                fileInfo["FileLocation"] = file.fileLocation
                fileInfo["FileOnline"] = file.fileOnline
                fileInfo["Duration"] = file.duration
                fileInfo["BackupDate"] = file.backupDate
                filesData.add(fileInfo)
            }
            trackData["Files"] = filesData
        }
        return trackData
    }
}
