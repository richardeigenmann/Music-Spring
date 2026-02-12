package org.richinet.musicbackend.controller

import org.richinet.musicbackend.data.projection.PlaylistProjection
import org.richinet.musicbackend.data.repository.GroupsRepository
import org.richinet.musicbackend.data.repository.TrackFileRepository
import org.richinet.musicbackend.data.repository.TrackRepository
import org.richinet.musicbackend.service.TrackDataService
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.File
import java.math.BigDecimal
import java.nio.file.Files

private val MUSIC_DIRECTORY = "/richi"

@RestController
@RequestMapping("/api")
class MusicDbController(
    private val trackRepository: TrackRepository,
    private val trackDataService: TrackDataService,
    private val groupsRepository: GroupsRepository,
    private val trackFileRepository: TrackFileRepository
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

    @GetMapping("/playlists")
    fun getPlaylists(): ResponseEntity<List<PlaylistProjection>> {
        val playlists = groupsRepository.findPlaylistsByTypeId(BigDecimal(4))
        return ResponseEntity.ok(playlists)
    }

    @GetMapping("/tracksByGroup/{id}")
    fun getTracksByGroup(@PathVariable id: Long): ResponseEntity<List<Map<String, Any?>>> {
        val tracks = trackRepository.findTracksByGroupId(id)
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
}
