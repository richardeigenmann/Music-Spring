package org.richinet.musicbackend.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.beans.factory.annotation.Value
import org.richinet.musicbackend.data.repository.TrackRepository
import org.springframework.boot.info.BuildProperties
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.sql.DataSource
import java.io.File
import java.sql.Connection

@RestController
@RequestMapping("/api")
class VersionController(
    private val buildProperties: BuildProperties,
    private val dataSource: DataSource,
    private val trackRepository: TrackRepository,
    @Value("\${app.music-directory}") private val musicDirectory: String
) {

    @Operation(summary = "Get application version information")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Version info returned",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = VersionInfo::class))])
    ])
    @GetMapping("/version")
    fun getVersion(): ResponseEntity<VersionInfo> {
        var dbConnected = false
        var dbUrl: String? = null
        var dbUser: String? = null
        var dbError: String? = null

        try {
            dataSource.connection.use { conn: Connection ->
                val metaData = conn.metaData
                dbConnected = true
                dbUrl = metaData.url
                dbUser = metaData.userName
            }
        } catch (e: Exception) {
            dbError = e.message
        }

        val versionInfo = VersionInfo(
            group = buildProperties.group ?: "unknown",
            artifact = buildProperties.artifact ?: "unknown",
            name = buildProperties.name ?: "unknown",
            version = buildProperties.version ?: "unknown",
            buildTime = buildProperties.time.toString(),
            runtime = System.getProperty("java.vm.name"),
            runtimeVersion = System.getProperty("java.version"),
            environment = if (File("/.dockerenv").exists() || System.getenv("KUBERNETES_SERVICE_HOST") != null) "container" else "ide/host",
            totalTrackCount = trackRepository.count(),
            dbConnected = dbConnected,
            dbUrl = dbUrl ?: "unknown",
            dbUser = dbUser ?: "unknown",
            musicDirectory = musicDirectory,
            dbError = dbError ?: ""
        )

        return ResponseEntity.ok(versionInfo)
    }
}
