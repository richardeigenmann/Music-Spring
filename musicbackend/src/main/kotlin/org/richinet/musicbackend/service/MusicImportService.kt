package org.richinet.musicbackend.service

import com.mpatric.mp3agic.Mp3File
import org.richinet.musicbackend.data.entity.Groups
import org.richinet.musicbackend.data.entity.Track
import org.richinet.musicbackend.data.entity.TrackFile
import org.richinet.musicbackend.data.entity.TrackGroup
import org.richinet.musicbackend.data.repository.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

data class ScanProgress(
    val totalEstimated: Int = 0,
    val checked: Int = 0,
    val added: Int = 0,
    val isDone: Boolean = false,
    val currentFile: String = ""
)

@Service
class MusicImportService(
    private val trackRepository: TrackRepository,
    private val groupsRepository: GroupsRepository,
    private val groupTypeRepository: GroupTypeRepository,
    private val trackFileRepository: TrackFileRepository,
    private val trackGroupRepository: TrackGroupRepository,
    private val trackDataService: TrackDataService
) {
    private val logger = LoggerFactory.getLogger(MusicImportService::class.java)
    private val scanProgress = AtomicReference(ScanProgress(isDone = true))

    @Value("\${app.music-directory}")
    private lateinit var musicDirectory: String

    fun getScanProgress(): ScanProgress = scanProgress.get()

    @Transactional
    fun importMusicData(data: List<Map<String, Any>>) {
        var count = 0
        for (trackData in data) {
            val track = Track()
            track.trackName = trackData["TrackName"] as? String
            trackRepository.save(track)

            processTrackData(track, trackData)

            count++
            if (count % 50 == 0) {
                logger.info("Imported $count tracks")
            }
        }
        logger.info("Import finished. Total tracks: $count")
    }

    @Transactional
    fun updateTrack(trackId: Long, trackData: Map<String, Any?>) {
        val track = trackRepository.findById(trackId).orElseThrow { RuntimeException("Track not found") }

        if (trackData.containsKey("TrackName")) {
            track.trackName = trackData["TrackName"] as? String
        }
        trackRepository.save(track)

        trackGroupRepository.deleteByTrackId(trackId)
        trackFileRepository.deleteByTrackId(trackId)

        processTrackData(track, trackData)
    }

    @Transactional
    fun deleteTrack(trackId: Long) {
        trackGroupRepository.deleteByTrackId(trackId)
        trackFileRepository.deleteByTrackId(trackId)
        trackRepository.deleteById(trackId)
    }

    private fun processTrackData(track: Track, trackData: Map<String, Any?>) {
        val groupTypeCache = groupTypeRepository.findAll().associateBy { it.groupTypeName }

        for ((key, value) in trackData) {
            if (key == "TrackName" || key == "TrackId") continue

            if (key == "Files") {
                val filesList = value as? List<Map<String, Any?>>
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
                    } else if (duration is BigDecimal) {
                         trackFile.duration = duration
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
            if (groupType != null && value != null) {
                val groupNames = if (value is List<*>) value else listOf(value)
                for (groupNameObj in groupNames) {
                    val groupName = groupNameObj as? String ?: continue
                    linkTrackToGroup(track.trackId!!, groupName, groupType.groupTypeId!!)
                }
            }
        }
    }

    private fun linkTrackToGroup(trackId: Long, groupName: String, groupTypeId: BigDecimal) {
        var group = groupsRepository.findByGroupNameAndGroupTypeId(groupName, groupTypeId)
        if (group == null) {
            group = Groups()
            group.groupTypeId = groupTypeId
            group.groupName = groupName
            group.lastModification = Timestamp(System.currentTimeMillis())
            groupsRepository.save(group)
        }

        val trackGroup = TrackGroup()
        trackGroup.trackId = trackId
        trackGroup.groupId = group.groupId
        trackGroup.sequence = BigDecimal.ZERO
        trackGroup.lastModification = Timestamp(System.currentTimeMillis())
        trackGroupRepository.save(trackGroup)
    }

    fun startMp3Scan() {
        if (!scanProgress.get().isDone) return

        val mp3Dir = File(musicDirectory)
        if (!mp3Dir.exists() || !mp3Dir.isDirectory) {
            logger.error("MP3 directory not found at $mp3Dir")
            return
        }

        Thread {
            try {
                logger.info("Starting MP3 scan in $mp3Dir")
                val filesToScan = mp3Dir.walkTopDown().filter { it.isFile && it.extension.lowercase() == "mp3" }.toList()
                val total = filesToScan.size
                scanProgress.set(ScanProgress(totalEstimated = total))

                filesToScan.forEachIndexed { index, file ->
                    scanProgress.updateAndGet { it.copy(checked = index + 1, currentFile = file.name) }
                    try {
                        processNewMp3File(file)
                    } catch (e: Throwable) {
                        logger.error("Fatal error processing file ${file.absolutePath}", e)
                    }
                }
            } catch (e: Exception) {
                logger.error("Error during MP3 scan", e)
            } finally {
                scanProgress.updateAndGet { it.copy(isDone = true) }
                logger.info("MP3 scan finished.")
            }
        }.start()
    }

    @Transactional
    fun processNewMp3File(file: File) {
        val fullPath = file.absolutePath
        val relPath = fullPath.substringAfter(musicDirectory)
        val fileName = file.name
        val fileLocation = relPath.substringBeforeLast(fileName)

        // Normalize fileLocation to start and end with /
        val normalizedLocation = if (fileLocation.startsWith("/")) fileLocation else "/$fileLocation"
        val finalLocation = if (normalizedLocation.endsWith("/")) normalizedLocation else "$normalizedLocation/"

        val existingFiles = trackFileRepository.findByFileNameAndFileLocation(fileName, finalLocation)
        if (existingFiles.isNotEmpty()) {
            return
        }

        // It's a new file
        logger.info("Processing new MP3 file: ${file.absolutePath}")
        val mp3file = Mp3File(file)
        var artist = ""
        var title = ""
        var album = ""
        var genre = ""
        var duration: BigDecimal

      if (mp3file.hasId3v2Tag()) {
            val tag = mp3file.id3v2Tag
            artist = tag.artist ?: ""
            title = tag.title ?: ""
            album = tag.album ?: ""
            genre = tag.genreDescription ?: ""
        } else if (mp3file.hasId3v1Tag()) {
            val tag = mp3file.id3v1Tag
            artist = tag.artist ?: ""
            title = tag.title ?: ""
            album = tag.album ?: ""
            genre = tag.genreDescription ?: ""
        }

        if (title.isBlank()) {
            title = fileName.substringBeforeLast(".")
        }
        duration = BigDecimal(mp3file.lengthInSeconds)

        // Try to find a track by artist and title (exact match)
        val existingTracks = if (artist.isNotBlank()) {
            trackRepository.findByTitleAndArtist(title, artist)
        } else {
            // Fallback to title only if artist is blank
            trackRepository.searchTracks(title).filter { it.trackName.equals(title, ignoreCase = true) }
        }
        var track = existingTracks.firstOrNull()

        if (track == null) {
            track = Track()
            track.trackName = title
            trackRepository.save(track)

            // Link metadata
            if (artist.isNotBlank()) {
                linkTrackToGroup(track.trackId!!, artist, BigDecimal("2.00")) // Artist
            }
            if (album.isNotBlank()) {
                linkTrackToGroup(track.trackId!!, album, BigDecimal("1.00")) // Media Name
            }
            if (genre.isNotBlank()) {
                linkTrackToGroup(track.trackId!!, genre, BigDecimal("6.00")) // Music Style
            }
        }

        val trackFile = TrackFile()
        trackFile.trackId = track.trackId
        trackFile.fileName = fileName
        trackFile.fileLocation = finalLocation
        trackFile.fileOnline = "Y"
        trackFile.duration = duration
        trackFile.backupDate = Timestamp(System.currentTimeMillis())
        trackFileRepository.save(trackFile)

        scanProgress.updateAndGet { it.copy(added = it.added + 1) }
    }

    @Transactional
    fun createPlaylist(name: String, trackIds: List<Long>): Groups {
        val playlistGroup = Groups()
        playlistGroup.groupName = name
        playlistGroup.groupTypeId = BigDecimal("4.00")
        playlistGroup.lastModification = Timestamp(System.currentTimeMillis())
        val saved = groupsRepository.save(playlistGroup)

        trackIds.forEachIndexed { index, trackId ->
            val tg = TrackGroup()
            tg.trackId = trackId
            tg.groupId = saved.groupId
            tg.sequence = BigDecimal(index)
            tg.lastModification = Timestamp(System.currentTimeMillis())
            trackGroupRepository.save(tg)
        }
        return saved
    }

    @Transactional
    fun deleteGroup(groupId: Long) {
        trackGroupRepository.deleteByGroupId(groupId)
        groupsRepository.deleteById(groupId)
    }
}
