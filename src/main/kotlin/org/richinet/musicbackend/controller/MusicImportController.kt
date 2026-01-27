package org.richinet.musicbackend.controller

import org.richinet.musicbackend.service.MusicImportService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class MusicImportController(
    private val musicImportService: MusicImportService
) {

    @PostMapping("/import")
    fun importMusicData(@RequestBody data: List<Map<String, Any>>) {
        musicImportService.importMusicData(data)
    }
}
