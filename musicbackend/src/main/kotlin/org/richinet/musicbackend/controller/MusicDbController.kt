package org.richinet.musicbackend.controller

import com.mpatric.mp3agic.Mp3File
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.richinet.musicbackend.data.entity.Groups
import org.richinet.musicbackend.data.projection.GroupProjection
import org.richinet.musicbackend.data.projection.PlaylistProjection
import org.richinet.musicbackend.data.repository.GroupsRepository
import org.richinet.musicbackend.data.repository.TrackFileRepository
import org.richinet.musicbackend.data.repository.TrackRepository
import org.richinet.musicbackend.service.MusicImportService
import org.richinet.musicbackend.service.ScanProgress
import org.richinet.musicbackend.service.TrackDataService
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.web.bind.annotation.*
import java.io.File
import java.math.BigDecimal
import java.nio.file.Files

private const val MUSIC_DIRECTORY = "/richi"

data class FilterRequest(
    val mustHaveIds: List<Long> = emptyList(),
    val canHaveIds: List<Long> = emptyList(),
    val mustNotHaveIds: List<Long> = emptyList()
)

data class CreatePlaylistRequest(
    val name: String,
    val trackIds: List<Long>
)

@RestController
@RequestMapping("/api")
@CrossOrigin
class MusicDbController(
    private val trackRepository: TrackRepository,
    private val trackDataService: TrackDataService,
    private val groupsRepository: GroupsRepository,
    private val trackFileRepository: TrackFileRepository,
    private val musicImportService: MusicImportService,
    private val jdbcTemplate: JdbcTemplate
) {

    @GetMapping("/track/{id}")
    fun getTrack(@PathVariable id: Long): ResponseEntity<Map<String, Any?>> {
        val track = trackRepository.findById(id)
        return if (track.isPresent) {
            ResponseEntity.ok(trackDataService.serializeTrack(track.get()))
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/track/{id}")
    fun updateTrack(@PathVariable id: Long, @RequestBody trackData: Map<String, Any?>): ResponseEntity<Void> {
        return try {
            musicImportService.updateTrack(id, trackData)
            ResponseEntity.ok().build()
        } catch (e: Exception) {
            ResponseEntity.notFound().build()
        }
    }

    @DeleteMapping("/track/{id}")
    fun deleteTrack(@PathVariable id: Long): ResponseEntity<Void> {
        return try {
            musicImportService.deleteTrack(id)
            ResponseEntity.ok().build()
        } catch (e: Exception) {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/playlists")
    fun getPlaylists(): ResponseEntity<List<PlaylistProjection>> {
        val playlists = groupsRepository.findPlaylistsByTypeId(BigDecimal(4))
        return ResponseEntity.ok(playlists)
    }

    @GetMapping("/groups")
    fun getGroups(): ResponseEntity<List<GroupProjection>> {
        val groups = groupsRepository.findAllEditableGroups()
        return ResponseEntity.ok(groups)
    }

    @GetMapping("/tracksByGroup/{id}")
    fun getTracksByGroup(@PathVariable id: Long): ResponseEntity<List<Map<String, Any?>>> {
        val tracks = trackRepository.findTracksByGroupId(id)
        val serializedTracks = tracks.map { trackDataService.serializeTrack(it) }
        return ResponseEntity.ok(serializedTracks)
    }

    @GetMapping("/trackSearch")
    fun searchTracks(@RequestParam query: String): ResponseEntity<List<Map<String, Any?>>> {
        val tracks = trackRepository.searchTracks(query)
        val serializedTracks = tracks.map { trackDataService.serializeTrack(it) }
        return ResponseEntity.ok(serializedTracks)
    }

    @GetMapping("/trackFile/{id}")
    fun getTrackFile(@PathVariable id: Long): ResponseEntity<Resource> {
        val trackFile = trackFileRepository.findById(id)
        if (trackFile.isPresent) {
            val fileEntity = trackFile.get()
            val filePath = MUSIC_DIRECTORY + (fileEntity.fileLocation ?: "") + (fileEntity.fileName ?: "")
            val file = File(filePath)
            if (file.exists()) {
                val resource = FileSystemResource(file)
                val contentType = Files.probeContentType(file.toPath()) ?: "audio/mpeg"
                return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"${file.name}\"")
                    .body(resource)
            }
        }
        return ResponseEntity.notFound().build()
    }

    @GetMapping("/trackFileImage/{id}")
    fun getTrackFileImage(@PathVariable id: Long): ResponseEntity<Resource> {
        val trackFile = trackFileRepository.findById(id)
        if (trackFile.isPresent) {
            val fileEntity = trackFile.get()
            val filePath = MUSIC_DIRECTORY + (fileEntity.fileLocation ?: "") + (fileEntity.fileName ?: "")
            val file = File(filePath)
            if (file.exists()) {
                try {
                    val mp3file = Mp3File(file)
                    if (mp3file.hasId3v2Tag()) {
                        val id3v2Tag = mp3file.id3v2Tag
                        val albumImage = id3v2Tag.albumImage
                        if (albumImage != null) {
                            val mimeType = id3v2Tag.albumImageMimeType ?: "image/jpeg"
                            return ResponseEntity.ok()
                                .contentType(MediaType.parseMediaType(mimeType))
                                .body(ByteArrayResource(albumImage))
                        }
                    }
                } catch (e: Exception) {}
            }
        }
        val placeholder = ClassPathResource("static/images/placeholder.png")
        return if (placeholder.exists()) {
             ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(placeholder)
        } else {
             ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/totalTrackCount")
    fun getTotalTrackCount(): ResponseEntity<Map<String, Long>> {
        val count = trackRepository.count()
        return ResponseEntity.ok(mapOf("count" to count))
    }

    @PostMapping("/scanTracks")
    fun scanTracks(): ResponseEntity<Void> {
        musicImportService.startMp3Scan()
        return ResponseEntity.ok().build()
    }

    @GetMapping("/scanProgress")
    fun getScanProgress(): ResponseEntity<ScanProgress> {
        return ResponseEntity.ok(musicImportService.getScanProgress())
    }

    @GetMapping("/unclassifiedTracks")
    fun getUnclassifiedTracks(): ResponseEntity<List<Map<String, Any?>>> {
        val tracks = trackRepository.findUnclassifiedTracks()
        val serializedTracks = tracks.map { trackDataService.serializeTrack(it) }
        return ResponseEntity.ok(serializedTracks)
    }

    @PostMapping("/filterTracks")
    fun filterTracks(@RequestBody request: FilterRequest): ResponseEntity<List<Map<String, Any?>>> {
        if (request.mustHaveIds.isEmpty() && request.canHaveIds.isEmpty() && request.mustNotHaveIds.isEmpty()) {
            return ResponseEntity.ok(emptyList())
        }

        val sql = StringBuilder("SELECT DISTINCT t.Track_Id FROM Track t ")
        val conditions = mutableListOf<String>()

        if (request.mustHaveIds.isNotEmpty()) {
            val ids = request.mustHaveIds.joinToString(",")
            conditions.add("t.Track_Id IN (SELECT tg.Track_Id FROM Track_Group tg WHERE tg.Group_Id IN ($ids) GROUP BY tg.Track_Id HAVING COUNT(DISTINCT tg.Group_Id) = ${request.mustHaveIds.size})")
        }

        if (request.canHaveIds.isNotEmpty()) {
            val ids = request.canHaveIds.joinToString(",")
            conditions.add("t.Track_Id IN (SELECT tg.Track_Id FROM Track_Group tg WHERE tg.Group_Id IN ($ids))")
        }

        if (request.mustNotHaveIds.isNotEmpty()) {
            val ids = request.mustNotHaveIds.joinToString(",")
            conditions.add("t.Track_Id NOT IN (SELECT tg.Track_Id FROM Track_Group tg WHERE tg.Group_Id IN ($ids))")
        }

        if (conditions.isNotEmpty()) {
            sql.append(" WHERE ").append(conditions.joinToString(" AND "))
        }

        val trackIds = jdbcTemplate.queryForList(sql.toString(), Long::class.java)
        val tracks = trackRepository.findAllById(trackIds)
        return ResponseEntity.ok(tracks.map { trackDataService.serializeTrack(it) })
    }

    @PostMapping("/createPlaylist")
    fun createPlaylist(@RequestBody request: CreatePlaylistRequest): ResponseEntity<Map<String, Any?>> {
        val groups = musicImportService.createPlaylist(request.name, request.trackIds)
        return ResponseEntity.ok(mapOf("groupId" to groups.groupId, "groupName" to groups.groupName))
    }
}
