package org.richinet.musicbackend.service

import com.mpatric.mp3agic.Mp3File
import org.richinet.musicbackend.data.entity.Tag
import org.richinet.musicbackend.data.entity.Track
import org.richinet.musicbackend.data.entity.TrackFile
import org.richinet.musicbackend.data.entity.TrackTag
import org.richinet.musicbackend.data.repository.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.math.BigDecimal
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
    private val tagRepository: TagRepository,
    private val tagTypeRepository: TagTypeRepository,
    private val trackFileRepository: TrackFileRepository,
    private val trackTagRepository: TrackTagRepository,
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
            track.name = (trackData["trackName"] ?: trackData["TrackName"]) as? String
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

        val trackNameKey = if (trackData.containsKey("trackName")) "trackName" else if (trackData.containsKey("TrackName")) "TrackName" else null
        if (trackNameKey != null) {
            track.name = trackData[trackNameKey] as? String
            trackRepository.save(track)
        }

        val tagTypeCache = tagTypeRepository.findAll().associateBy { it.name }

        for ((key, value) in trackData) {
            if (key == "trackName" || key == "TrackName" || key == "trackId" || key == "TrackId") continue

            if (key == "files" || key == "Files") {
                @Suppress("UNCHECKED_CAST")
                val filesList = value as? List<Map<String, Any?>>
                filesList?.forEach { fileData ->
                    val fileId = ((fileData["fileId"] ?: fileData["FileId"]) as? Number)?.toLong() ?: return@forEach
                    val newFileName = (fileData["fileName"] ?: fileData["FileName"]) as? String ?: return@forEach

                    val trackFile = trackFileRepository.findById(fileId).orElse(null) ?: return@forEach

                    if (trackFile.fileName != newFileName) {
                        // Check if the new file actually exists on disk
                        val baseDir = File(musicDirectory)
                        val relativeLocation = trackFile.fileLocation?.trim('/') ?: ""
                        val targetDir = if (relativeLocation.isEmpty()) baseDir else File(baseDir, relativeLocation)
                        val newFile = File(targetDir, newFileName)

                        if (newFile.exists() && newFile.isFile) {
                            logger.info("Updating file mapping for ID $fileId to $newFileName")
                            trackFile.fileName = newFileName

                            // Update duration from the new file
                            try {
                                val mp3File = Mp3File(newFile)
                                trackFile.duration = BigDecimal(mp3File.lengthInSeconds)
                            } catch (e: Exception) {
                                logger.warn("Could not read duration from new file ${newFile.absolutePath}: ${e.message}")
                            }

                            trackFileRepository.save(trackFile)
                        } else {
                            logger.warn("Ignoring filename change for ID $fileId: file ${newFile.absolutePath} not found")
                        }
                    }
                }
                continue
            }

            val tagType = tagTypeCache[key]
            if (tagType != null && value != null) {
                // Surgical delete: only delete mappings for this specific TagType
                trackTagRepository.deleteByTrackIdAndTagTypeId(trackId, tagType.id!!)

                val tagNames = if (value is List<*>) value else listOf(value)
                for (tagNameObj in tagNames) {
                    val tagName = tagNameObj as? String ?: continue
                    linkTrackToTag(trackId, tagName, tagType.id!!)
                }
            }
        }
    }

    @Transactional
    fun deleteTrack(trackId: Long) {
        trackTagRepository.deleteByTrackId(trackId)
        trackFileRepository.deleteByTrackId(trackId)
        trackRepository.deleteById(trackId)
    }

    private fun processTrackData(track: Track, trackData: Map<String, Any?>) {
        val tagTypeCache = tagTypeRepository.findAll().associateBy { it.name }

        for ((key, value) in trackData) {
            if (key == "trackName" || key == "TrackName" || key == "trackId" || key == "TrackId" || key == "files" || key == "Files") continue

            val tagType = tagTypeCache[key]
            if (tagType != null && value != null) {
                val tagNames = if (value is List<*>) value else listOf(value)
                for (tagNameObj in tagNames) {
                    val tagName = tagNameObj as? String ?: continue
                    linkTrackToTag(track.id!!, tagName, tagType.id!!)
                }
            }
        }
    }

    private fun linkTrackToTag(trackId: Long, tagName: String, tagTypeId: Long) {
        var tag = tagRepository.findByNameAndTagTypeId(tagName, tagTypeId)
        if (tag == null) {
            tag = Tag()
            tag.tagTypeId = tagTypeId
            tag.name = tagName
            tagRepository.save(tag)
        }

        val trackTag = TrackTag()
        trackTag.trackId = trackId
        trackTag.tagId = tag.id
        trackTag.sequence = 0
        trackTagRepository.save(trackTag)
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
            trackRepository.searchTracks(title).filter { it.name.equals(title, ignoreCase = true) }
        }
        var track = existingTracks.firstOrNull()

        if (track == null) {
            track = Track()
            track.name = title
            trackRepository.save(track)

            // Link metadata
            if (artist.isNotBlank()) {
                linkTrackToTag(track.id!!, artist, 2L) // Artist
            }
            if (album.isNotBlank()) {
                linkTrackToTag(track.id!!, album, 1L) // Media Name
            }
            if (genre.isNotBlank()) {
                linkTrackToTag(track.id!!, genre, 6L) // Genre
            }
        }

        val trackFile = TrackFile()
        trackFile.trackId = track.id
        trackFile.fileName = fileName
        trackFile.fileLocation = finalLocation
        trackFile.duration = duration
        trackFileRepository.save(trackFile)

        scanProgress.updateAndGet { it.copy(added = it.added + 1) }
    }

    @Transactional
    fun createTagWithTracks(name: String, tagTypeId: Long, trackIds: List<Long>): Tag {
        val tag = Tag()
        tag.name = name
        tag.tagTypeId = tagTypeId
        val saved = tagRepository.save(tag)

        trackIds.forEachIndexed { index, trackId ->
            val tt = TrackTag()
            tt.trackId = trackId
            tt.tagId = saved.id
            tt.sequence = index
            trackTagRepository.save(tt)
        }
        return saved
    }

    @Transactional
    fun deleteTag(tagId: Long) {
        trackTagRepository.deleteByTagId(tagId)
        tagRepository.deleteById(tagId)
    }
}
