package org.richinet.musicbackend.controller

import com.mpatric.mp3agic.Mp3File
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.richinet.musicbackend.data.projection.GroupProjection
import org.richinet.musicbackend.data.projection.PlaylistProjection
import org.richinet.musicbackend.data.repository.GroupsRepository
import org.richinet.musicbackend.data.repository.TrackFileRepository
import org.richinet.musicbackend.data.repository.TrackRepository
import org.richinet.musicbackend.service.MusicImportService
import org.richinet.musicbackend.service.ScanProgress
import org.richinet.musicbackend.service.TrackDataService
import org.slf4j.LoggerFactory
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
import java.util.concurrent.Semaphore

private const val MUSIC_DIRECTORY = "/mp3/"

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
  private val logger = LoggerFactory.getLogger(MusicDbController::class.java)
  private val imageExtractionSemaphore = Semaphore(4) // Only 4 concurrent image extractions allowed

  @Operation(summary = "Returns serialized track data including metadata.")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200", description = "Found the track",
        content = [Content(
          mediaType = "application/json", schema = Schema(implementation = Map::class), examples = [
            ExampleObject(
              value = """
                {
                  "TrackId": 1,
                  "TrackName": "Song Title",
                  "Artist": "Artist Name",
                  "Album": "Album Name",
                  "Files": [
                    {
                      "FileId": 101,
                      "FileName": "song.mp3",
                      "FileLocation": "/path/to/file/",
                      "FileOnline": "Y",
                      "Duration": 300,
                      "BackupDate": "2023-01-01T12:00:00"
                    }
                  ]
                }
            """
            )
          ]
        )]
      ),
      ApiResponse(responseCode = "404", description = "Track not found", content = [Content()])
    ]
  )
  @GetMapping("/track/{id}")
  fun getTrack(@PathVariable id: Long): ResponseEntity<Map<String, Any?>> {
    val track = trackRepository.findById(id)
    return if (track.isPresent) {
      ResponseEntity.ok(trackDataService.serializeTrack(track.get()))
    } else {
      ResponseEntity.notFound().build()
    }
  }

  @Operation(summary = "Update a track and its relationships")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Track updated successfully"),
      ApiResponse(responseCode = "404", description = "Track not found")
    ]
  )
  @PostMapping("/track/{id}")
  fun updateTrack(@PathVariable id: Long, @RequestBody trackData: Map<String, Any?>): ResponseEntity<Unit> {
    return try {
      musicImportService.updateTrack(id, trackData)
      ResponseEntity.ok().build()
    } catch (e: Exception) {
      logger.error("Failed to update track $id. Exception: ${e.message}")
      ResponseEntity.notFound().build()
    }
  }

  @Operation(summary = "Delete a track and its relationships")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Track deleted successfully"),
      ApiResponse(responseCode = "404", description = "Track not found")
    ]
  )
  @DeleteMapping("/track/{id}")
  fun deleteTrack(@PathVariable id: Long): ResponseEntity<Unit> {
    return try {
      musicImportService.deleteTrack(id)
      ResponseEntity.ok().build()
    } catch (e: Exception) {
      logger.error("Failed to delete track $id. Exception: ${e.message}")
      ResponseEntity.notFound().build()
    }
  }

  @Operation(summary = "Get all playlists")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200", description = "Found the playlists",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = PlaylistProjection::class))]
      )
    ]
  )
  @GetMapping("/playlists")
  fun getPlaylists(): ResponseEntity<List<PlaylistProjection>> {
    val playlists = groupsRepository.findPlaylistsByTypeId(BigDecimal(4))
    return ResponseEntity.ok(playlists)
  }

  @Operation(summary = "Get all editable groups with their types")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200", description = "Found the groups",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = GroupProjection::class))]
      )
    ]
  )
  @GetMapping("/groups")
  fun getGroups(): ResponseEntity<List<GroupProjection>> {
    val groups = groupsRepository.findAllEditableGroups()
    return ResponseEntity.ok(groups)
  }

  @Operation(summary = "Get all tracks belonging to a group")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200", description = "Found the tracks",
        content = [Content(
          mediaType = "application/json", schema = Schema(implementation = List::class), examples = [
            ExampleObject(
              value = """
                [
                  {
                    "TrackId": 1,
                    "TrackName": "Song Title",
                    "Artist": "Artist Name",
                    "Album": "Album Name",
                    "Files": [
                      {
                        "FileId": 101,
                        "FileName": "song.mp3",
                        "FileLocation": "/path/to/file/",
                        "FileOnline": "Y",
                        "Duration": 300,
                        "BackupDate": "2023-01-01T12:00:00"
                      }
                    ]
                  }
                ]
            """
            )
          ]
        )]
      )
    ]
  )
  @GetMapping("/tracksByGroup/{id}")
  fun getTracksByGroup(@PathVariable id: Long): ResponseEntity<List<Map<String, Any?>>> {
    val tracks = trackRepository.findTracksByGroupId(id)
    val serializedTracks = tracks.map { trackDataService.serializeTrack(it) }
    return ResponseEntity.ok(serializedTracks)
  }

  @Operation(summary = "Search for tracks by name or group name")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200", description = "Found matching tracks",
        content = [Content(
          mediaType = "application/json", schema = Schema(implementation = List::class), examples = [
            ExampleObject(
              value = """
                [
                  {
                    "TrackId": 1,
                    "TrackName": "Song Title",
                    "Artist": "Artist Name",
                    "Album": "Album Name",
                    "Files": [
                      {
                        "FileId": 101,
                        "FileName": "song.mp3",
                        "FileLocation": "/path/to/file/",
                        "FileOnline": "Y",
                        "Duration": 300,
                        "BackupDate": "2023-01-01T12:00:00"
                      }
                    ]
                  }
                ]
            """
            )
          ]
        )]
      )
    ]
  )
  @GetMapping("/trackSearch")
  fun searchTracks(@RequestParam query: String): ResponseEntity<List<Map<String, Any?>>> {
    val tracks = trackRepository.searchTracks(query)
    val serializedTracks = tracks.map { trackDataService.serializeTrack(it) }
    return ResponseEntity.ok(serializedTracks)
  }

  @Operation(summary = "Stream audio file")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200", description = "File found and streaming",
        content = [Content(mediaType = "audio/mpeg")]
      ),
      ApiResponse(responseCode = "404", description = "File not found", content = [Content()])
    ]
  )
  @GetMapping("/trackFile/{id}")
  fun getTrackFile(@PathVariable id: Long): ResponseEntity<Resource> {
    val trackFile = trackFileRepository.findById(id)
    if (trackFile.isPresent) {
      val fileEntity = trackFile.get()
      val fileLocation = fileEntity.fileLocation?.trim('/') ?: ""
      val fileName = fileEntity.fileName ?: ""
      val file = if (fileLocation.isEmpty()) {
        File(MUSIC_DIRECTORY, fileName)
      } else {
        File(File(MUSIC_DIRECTORY, fileLocation), fileName)
      }
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

  @Operation(summary = "Get album art from track file")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200", description = "Image found",
        content = [Content(mediaType = "image/jpeg"), Content(mediaType = "image/png")]
      ),
      ApiResponse(responseCode = "404", description = "File not found", content = [Content()])
    ]
  )
  @GetMapping("/trackFileImage/{id}")
  fun getTrackFileImage(@PathVariable id: Long): ResponseEntity<Resource> {
    val trackFile = trackFileRepository.findById(id)
    if (trackFile.isPresent) {
      val fileEntity = trackFile.get()
      val fileLocation = fileEntity.fileLocation?.trim('/') ?: ""
      val fileName = fileEntity.fileName ?: ""
      val file = if (fileLocation.isEmpty()) {
        File(MUSIC_DIRECTORY, fileName)
      } else {
        File(File(MUSIC_DIRECTORY, fileLocation), fileName)
      }
      if (file.exists()) {
        try {
          imageExtractionSemaphore.acquire()
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
          } finally {
            imageExtractionSemaphore.release()
          }
        } catch (e: Throwable) {
          logger.warn("Failed to extract image from file: ${file.absolutePath}. Reason: ${e.message}")
        }
      }
    }
    val placeholder = ClassPathResource("static/images/placeholder.png")
    return if (placeholder.exists()) {
      ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(placeholder)
    } else {
      ResponseEntity.notFound().build()
    }
  }

  @Operation(summary = "Get total count of tracks")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200", description = "Count returned",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = Map::class))]
      )
    ]
  )
  @GetMapping("/totalTrackCount")
  fun getTotalTrackCount(): ResponseEntity<Map<String, Long>> {
    val count = trackRepository.count()
    return ResponseEntity.ok(mapOf("count" to count))
  }

  @Operation(summary = "Start scanning for new MP3 files")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Scan started")
    ]
  )
  @PostMapping("/scanTracks")
  fun scanTracks(): ResponseEntity<Unit> {
    musicImportService.startMp3Scan()
    return ResponseEntity.ok().build()
  }

  @Operation(summary = "Get current scan progress")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200", description = "Progress returned",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ScanProgress::class))]
      )
    ]
  )
  @GetMapping("/scanProgress")
  fun getScanProgress(): ResponseEntity<ScanProgress> {
    return ResponseEntity.ok(musicImportService.getScanProgress())
  }

  @Operation(summary = "Get tracks that are not in any group")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200", description = "Found unclassified tracks",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = List::class))]
      )
    ]
  )
  @GetMapping("/unclassifiedTracks")
  fun getUnclassifiedTracks(): ResponseEntity<List<Map<String, Any?>>> {
    val tracks = trackRepository.findUnclassifiedTracks()
    val serializedTracks = tracks.map { trackDataService.serializeTrack(it) }
    return ResponseEntity.ok(serializedTracks)
  }

  @Operation(summary = "Filter tracks based on group criteria")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200", description = "Found matching tracks",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = List::class))]
      )
    ]
  )
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
    return ResponseEntity.ok(trackRepository.findAllById(trackIds).map { trackDataService.serializeTrack(it) })
  }

  @Operation(summary = "Create a new playlist from a list of tracks")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200", description = "Playlist created",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = Map::class))]
      )
    ]
  )
  @PostMapping("/createPlaylist")
  fun createPlaylist(@RequestBody request: CreatePlaylistRequest): ResponseEntity<Map<String, Any?>> {
    val groups = musicImportService.createPlaylist(request.name, request.trackIds)
    return ResponseEntity.ok(mapOf("groupId" to groups.groupId, "groupName" to groups.groupName))
  }

  @Operation(summary = "Delete a group/playlist")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Group deleted")
    ]
  )
  @DeleteMapping("/group/{id}")
  fun deleteGroup(@PathVariable id: Long): ResponseEntity<Unit> {
    musicImportService.deleteGroup(id)
    return ResponseEntity.ok().build()
  }
}
