package org.richinet.musicbackend.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.richinet.musicbackend.data.repository.TrackRepository
import org.richinet.musicbackend.service.DatabaseMaintenanceService
import org.richinet.musicbackend.service.MusicImportService
import org.richinet.musicbackend.service.TrackDataService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.io.File

@RestController
@RequestMapping("/api")
class MusicDumpController(
    private val trackRepository: TrackRepository,
    private val trackDataService: TrackDataService,
    private val databaseMaintenanceService: DatabaseMaintenanceService,
    private val musicImportService: MusicImportService,
    private val objectMapper: ObjectMapper
) {

    private val dumpPath = "/richi/ToDo/music_db.sql"
    private val jsonDumpPath = "/richi/ToDo/music.json"

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

    @Operation(summary = "Import music data from request body")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Data imported successfully")
    ])
    @PostMapping("/import")
    fun importMusicData(@RequestBody data: List<Map<String, Any>>) {
        musicImportService.importMusicData(data)
    }

    @Operation(
        summary = "Import music data from a hardcoded file path",
        description = "Reads music data from a JSON file located at the hardcoded path: `/richi/ToDo/music.json` and imports it into the database."
    )
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Data imported successfully from file"),
        ApiResponse(responseCode = "404", description = "JSON file not found at the specified path"),
        ApiResponse(responseCode = "500", description = "Failed to parse JSON data")
    ])
    @PostMapping("/import-from-file")
    fun importMusicDataFromFile(): ResponseEntity<String> {
        val file = File(jsonDumpPath)
        if (!file.exists()) {
            return ResponseEntity.status(404).body("File not found: $jsonDumpPath")
        }

        return try {
            val data: List<Map<String, Any>> = objectMapper.readValue(file)
            musicImportService.importMusicData(data)
            ResponseEntity.ok("Successfully imported data from $jsonDumpPath")
        } catch (e: Exception) {
            ResponseEntity.status(500).body("Failed to parse or import data: ${e.message}")
        }
    }
}
