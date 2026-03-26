package org.richinet.musicbackend.data.entity

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.*

@Entity
@Table(name = "track")
@Schema(description = "Represents a music track")
class Track {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Schema(description = "Unique identifier for the track")
    var id: Long? = null

    @Column(name = "name")
    @Schema(description = "Name of the track")
    var name: String? = null

    @OneToMany(mappedBy = "track")
    @OrderBy("sequence ASC")
    @Schema(description = "List of tags this track belongs to")
    var trackTags: List<TrackTag>? = null

    @OneToMany(mappedBy = "track")
    @Schema(description = "List of files associated with this track")
    var trackFiles: List<TrackFile>? = null
}
