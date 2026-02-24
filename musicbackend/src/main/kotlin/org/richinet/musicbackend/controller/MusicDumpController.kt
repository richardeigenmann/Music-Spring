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
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.io.File

private const val DB_DUMP_FILE = "/richi/ToDo/music_db.sql"
private const val DB_JSON_FILE = "/richi/ToDo/music.json"

private val logger = LoggerFactory.getLogger(MusicDumpController::class.java)

@RestController
@RequestMapping("/api")
class MusicDumpController(
  private val trackRepository: TrackRepository,
  private val trackDataService: TrackDataService,
  private val databaseMaintenanceService: DatabaseMaintenanceService,
  private val musicImportService: MusicImportService,
  private val objectMapper: ObjectMapper
) {

  @Operation(
    summary = "Returns the Music database as a JSON string in the response",
    description = "Returns the entire Music database as a JSON string in the response"
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "A JSON response with the contents of the Music Database")
    ]
  )
  @GetMapping("/dump")
  fun dumpMusicData(): List<Map<String, Any?>> {
    return trackRepository.findAll().map { trackDataService.serializeTrack(it) }
  }

  @Operation(
    summary = "Dumps the Music database to the hardcoded server file /richi/ToDo/music_db.sql",
    description = "Dumps the Music database to the hardcoded server file /richi/ToDo/music_db.sql"
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Database dumped to /richi/ToDo/music_db.sql"),
    ]
  )
  @GetMapping("/dump-db")
  fun dumpDatabase(): String {
    databaseMaintenanceService.dumpDatabase(DB_DUMP_FILE)
    return "Database dumped to $DB_DUMP_FILE"
  }

  @Operation(
    summary = "Load the Music database from the hardcoded server file /richi/ToDo/music_db.sql",
    description = "Load the Music database from the hardcoded server file /richi/ToDo/music_db.sql"
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Database restored from /richi/ToDo/music_db.sql"),
    ]
  )
  @PostMapping("/sync-db")
  fun syncDatabase(): String {
    databaseMaintenanceService.restoreDatabase(DB_DUMP_FILE)
    logger.info ("Database restored from $DB_DUMP_FILE")
    return "Database restored from $DB_DUMP_FILE"
  }

  @Operation(
    summary = "Clear all tables in the Music database except for GroupType",
    description = "Clears all user-related tables (TrackGroup, TrackFile, Track, Groups) while leaving GroupType intact."
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Database cleared successfully"),
    ]
  )
  @PostMapping("/clear-db")
  fun clearDatabase(): String {
    databaseMaintenanceService.clearDatabase()
    logger.info("Database cleared (except GroupType)")
    return "Database cleared (except GroupType)"
  }

  @Operation(summary = "Import music data from request body")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Data imported successfully")
    ]
  )
  @PostMapping("/import")
  fun importMusicData(@RequestBody data: List<Map<String, Any>>) {
    musicImportService.importMusicData(data)
  }

  @Operation(
    summary = "Import music data from a hardcoded file path",
    description = "Reads music data from a JSON file located at the hardcoded path: `/richi/ToDo/music.json` and imports it into the database."
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Data imported successfully from file"),
      ApiResponse(responseCode = "404", description = "JSON file not found at the specified path"),
      ApiResponse(responseCode = "500", description = "Failed to parse JSON data")
    ]
  )
  @PostMapping("/import-from-file")
  fun importMusicDataFromFile(): ResponseEntity<String> {
    val file = File(DB_JSON_FILE)
    if (!file.exists()) {
      return ResponseEntity.status(404).body("File not found: $DB_JSON_FILE")
    }

    return try {
      val data: List<Map<String, Any>> = objectMapper.readValue(file)
      musicImportService.importMusicData(data)
      ResponseEntity.ok("Successfully imported data from $DB_JSON_FILE")
    } catch (e: Exception) {
      ResponseEntity.status(500).body("Failed to parse or import data: ${e.message}")
    }
  }
}
