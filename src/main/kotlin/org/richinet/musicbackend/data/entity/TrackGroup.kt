package org.richinet.musicbackend.data.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.sql.Timestamp

@Entity
@Table(name = "TrackGroup")
@IdClass(TrackGroupId::class)
class TrackGroup {
    @Id
    @Column(name = "TrackId")
    var trackId: Long? = null

    @Id
    @Column(name = "GroupId")
    var groupId: Long? = null

    @Column(name = "Sequence")
    var sequence: BigDecimal? = null

    @Column(name = "LastModification")
    var lastModification: Timestamp? = null

    @ManyToOne
    @JoinColumn(name = "GroupId", insertable = false, updatable = false)
    var group: Groups? = null

    @ManyToOne
    @JoinColumn(name = "TrackId", insertable = false, updatable = false)
    var track: Track? = null
}
