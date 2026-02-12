package org.richinet.musicbackend.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.richinet.musicbackend.service.MusicImportService
import org.springframework.boot.CommandLineRunner
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.io.File

//@Component
//@Order(2)
class StartupImportHandler(
    private val musicImportService: MusicImportService,
    private val objectMapper: ObjectMapper
) : CommandLineRunner {

    override fun run(vararg args: String) {
        val file = File("/richi/ToDo/music.json")
        if (file.exists()) {
            println("Found music.json, starting import...")
            try {
                val data = objectMapper.readValue(file, List::class.java) as List<Map<String, Any>>
                musicImportService.importMusicData(data)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            println("music.json not found at /richi/ToDo/music.json")
        }
    }
}
