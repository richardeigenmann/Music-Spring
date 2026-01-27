package org.richinet.musicbackend.data.entity

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "Track")
class Track {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TrackId")
    var trackId: Long? = null

    @Column(name = "TrackName")
    var trackName: String? = null

    @OneToMany(mappedBy = "track")
    @OrderBy("sequence ASC")
    var trackGroups: List<TrackGroup>? = null

    @OneToMany(mappedBy = "track")
    var trackFiles: List<TrackFile>? = null
}
