package org.richinet.musicbackend.controller

import org.richinet.musicbackend.service.DatabaseMaintenanceService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class MaintenanceController(private val databaseMaintenanceService: DatabaseMaintenanceService) {

    private val dumpPath = "/richi/ToDo/music_db.sql"

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
}
