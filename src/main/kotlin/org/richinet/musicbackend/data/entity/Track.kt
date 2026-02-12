package org.richinet.musicbackend.data.entity

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "Track")
@Schema(description = "Represents a music track")
class Track {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TrackId")
    @Schema(description = "Unique identifier for the track")
    var trackId: Long? = null

    @Column(name = "TrackName")
    @Schema(description = "Name of the track")
    var trackName: String? = null

    @OneToMany(mappedBy = "track")
    @OrderBy("sequence ASC")
    @Schema(description = "List of groups this track belongs to")
    var trackGroups: List<TrackGroup>? = null

    @OneToMany(mappedBy = "track")
    @Schema(description = "List of files associated with this track")
    var trackFiles: List<TrackFile>? = null
}
