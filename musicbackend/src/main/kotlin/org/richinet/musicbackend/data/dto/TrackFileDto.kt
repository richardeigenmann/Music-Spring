package org.richinet.musicbackend.data.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(description = "Data Transfer Object for a track file")
data class TrackFileDto(
    @Schema(description = "Unique identifier for the file")
    val fileId: Long?,
    @Schema(description = "Name of the file")
    val fileName: String?,
    @Schema(description = "Relative path location of the file")
    val fileLocation: String?,
    @Schema(description = "Duration of the track in seconds")
    val duration: BigDecimal?
)
