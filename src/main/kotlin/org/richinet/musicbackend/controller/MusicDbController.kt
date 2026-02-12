package org.richinet.musicbackend.controller

import com.mpatric.mp3agic.Mp3File
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.richinet.musicbackend.data.projection.PlaylistProjection
import org.richinet.musicbackend.data.repository.GroupsRepository
import org.richinet.musicbackend.data.repository.TrackFileRepository
import org.richinet.musicbackend.data.repository.TrackRepository
import org.richinet.musicbackend.service.MusicImportService
import org.richinet.musicbackend.service.TrackDataService
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.io.File
import java.math.BigDecimal
import java.nio.file.Files

private const val MUSIC_DIRECTORY = "/richi"

@RestController
@RequestMapping("/api")
class MusicDbController(
    private val trackRepository: TrackRepository,
    private val trackDataService: TrackDataService,
    private val groupsRepository: GroupsRepository,
    private val trackFileRepository: TrackFileRepository,
    private val musicImportService: MusicImportService
) {

    @Operation(summary = "Returns serialized track data including metadata.")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Found the track",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = Map::class), examples = [
                ExampleObject(value = """
                {
                  "TrackName": "Song Title",
                  "Artist": "Artist Name",
                  "Album": "Album Name",
                  "Files": [
                    {
                      "FileName": "song.mp3",
                      "FileLocation": "/path/to/file/",
                      "FileOnline": "Y",
                      "Duration": 300,
                      "BackupDate": "2023-01-01T12:00:00"
                    }
                  ]
                }
            """)
            ])]),
        ApiResponse(responseCode = "404", description = "Track not found", content = [Content()])
    ])
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
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Track updated successfully"),
        ApiResponse(responseCode = "404", description = "Track not found")
    ])
    @PostMapping("/track/{id}")
    fun updateTrack(@PathVariable id: Long, @RequestBody trackData: Map<String, Any?>): ResponseEntity<Void> {
        return try {
            musicImportService.updateTrack(id, trackData)
            ResponseEntity.ok().build()
        } catch (e: Exception) {
            ResponseEntity.notFound().build()
        }
    }

    @Operation(summary = "Delete a track and its relationships")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Track deleted successfully"),
        ApiResponse(responseCode = "404", description = "Track not found")
    ])
    @DeleteMapping("/track/{id}")
    fun deleteTrack(@PathVariable id: Long): ResponseEntity<Void> {
        return try {
            musicImportService.deleteTrack(id)
            ResponseEntity.ok().build()
        } catch (e: Exception) {
            ResponseEntity.notFound().build()
        }
    }

    @Operation(summary = "Get all playlists")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Found the playlists",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = PlaylistProjection::class))])
    ])
    @GetMapping("/playlists")
    fun getPlaylists(): ResponseEntity<List<PlaylistProjection>> {
        val playlists = groupsRepository.findPlaylistsByTypeId(BigDecimal(4))
        return ResponseEntity.ok(playlists)
    }

    @Operation(summary = "Get all tracks belonging to a group")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Found the tracks",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = List::class), examples = [
                ExampleObject(value = """
                [
                  {
                    "TrackName": "Song Title",
                    "Artist": "Artist Name",
                    "Album": "Album Name",
                    "Files": [
                      {
                        "FileName": "song.mp3",
                        "FileLocation": "/path/to/file/",
                        "FileOnline": "Y",
                        "Duration": 300,
                        "BackupDate": "2023-01-01T12:00:00"
                      }
                    ]
                  }
                ]
            """)
            ])])
    ])
    @GetMapping("/tracksByGroup/{id}")
    fun getTracksByGroup(@PathVariable id: Long): ResponseEntity<List<Map<String, Any?>>> {
        val tracks = trackRepository.findTracksByGroupId(id)
        val serializedTracks = tracks.map { trackDataService.serializeTrack(it) }
        return ResponseEntity.ok(serializedTracks)
    }

    @Operation(summary = "Stream audio file")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "File found and streaming",
            content = [Content(mediaType = "audio/mpeg")]),
        ApiResponse(responseCode = "404", description = "File not found", content = [Content()])
    ])
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

    @Operation(summary = "Get album art from track file")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Image found",
            content = [Content(mediaType = "image/jpeg"), Content(mediaType = "image/png")]),
        ApiResponse(responseCode = "404", description = "File not found", content = [Content()])
    ])
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
                } catch (e: Exception) {
                    // Log error or handle it, fall through to placeholder
                }
            }
        }
        
        // Return placeholder image
        val placeholder = ClassPathResource("static/images/placeholder.png")
        return if (placeholder.exists()) {
             ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(placeholder)
        } else {
            // Fallback if placeholder is missing (e.g. return 404 or empty)
             ResponseEntity.notFound().build()
        }
    }
}
