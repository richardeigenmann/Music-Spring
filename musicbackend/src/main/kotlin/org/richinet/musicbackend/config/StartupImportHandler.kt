package org.richinet.musicbackend.config

import tools.jackson.databind.ObjectMapper
import org.richinet.musicbackend.service.MusicImportService
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.CommandLineRunner
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.File

//@Component
//@Order(2)
class StartupImportHandler(
    private val musicImportService: MusicImportService,
    @Qualifier("jacksonJsonMapper") private val objectMapper: ObjectMapper
) : CommandLineRunner {

    @Value("\${app.db.json-dump-path:/richi/ToDo/music.json}")
    private lateinit var jsonDumpPath: String

    override fun run(vararg args: String) {
        val file = File(jsonDumpPath)
        if (file.exists()) {
            println("Found $jsonDumpPath, starting import...")
            try {
                val data = objectMapper.readValue(file, List::class.java) as List<Map<String, Any>>
                musicImportService.importMusicData(data)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            println("JSON dump not found at $jsonDumpPath")
        }
    }
}
