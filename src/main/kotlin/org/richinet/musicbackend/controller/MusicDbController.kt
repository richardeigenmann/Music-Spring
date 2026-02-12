package org.richinet.musicbackend.controller

import org.richinet.musicbackend.data.projection.PlaylistProjection
import org.richinet.musicbackend.data.repository.GroupsRepository
import org.richinet.musicbackend.data.repository.TrackRepository
import org.richinet.musicbackend.service.TrackDataService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

@RestController
@RequestMapping("/api")
class MusicDbController(
    private val trackRepository: TrackRepository,
    private val trackDataService: TrackDataService,
    private val groupsRepository: GroupsRepository
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
}
