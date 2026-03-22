package org.richinet.musicbackend.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
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
    private val dataSource: DataSource
) {

    @Operation(summary = "Get application version information")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Version info returned",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = Map::class))])
    ])
    @GetMapping("/version")
    fun getVersion(): ResponseEntity<Map<String, Any>> {
        val versionInfo = mutableMapOf<String, Any>(
            "group" to buildProperties.group,
            "artifact" to buildProperties.artifact,
            "name" to buildProperties.name,
            "version" to buildProperties.version,
            "time" to buildProperties.time.toString(),
            "runtime" to System.getProperty("java.vm.name"),
            "runtimeVersion" to System.getProperty("java.version"),
            "environment" to if (File("/.dockerenv").exists() || System.getenv("KUBERNETES_SERVICE_HOST") != null) "container" else "ide/host"
        )

        try {
            dataSource.connection.use { conn: Connection ->
                val metaData = conn.metaData
                versionInfo["dbConnected"] = true
                versionInfo["dbUrl"] = metaData.url
                versionInfo["dbUser"] = metaData.userName
            }
        } catch (e: Exception) {
            versionInfo["dbConnected"] = false
            versionInfo["dbError"] = e.message ?: "Unknown connection error"
        }

        return ResponseEntity.ok(versionInfo)
    }
}
