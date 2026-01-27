package org.richinet.musicbackend.service

import org.richinet.musicbackend.data.entity.*
import org.richinet.musicbackend.data.repository.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant

@Service
class MusicImportService(
    private val trackRepository: TrackRepository,
    private val groupsRepository: GroupsRepository,
    private val groupTypeRepository: GroupTypeRepository,
    private val trackFileRepository: TrackFileRepository,
    private val trackGroupRepository: TrackGroupRepository
) {
    private val logger = LoggerFactory.getLogger(MusicImportService::class.java)

    @Transactional
    fun importMusicData(data: List<Map<String, Any>>) {
        val groupTypeCache = groupTypeRepository.findAll().associateBy { it.groupTypeName }
        var count = 0

        for (trackData in data) {
            val track = Track()
            track.trackName = trackData["TrackName"] as? String
            trackRepository.save(track)

            for ((key, value) in trackData) {
                if (key == "TrackName") continue
                if (key == "Files") {
                    val filesList = value as? List<Map<String, Any>>
                    filesList?.forEach { fileData ->
                        val trackFile = TrackFile()
                        trackFile.trackId = track.trackId
                        trackFile.fileName = fileData["FileName"] as? String
                        trackFile.fileLocation = fileData["FileLocation"] as? String
                        trackFile.fileOnline = fileData["FileOnline"] as? String
                        
                        val duration = fileData["Duration"]
                        if (duration is Int) {
                             trackFile.duration = BigDecimal(duration)
                        } else if (duration is Double) {
                             trackFile.duration = BigDecimal(duration)
                        }

                        val backupDateStr = fileData["BackupDate"] as? String
                        if (backupDateStr != null) {
                             try {
                                 trackFile.backupDate = Timestamp.from(Instant.parse(backupDateStr))
                             } catch (e: Exception) {
                                 // ignore
                             }
                        }
                        
                        trackFileRepository.save(trackFile)
                    }
                    continue
                }

                val groupType = groupTypeCache[key]
                if (groupType != null) {
                    val groupNames = if (value is List<*>) value else listOf(value)
                    for (groupNameObj in groupNames) {
                        val groupName = groupNameObj as? String ?: continue
                        
                        var group = groupsRepository.findByGroupNameAndGroupTypeId(groupName, groupType.groupTypeId!!)
                        if (group == null) {
                            group = Groups()
                            group.groupTypeId = groupType.groupTypeId
                            group.groupName = groupName
                            group.lastModification = Timestamp(System.currentTimeMillis())
                            groupsRepository.save(group)
                        }

                        val trackGroup = TrackGroup()
                        trackGroup.trackId = track.trackId
                        trackGroup.groupId = group.groupId
                        trackGroup.sequence = BigDecimal.ZERO
                        trackGroup.lastModification = Timestamp(System.currentTimeMillis())
                        trackGroupRepository.save(trackGroup)
                    }
                }
            }
            
            count++
            if (count % 200 == 0) {
                logger.info("Imported $count tracks")
            }
        }
        logger.info("Import finished. Total tracks: $count")
    }
}
