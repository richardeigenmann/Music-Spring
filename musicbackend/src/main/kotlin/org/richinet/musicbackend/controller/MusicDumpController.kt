package org.richinet.musicbackend.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.richinet.musicbackend.service.DatabaseMaintenanceService
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*

private val logger = LoggerFactory.getLogger(MusicDumpController::class.java)

@RestController
@RequestMapping("/api")
class MusicDumpController(
  private val databaseMaintenanceService: DatabaseMaintenanceService
) {

  @Operation(
    summary = "Dumps the Music database to a specified file",
    description = "Dumps the Music database to the file path provided in the 'filePath' parameter."
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Database dumped successfully"),
    ]
  )
  @GetMapping("/dump-db")
  fun dumpDatabase(
      @Parameter(description = "Full path to the dump file (e.g. /tmp/music_db.sql)")
      @RequestParam filePath: String
  ): String {
    databaseMaintenanceService.dumpDatabase(filePath)
    return "Database dumped to $filePath"
  }

  @Operation(
    summary = "Load the Music database from a specified file",
    description = "Load the Music database from the file path provided in the 'filePath' parameter."
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Database restored successfully"),
    ]
  )
  @PostMapping("/sync-db")
  fun syncDatabase(
      @Parameter(description = "Full path to the dump file (e.g. /tmp/music_db.sql)")
      @RequestParam filePath: String
  ): String {
    databaseMaintenanceService.restoreDatabase(filePath)
    logger.info ("Database restored from $filePath")
    return "Database restored from $filePath"
  }

  @Operation(
    summary = "Clear all tables in the Music database",
    description = "Clears all tables (TrackGroup, TrackFile, Track, Groups, GroupType)."
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Database cleared successfully"),
    ]
  )
  @PostMapping("/clear-db")
  fun clearDatabase(): String {
    databaseMaintenanceService.clearDatabase()
    logger.info("Database cleared (including GroupType)")
    return "Database cleared (including GroupType)"
  }
}
