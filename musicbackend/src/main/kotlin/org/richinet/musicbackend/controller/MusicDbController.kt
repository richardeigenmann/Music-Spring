package org.richinet.musicbackend.controller

import com.mpatric.mp3agic.Mp3File
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.richinet.musicbackend.data.entity.Groups
import org.richinet.musicbackend.data.projection.GroupProjection
import org.richinet.musicbackend.data.projection.PlaylistProjection
import org.richinet.musicbackend.data.repository.GroupTypeRepository
import org.richinet.musicbackend.data.repository.GroupsRepository
import org.richinet.musicbackend.data.repository.TrackFileRepository
import org.richinet.musicbackend.data.repository.TrackRepository
import org.richinet.musicbackend.service.MusicImportService
import org.richinet.musicbackend.service.ScanProgress
import org.richinet.musicbackend.service.TrackDataService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler
import java.io.File
import java.math.BigDecimal
import java.sql.Timestamp
import java.util.concurrent.Semaphore
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class FilterRequest(
  val mustHaveIds: List<Long> = emptyList(),
  val canHaveIds: List<Long> = emptyList(),
  val mustNotHaveIds: List<Long> = emptyList()
)

data class CreatePlaylistRequest(
  val name: String,
  val trackIds: List<Long>
)

data class CreateGroupRequest(
  val groupType: String,
  val groupName: String
)

@RestController
@RequestMapping("/api")
@CrossOrigin
class MusicDbController(
  private val trackRepository: TrackRepository,
  private val trackDataService: TrackDataService,
  private val groupsRepository: GroupsRepository,
  private val groupTypeRepository: GroupTypeRepository,
  private val trackFileRepository: TrackFileRepository,
  private val musicImportService: MusicImportService,
  private val jdbcTemplate: JdbcTemplate
) {
  private val logger = LoggerFactory.getLogger(MusicDbController::class.java)
  private val imageExtractionSemaphore = Semaphore(4) // Only 4 concurrent image extractions allowed

  @Value("\${app.music-directory:/mp3/}")
  private lateinit var musicDirectory: String

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

  @Autowired
  lateinit var audioHandler: ResourceHttpRequestHandler

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
  fun getTrackFile(
    @PathVariable id: Long,
    request: HttpServletRequest,
    response: HttpServletResponse
  ) {
    val trackFile = trackFileRepository.findById(id).orElse(null) ?: return
    val file = File(File(musicDirectory, trackFile.fileLocation ?: ""), trackFile.fileName ?: "")

    if (file.exists()) {
      val resource = FileSystemResource(file)

      // EXPLICITLY set the content type before the handler takes over
      response.contentType = "audio/mpeg"
      // Some GStreamer versions need this to recognize the stream can be indexed
      response.setHeader("Accept-Ranges", "bytes")
      // Pass the resource to the handler via a request attribute
      request.setAttribute("trackResource", resource)
      // 2. The "Anti-Stall" Headers for Amarok
      // Prevents GStreamer from trying to 'buffer' a previous seek's data
      response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
      response.setHeader(HttpHeaders.PRAGMA, "no-cache")
      response.setHeader(HttpHeaders.EXPIRES, "0")
    // 3. Keep-Alive is vital for GStreamer to reuse the connection after a seek
      response.setHeader(HttpHeaders.CONNECTION, "keep-alive")

      // Pass the resource to the handler via a request attribute
      request.setAttribute("trackResource", resource)
      // Let the pre-initialized bean handle the Range headers and 206 status
      audioHandler.handleRequest(request, response)
    } else {
      response.status = HttpServletResponse.SC_NOT_FOUND
    }
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
        File(musicDirectory, fileName)
      } else {
        File(File(musicDirectory, fileLocation), fileName)
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

  @GetMapping("/stats/groupUsage")
  fun getGroupUsageStats(): ResponseEntity<List<Map<String, Any>>> {
    return ResponseEntity.ok(groupsRepository.getGroupUsageStats())
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

  @Operation(summary = "Create a new group under a specific group type")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201", description = "Group created",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = Map::class))]
      ),
      ApiResponse(responseCode = "404", description = "Group type not found")
    ]
  )
  @PostMapping("/group")
  fun createGroup(@RequestBody request: CreateGroupRequest): ResponseEntity<Map<String, Any?>> {
    val groupType = groupTypeRepository.findByGroupTypeName(request.groupType)
      ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

    val newGroup = Groups().apply {
      this.groupName = request.groupName
      this.groupTypeId = groupType.groupTypeId
      this.lastModification = Timestamp(System.currentTimeMillis())
    }

    val savedGroup = groupsRepository.save(newGroup)
    return ResponseEntity.status(HttpStatus.CREATED).body(
      mapOf(
        "groupId" to savedGroup.groupId,
        "groupName" to savedGroup.groupName,
        "groupTypeName" to groupType.groupTypeName
      )
    )
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

  private fun sanitizeFilename(name: String): String {
    // Replace any character that is not a letter, number, space, hyphen, or underscore with an underscore.
    val sanitized = name.replace(Regex("[^a-zA-Z0-9 \\-_]"), "_")
    // Collapse multiple underscores or spaces into a single underscore.
    return sanitized.replace(Regex("[_ ]+"), "_")
  }

  @Operation(summary = "Download a group's tracks as an M3U playlist")
  @GetMapping("/group/{id}/m3u")
  fun downloadGroupAsM3u(@PathVariable id: Long, @RequestHeader(HttpHeaders.HOST) host: String): ResponseEntity<String> {
    val group = groupsRepository.findById(id).orElse(null) ?: return ResponseEntity.notFound().build()
    val tracks = trackRepository.findTracksByGroupId(id)
    val baseUrl = "http://$host/api"

    val m3uContent = buildString {
      appendLine("#EXTM3U")
      tracks.forEach { track ->
        val serializedTrack = trackDataService.serializeTrack(track)
        val files = serializedTrack["Files"] as? List<Map<String, Any?>>
        val firstFile = files?.firstOrNull()

        if (firstFile != null) {
          val artist = (serializedTrack["Artist"] as? Any)?.let {
            if (it is List<*>) it.joinToString(", ") else it.toString()
          } ?: "Unknown Artist"
          val title = serializedTrack["TrackName"] as? String ?: "Unknown Track"
          val duration = (firstFile["Duration"] as? Number)?.toInt() ?: -1
          val fileId = firstFile["FileId"]

          appendLine("#EXTINF:$duration,$artist - $title")
          appendLine("$baseUrl/trackFile/$fileId")
        }
      }
    }

    val groupTypeName = group.groupType?.groupTypeName ?: "Group"
    val safeFilename = sanitizeFilename("$groupTypeName - ${group.groupName}")

    val headers = HttpHeaders()
    headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${safeFilename}.m3u\"")
    headers.add(HttpHeaders.CONTENT_TYPE, "application/x-mpegurl")

    return ResponseEntity.ok().headers(headers).body(m3uContent)
  }

  @Operation(summary = "Download a group's tracks and playlist as a ZIP file")
  @GetMapping("/group/{id}/zip")
  fun downloadGroupAsZip(@PathVariable id: Long): ResponseEntity<StreamingResponseBody> {
    val group = groupsRepository.findById(id).orElse(null) ?: return ResponseEntity.notFound().build()
    val tracks = trackRepository.findTracksByGroupId(id)
    val serializedTracks = tracks.map { trackDataService.serializeTrack(it) }

    val groupTypeName = group.groupType?.groupTypeName ?: "Group"
    val safeFilenameBase = sanitizeFilename("$groupTypeName - ${group.groupName}")

    val streamingResponseBody = StreamingResponseBody { outputStream ->
      val zipOut = ZipOutputStream(outputStream)

      // 1. Create and add the M3U file to the zip
      val m3uContent = buildString {
        appendLine("#EXTM3U")
        serializedTracks.forEach { serializedTrack ->
          val files = serializedTrack["Files"] as? List<Map<String, Any?>>
          val firstFile = files?.firstOrNull()

          if (firstFile != null) {
            val artist = (serializedTrack["Artist"] as? Any)?.let {
              if (it is List<*>) it.joinToString(", ") else it.toString()
            } ?: "Unknown Artist"
            val title = serializedTrack["TrackName"] as? String ?: "Unknown Track"
            val duration = (firstFile["Duration"] as? Number)?.toInt() ?: -1
            val fileName = firstFile["FileName"] as? String

            if (fileName != null) {
              appendLine("#EXTINF:$duration,$artist - $title")
              appendLine(fileName) // Use relative paths for the ZIP
            }
          }
        }
      }
      val m3uEntry = ZipEntry("${safeFilenameBase}.m3u")
      zipOut.putNextEntry(m3uEntry)
      zipOut.write(m3uContent.toByteArray())
      zipOut.closeEntry()

      // 2. Add each MP3 file to the zip
      serializedTracks.forEach { serializedTrack ->
        val files = serializedTrack["Files"] as? List<Map<String, Any?>>
        files?.firstOrNull()?.let { fileMap ->
          val fileLocation = fileMap["FileLocation"] as? String ?: ""
          val fileName = fileMap["FileName"] as? String ?: ""
          val file = if (fileLocation.trim('/').isEmpty()) {
            File(musicDirectory, fileName)
          } else {
            File(File(musicDirectory, fileLocation.trim('/')), fileName)
          }

          if (file.exists()) {
            val zipEntry = ZipEntry(fileName)
            zipOut.putNextEntry(zipEntry)
            file.inputStream().use { it.copyTo(zipOut) }
            zipOut.closeEntry()
          }
        }
      }
      zipOut.close()
    }

    val headers = HttpHeaders()
    headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${safeFilenameBase}.zip\"")
    headers.add(HttpHeaders.CONTENT_TYPE, "application/zip")

    return ResponseEntity.ok().headers(headers).body(streamingResponseBody)
  }
}
