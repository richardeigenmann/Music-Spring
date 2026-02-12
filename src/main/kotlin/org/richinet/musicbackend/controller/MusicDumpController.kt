package org.richinet.musicbackend.controller

import org.richinet.musicbackend.data.repository.TrackRepository
import org.richinet.musicbackend.service.DatabaseMaintenanceService
import org.richinet.musicbackend.service.MusicImportService
import org.richinet.musicbackend.service.TrackDataService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class MusicDumpController(
    private val trackRepository: TrackRepository,
    private val trackDataService: TrackDataService,
    private val databaseMaintenanceService: DatabaseMaintenanceService,
    private val musicImportService: MusicImportService
) {

    private val dumpPath = "/richi/ToDo/music_db.sql"

    @GetMapping("/dump")
    fun dumpMusicData(): List<Map<String, Any?>> {
        val tracks = trackRepository.findAll()
        return tracks.map { trackDataService.serializeTrack(it) }
    }

    @GetMapping("/dump-db")
    fun dumpDatabase(): String {
        databaseMaintenanceService.dumpDatabase(dumpPath)
        return "Database dumped to $dumpPath"
    }

    @PostMapping("/sync-db")
    fun syncDatabase(): String {
        databaseMaintenanceService.restoreDatabase(dumpPath)
        return "Database restored from $dumpPath"
    }

    @PostMapping("/import")
    fun importMusicData(@RequestBody data: List<Map<String, Any>>) {
        musicImportService.importMusicData(data)
    }
}
