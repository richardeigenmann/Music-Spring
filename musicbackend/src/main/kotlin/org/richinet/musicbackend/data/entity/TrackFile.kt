package org.richinet.musicbackend.data.entity

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.*
import java.math.BigDecimal
import java.sql.Timestamp

@Entity
@Table(name = "TrackFile")
@Schema(description = "Represents a physical file for a track")
class TrackFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FileId")
    @Schema(description = "Unique identifier for the file")
    var fileId: Long? = null

    @Column(name = "TrackId")
    @Schema(description = "ID of the track this file belongs to")
    var trackId: Long? = null

    @Column(name = "FileName")
    @Schema(description = "Name of the file")
    var fileName: String? = null

    @Column(name = "FileLocation")
    @Schema(description = "Relative path location of the file")
    var fileLocation: String? = null

    @Column(name = "FileOnline")
    @Schema(description = "Status indicating if the file is online")
    var fileOnline: String? = null

    @Column(name = "Duration")
    @Schema(description = "Duration of the track in seconds")
    var duration: BigDecimal? = null

    @Column(name = "BackupDate")
    @Schema(description = "Timestamp when the file was last backed up")
    var backupDate: Timestamp? = null

    @ManyToOne
    @JoinColumn(name = "TrackId", insertable = false, updatable = false)
    @Schema(description = "The track entity associated with this file")
    var track: Track? = null
}
