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

@RestController
@RequestMapping("/api")
class VersionController(
    private val buildProperties: BuildProperties
) {

    @Operation(summary = "Get application version information")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Version info returned",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = Map::class))])
    ])
    @GetMapping("/version")
    fun getVersion(): ResponseEntity<Map<String, String>> {
        val versionInfo = mapOf(
            "group" to buildProperties.group,
            "artifact" to buildProperties.artifact,
            "name" to buildProperties.name,
            "version" to buildProperties.version,
            "time" to buildProperties.time.toString()
        )
        return ResponseEntity.ok(versionInfo)
    }
}
