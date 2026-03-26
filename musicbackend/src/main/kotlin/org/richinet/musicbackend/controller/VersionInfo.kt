package org.richinet.musicbackend.controller

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Application version and status information")
data class VersionInfo(
    val group: String,
    val artifact: String,
    val name: String,
    val version: String,
    val buildTime: String,
    val runtime: String,
    val runtimeVersion: String,
    val environment: String,
    val totalTrackCount: Long,
    val dbConnected: Boolean,
    val dbUrl: String,
    val dbUser: String,
    val musicDirectory: String,
    val dbError: String = ""
)
