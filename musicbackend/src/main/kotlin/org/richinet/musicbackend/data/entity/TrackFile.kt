package org.richinet.musicbackend.data.entity

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(
    name = "track_file",
    indexes = [Index(name = "idx_track_file_hash", columnList = "file_hash")]
)
@Schema(description = "Represents a physical file for a track")
class TrackFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    @Schema(description = "Unique identifier for the file")
    var id: Long? = null

    @Column(name = "track_id")
    @Schema(description = "ID of the track this file belongs to")
    var trackId: Long? = null

    @Column(name = "file_name")
    @Schema(description = "Name of the file")
    var fileName: String? = null

    @Column(name = "file_location")
    @Schema(description = "Relative path location of the file")
    var fileLocation: String? = null

    @Column(name = "duration")
    @Schema(description = "Duration of the track in seconds")
    var duration: BigDecimal? = null

    @Column(name = "file_hash", length = 64)
    @Schema(description = "Audio fingerprint (file size + MD5 of first 64KB) used for duplicate detection")
    var fileHash: String? = null

    @ManyToOne
    @JoinColumn(name = "track_id", insertable = false, updatable = false)
    @Schema(description = "The track entity associated with this file")
    var track: Track? = null
}
