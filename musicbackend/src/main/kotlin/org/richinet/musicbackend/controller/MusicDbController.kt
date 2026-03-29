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
import org.richinet.musicbackend.data.entity.Tag
import org.richinet.musicbackend.data.projection.TagProjection
import org.richinet.musicbackend.data.repository.TagTypeRepository
import org.richinet.musicbackend.data.repository.TagRepository
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
import java.util.concurrent.Semaphore
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class FilterRequest(
  val mustHaveIds: List<Long> = emptyList(),
  val canHaveIds: List<Long> = emptyList(),
  val mustNotHaveIds: List<Long> = emptyList()
)

data class CreateTagRequest(
  val tagType: String,
  val tagName: String,
  val trackIds: List<Long> = emptyList()
)

@RestController
@RequestMapping("/api")
@CrossOrigin
class MusicDbController(
  private val trackRepository: TrackRepository,
  private val trackDataService: TrackDataService,
  private val tagRepository: TagRepository,
  private val tagTypeRepository: TagTypeRepository,
  private val trackFileRepository: TrackFileRepository,
  private val musicImportService: MusicImportService,
  private val jdbcTemplate: JdbcTemplate,
  private val jacksonJsonMapper: tools.jackson.databind.ObjectMapper
) {
  private val logger = LoggerFactory.getLogger(MusicDbController::class.java)
  private val imageExtractionSemaphore = Semaphore(4) // Only 4 concurrent image extractions allowed

  @Value("\${app.music-directory}")
  private lateinit var musicDirectory: String

  @Operation(
    summary = "Returns the Music database as a JSON string for download",
    description = "Returns the entire Music database as a JSON file, suitable for downloading via browser or curl."
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "A JSON response with the contents of the Music Database")
    ]
  )
  @GetMapping("/tracks/dump")
  fun dumpMusicData(response: jakarta.servlet.http.HttpServletResponse): List<Map<String, Any?>> {
    response.setHeader("Content-Disposition", "attachment; filename=\"tracks.json\"")
    return trackRepository.findAll().map { trackDataService.serializeTrack(it) }
  }

  @Operation(
    summary = "Import music data in bulk",
    description = "Imports music data from an uploaded JSON file."
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Data imported successfully"),
      ApiResponse(responseCode = "400", description = "Invalid request or file format"),
      ApiResponse(responseCode = "500", description = "Failed to import data")
    ]
  )
  @PostMapping("/tracks/bulk", consumes = ["multipart/form-data"])
  fun importTracks(
      @RequestPart file: org.springframework.web.multipart.MultipartFile
  ): ResponseEntity<String> {
    return try {
      val fileData: List<Map<String, Any>> = jacksonJsonMapper.readValue(file.inputStream, object : tools.jackson.core.type.TypeReference<List<Map<String, Any>>>() {})
      musicImportService.importMusicData(fileData)
      ResponseEntity.ok("Successfully imported data from file: ${file.originalFilename}")
    } catch (e: Exception) {
      ResponseEntity.status(500).body("Failed to parse or import data: ${e.message}")
    }
  }

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
                  "trackId": 1,
                  "trackName": "Song Title",
                  "Artist": "Artist Name",
                  "Album": "Album Name",
                  "files": [
                    {
                      "fileId": 101,
                      "fileName": "song.mp3",
                      "fileLocation": "/path/to/file/",
                      "duration": 300
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

  @Operation(summary = "Get all editable tags with their types")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200", description = "Found the tags",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = TagProjection::class))]
      )
    ]
  )
  @GetMapping("/tags")
  fun getTags(): ResponseEntity<List<TagProjection>> {
    val tags = tagRepository.findAllEditableTags()
    return ResponseEntity.ok(tags)
  }

  @Operation(summary = "Get all tracks belonging to a tag")
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
                    "trackId": 1,
                    "trackName": "Song Title",
                    "Artist": "Artist Name",
                    "Album": "Album Name",
                    "files": [
                      {
                        "fileId": 101,
                        "fileName": "song.mp3",
                        "fileLocation": "/path/to/file/",
                        "duration": 300
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
  @GetMapping("/tracksByTag/{id}")
  fun getTracksByTag(@PathVariable id: Long): ResponseEntity<List<Map<String, Any?>>> {
    val tracks = trackRepository.findTracksByTagId(id)
    val serializedTracks = tracks.map { trackDataService.serializeTrack(it) }
    return ResponseEntity.ok(serializedTracks)
  }

  @Operation(summary = "Search for tracks by name or tag name")
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
                    "trackId": 1,
                    "trackName": "Song Title",
                    "Artist": "Artist Name",
                    "Album": "Album Name",
                    "files": [
                      {
                        "fileId": 101,
                        "fileName": "song.mp3",
                        "fileLocation": "/path/to/file/",
                        "duration": 300
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
    logger.info("Fetching track file with id: $id")
    val trackFile = trackFileRepository.findById(id).orElse(null)
    if (trackFile == null) {
      logger.error("Failed to find track file $id in the database")
      response.status = HttpServletResponse.SC_NOT_FOUND
      return
    }

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
      logger.error("Failed find the file $file")
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
    logger.info("Fetching track file image with id: $id")
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
      logger.info("Searching for image in file: ${file.absolutePath}")
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

  @Operation(summary = "Start scanning for new MP3 files in directory defined in property app.music-directory")
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

  @GetMapping("/stats/tagUsage")
  fun getTagUsageStats(): ResponseEntity<List<Map<String, Any>>> {
    return ResponseEntity.ok(tagRepository.getTagUsageStats())
  }

  @Operation(summary = "Get tracks that are not in any tag")
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

  @Operation(summary = "Filter tracks based on tag criteria")
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

    val sql = StringBuilder("SELECT DISTINCT t.id FROM track t ")
    val conditions = mutableListOf<String>()

    if (request.mustHaveIds.isNotEmpty()) {
      val ids = request.mustHaveIds.joinToString(",")
      conditions.add("t.id IN (SELECT tt.track_id FROM track_tag tt WHERE tt.tag_id IN ($ids) GROUP BY tt.track_id HAVING COUNT(DISTINCT tt.tag_id) = ${request.mustHaveIds.size})")
    }

    if (request.canHaveIds.isNotEmpty()) {
      val ids = request.canHaveIds.joinToString(",")
      conditions.add("t.id IN (SELECT tt.track_id FROM track_tag tt WHERE tt.tag_id IN ($ids))")
    }

    if (request.mustNotHaveIds.isNotEmpty()) {
      val ids = request.mustNotHaveIds.joinToString(",")
      conditions.add("t.id NOT IN (SELECT tt.track_id FROM track_tag tt WHERE tt.tag_id IN ($ids))")
    }

    if (conditions.isNotEmpty()) {
      sql.append(" WHERE ").append(conditions.joinToString(" AND "))
    }

    val trackIds = jdbcTemplate.queryForList(sql.toString(), Long::class.java)
    return ResponseEntity.ok(trackRepository.findAllById(trackIds.filterNotNull()).map { trackDataService.serializeTrack(it) })
  }

  @Operation(summary = "Create a new tag under a specific tag type, optionally linking it to tracks")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201", description = "Tag created",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = Map::class))]
      ),
      ApiResponse(responseCode = "404", description = "Tag type not found")
    ]
  )
  @PostMapping("/tag")
  fun createTag(@RequestBody request: CreateTagRequest): ResponseEntity<Map<String, Any?>> {
    val tagType = tagTypeRepository.findByName(request.tagType)
      ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

    val savedTag = if (request.trackIds.isNotEmpty()) {
      musicImportService.createTagWithTracks(request.tagName, tagType.id!!, request.trackIds)
    } else {
      val newTag = Tag().apply {
        this.name = request.tagName
        this.tagTypeId = tagType.id
      }
      tagRepository.save(newTag)
    }

    return ResponseEntity.status(HttpStatus.CREATED).body(
      mapOf(
        "tagId" to savedTag.id,
        "tagName" to savedTag.name,
        "tagTypeName" to tagType.name
      )
    )
  }

  @Operation(summary = "Delete a tag/playlist")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Tag deleted")
    ]
  )
  @DeleteMapping("/tag/{id}")
  fun deleteTag(@PathVariable id: Long): ResponseEntity<Unit> {
    musicImportService.deleteTag(id)
    return ResponseEntity.ok().build()
  }

  private fun sanitizeFilename(name: String): String {
    // Replace any character that is not a letter, number, space, hyphen, or underscore with an underscore.
    val sanitized = name.replace(Regex("[^a-zA-Z0-9 _-]"), "_")
    // Collapse multiple underscores or spaces into a single underscore.
    return sanitized.replace(Regex("[_ ]+"), "_")
  }

  @Operation(summary = "Download a tag's tracks as an M3U playlist")
  @GetMapping("/tag/{id}/m3u")
  fun downloadTagAsM3u(@PathVariable id: Long, @RequestHeader(HttpHeaders.HOST) host: String): ResponseEntity<String> {
    val tag = tagRepository.findById(id).orElse(null) ?: return ResponseEntity.notFound().build()
    val tracks = trackRepository.findTracksByTagId(id)
    val baseUrl = "http://$host/api"

    val m3uContent = buildString {
      appendLine("#EXTM3U")
      tracks.forEach { track ->
        val serializedTrack = trackDataService.serializeTrack(track)
        val files = serializedTrack["files"] as? List<*>
        @Suppress("UNCHECKED_CAST")
        val firstFile = files?.firstOrNull() as? Map<String, Any?>

        if (firstFile != null) {
          val artist = (serializedTrack["Artist"])?.let {
            if (it is List<*>) it.joinToString(", ") else it.toString()
          } ?: "Unknown Artist"
          val title = serializedTrack["trackName"] as? String ?: "Unknown Track"
          val duration = (firstFile["duration"] as? Number)?.toInt() ?: -1
          val fileId = firstFile["fileId"]

          appendLine("#EXTINF:$duration,$artist - $title")
          appendLine("$baseUrl/trackFile/$fileId")
        }
      }
    }

    val tagTypeName = tag.tagType?.name ?: "Tag"
    val safeFilename = sanitizeFilename("$tagTypeName - ${tag.name}")

    val headers = HttpHeaders()
    headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${safeFilename}.m3u\"")
    headers.add(HttpHeaders.CONTENT_TYPE, "application/x-mpegurl")

    return ResponseEntity.ok().headers(headers).body(m3uContent)
  }

  @Operation(summary = "Download a tag's tracks and playlist as a ZIP file")
  @GetMapping("/tag/{id}/zip")
  fun downloadTagAsZip(@PathVariable id: Long): ResponseEntity<StreamingResponseBody> {
    val tag = tagRepository.findById(id).orElse(null) ?: return ResponseEntity.notFound().build()
    val tracks = trackRepository.findTracksByTagId(id)
    val serializedTracks = tracks.map { trackDataService.serializeTrack(it) }

    val tagTypeName = tag.tagType?.name ?: "Tag"
    val safeFilenameBase = sanitizeFilename("$tagTypeName - ${tag.name}")

    val streamingResponseBody = StreamingResponseBody { outputStream ->
      val zipOut = ZipOutputStream(outputStream)

      // 1. Create and add the M3U file to the zip
      val m3uContent = buildString {
        appendLine("#EXTM3U")
        serializedTracks.forEach { serializedTrack ->
          val files = serializedTrack["files"] as? List<*>
          @Suppress("UNCHECKED_CAST")
          val firstFile = files?.firstOrNull() as? Map<String, Any?>

          if (firstFile != null) {
            val artist = serializedTrack["Artist"]?.let {

              if (it is List<*>) it.joinToString(", ") else it.toString()
            } ?: "Unknown Artist"
            val title = serializedTrack["trackName"] as? String ?: "Unknown Track"
            val duration = (firstFile["duration"] as? Number)?.toInt() ?: -1
            val fileName = firstFile["fileName"] as? String

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
        val files = serializedTrack["files"] as? List<*>
        @Suppress("UNCHECKED_CAST")
        val firstFile = files?.firstOrNull() as? Map<String, Any?>
        firstFile?.let { fileMap ->
          val fileLocation = fileMap["fileLocation"] as? String ?: ""
          val fileName = fileMap["fileName"] as? String ?: ""
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
