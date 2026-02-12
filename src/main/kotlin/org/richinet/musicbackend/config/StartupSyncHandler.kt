package org.richinet.musicbackend.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.richinet.musicbackend.service.DatabaseMaintenanceService
import org.richinet.musicbackend.service.MusicImportService
import org.springframework.boot.CommandLineRunner
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.io.File

@Component
@Order(2)
class StartupSyncHandler(
    private val databaseMaintenanceService: DatabaseMaintenanceService,
    private val musicImportService: MusicImportService,
    private val objectMapper: ObjectMapper
) : CommandLineRunner {

    private val sqlDumpPath = "/richi/ToDo/music_db.sql"
    private val jsonDumpPath = "/richi/ToDo/music.json"

    override fun run(vararg args: String) {
        val sqlFile = File(sqlDumpPath)
        if (sqlFile.exists()) {
            println("Found SQL dump file, attempting to sync database from $sqlDumpPath")
            databaseMaintenanceService.restoreDatabase(sqlDumpPath)
        } else {
            println("SQL dump file not found. Falling back to JSON import from $jsonDumpPath")
            val jsonFile = File(jsonDumpPath)
            if (jsonFile.exists()) {
                try {
                    val data = objectMapper.readValue(jsonFile, List::class.java) as List<Map<String, Any>>
                    musicImportService.importMusicData(data)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                println("JSON dump file not found at $jsonDumpPath. No data imported.")
            }
        }
    }
}
