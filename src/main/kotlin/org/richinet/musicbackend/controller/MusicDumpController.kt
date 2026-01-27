package org.richinet.musicbackend.controller

import org.richinet.musicbackend.data.repository.TrackRepository
import org.richinet.musicbackend.service.TrackDataService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class MusicDumpController(
    private val trackRepository: TrackRepository,
    private val trackDataService: TrackDataService
) {

    @GetMapping("/dump")
    fun dumpMusicData(): List<Map<String, Any?>> {
        val tracks = trackRepository.findAll()
        return tracks.map { trackDataService.serializeTrack(it) }
    }

    @GetMapping("/track/{id}")
    fun getTrack(@PathVariable id: Long): ResponseEntity<Map<String, Any?>> {
        val track = trackRepository.findById(id)
        return if (track.isPresent) {
            ResponseEntity.ok(trackDataService.serializeTrack(track.get()))
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
