package org.richinet.musicbackend.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.richinet.musicbackend.service.DatabaseMaintenanceService
import org.richinet.musicbackend.service.MusicImportService
import org.springframework.boot.CommandLineRunner
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.core.env.Environment
import java.io.File

@Component
@Order(2)
class StartupSyncHandler(
    private val databaseMaintenanceService: DatabaseMaintenanceService,
    private val musicImportService: MusicImportService,
    private val objectMapper: ObjectMapper,
    private val environment: Environment
) : CommandLineRunner {

    private val sqlDumpPath = "/richi/ToDo/music_db.sql"
    private val jsonDumpPath = "/richi/ToDo/music.json"

    override fun run(vararg args: String) {
        val jdbcUrl = environment.getProperty("spring.datasource.url") ?: ""
        if (!jdbcUrl.contains("jdbc:h2:", ignoreCase = true)) {
            println("Not using H2 database (URL: $jdbcUrl). Skipping startup sync.")
            return
        }

        val sqlFile = File(sqlDumpPath)
        var sqlRestoreSuccess = false
        if (sqlFile.exists()) {
            println("Found SQL dump file, attempting to sync database from $sqlDumpPath")
            try {
                databaseMaintenanceService.restoreDatabase(sqlDumpPath)
                sqlRestoreSuccess = true
            } catch (e: Exception) {
                println("SQL restore failed (likely incompatible database): ${e.message}")
            }
        }

        if (!sqlRestoreSuccess) {
            println("SQL sync not performed or failed. Falling back to JSON import check from $jsonDumpPath")
            val jsonFile = File(jsonDumpPath)
            if (jsonFile.exists()) {
                try {
                    val data = objectMapper.readValue(jsonFile, List::class.java) as List<Map<String, Any>>
                    musicImportService.importMusicData(data)
                    println("JSON import successful from $jsonDumpPath")
                } catch (e: Exception) {
                    println("JSON import failed: ${e.message}")
                    e.printStackTrace()
                }
            } else {
                println("JSON dump file not found at $jsonDumpPath. No data imported.")
            }
        }
    }
}
